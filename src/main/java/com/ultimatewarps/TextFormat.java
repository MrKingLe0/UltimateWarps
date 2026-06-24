package com.ultimatewarps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bug fix history:
 *   1. Names/lore were originally only run through ChatColor.translateAlternateColorCodes
 *      ('&' codes only) - MiniMessage tags like <gradient:...> were never parsed at all
 *      and showed up as literal text.
 *   2. A later fix attempt tried to support legacy codes and MiniMessage together by
 *      deserializing into a Component and then re-serializing back out to a string (or
 *      mixing two different Component-based serializers on the same string). Both
 *      approaches actively destroy certain tags: a <gradient:...> tag's color stops are
 *      consumed into individual per-character colors once parsed, and there's no way to
 *      reconstruct the original two-color argument list from that when serializing back
 *      out - re-serializing produces a malformed tag like <gradient::> with the colors
 *      gone, which is exactly the bug that was reported (this is also why a third-party
 *      plugin chat-converter that does deserialize-then-reserialize on every message can
 *      corrupt a player's MiniMessage gradient before this plugin's own listener ever
 *      sees it, if that plugin's listener runs at an earlier priority).
 *   3. Item names/lore also have a vanilla-Minecraft parent style that makes them italic
 *      by default - any Component placed there needs italic explicitly turned off, or it
 *      renders slanted even if nothing asked for that.
 *
 * The fix: support legacy '&' codes (including hex: &#RRGGBB and the old &x&R&R&G&G&B&B
 * form) AND MiniMessage tags in the same string by upgrading the legacy parts to
 * MiniMessage tag syntax via pure text substitution - never by parsing into a Component
 * and back. Substitution skips over any text already inside a MiniMessage tag (so a
 * gradient's own color arguments, which contain '#' hex values, are never mistaken for a
 * legacy hex code), and the whole result is parsed by MiniMessage exactly once. This
 * means a single render() call is the only place formatting is ever interpreted - no
 * downstream code should deserialize an already-rendered Component back into a string
 * and re-parse it, since that's what causes the corruption described above.
 */
public final class TextFormat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // &#RRGGBB - modern legacy hex shorthand used by many plugins/configs.
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    // &x&R&R&G&G&B&B - the older, more verbose legacy hex form.
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");
    // A single legacy '&' formatting/color code.
    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&([0-9A-FK-ORa-fk-or])");
    // A single legacy '§' (section-sign) formatting/color code - some hardcoded strings
    // in this codebase use the raw section-sign form directly rather than '&'.
    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("\u00A7([0-9A-FK-ORa-fk-or])");

    private TextFormat() {}

    /**
     * Renders a raw string (warp name, display name, lore line, GUI template, etc.) that
     * may contain MiniMessage tags, legacy '&' codes, '&#RRGGBB' hex, legacy
     * '&x&R&R&G&G&B&B' hex, or any mix of these, into a single Component. Always forces
     * italic off (vanilla Minecraft renders item names/lore italic by default unless told
     * otherwise), unless the text explicitly set italic itself.
     */
    public static Component render(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        String upgraded = upgradeLegacyToMiniMessage(raw);
        Component component = MINI_MESSAGE.deserialize(upgraded);
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Converts every legacy '&' code/hex sequence in the string into the equivalent
     * MiniMessage tag, while leaving anything already inside a MiniMessage tag
     * (<...>) completely untouched - critical so that a gradient's own color arguments
     * (which are plain text like "red" or "#ff0000" sitting between '<' and '>') are
     * never mistaken for legacy syntax and corrupted.
     *
     * Legacy color/format codes are NOT auto-closed with their MiniMessage closing tag;
     * MiniMessage tags without an explicit closing tag automatically apply to the rest of
     * the string (and any unclosed tags are closed automatically at the end), which is
     * exactly how legacy '&' codes already behave - this keeps the converted output
     * behaviorally identical to plain legacy text.
     */
    private static String upgradeLegacyToMiniMessage(String input) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean insideTag = false;

        while (i < input.length()) {
            char c = input.charAt(i);

            if (c == '<') {
                insideTag = true;
                result.append(c);
                i++;
                continue;
            }
            if (c == '>') {
                insideTag = false;
                result.append(c);
                i++;
                continue;
            }
            if (insideTag) {
                // Never touch text inside an existing MiniMessage tag - this is what
                // protects a <gradient:red:blue> tag's own arguments from being treated
                // as legacy syntax.
                result.append(c);
                i++;
                continue;
            }

            if (c == '&') {
                Matcher legacyHex = LEGACY_HEX_PATTERN.matcher(input.substring(i));
                if (legacyHex.lookingAt()) {
                    String match = legacyHex.group();
                    StringBuilder hex = new StringBuilder();
                    for (int j = 2; j < match.length(); j += 2) {
                        if (j + 1 < match.length()) hex.append(match.charAt(j + 1));
                    }
                    result.append("<#").append(hex).append(">");
                    i += match.length();
                    continue;
                }

                Matcher hex = HEX_PATTERN.matcher(input.substring(i));
                if (hex.lookingAt()) {
                    result.append("<#").append(hex.group(1)).append(">");
                    i += hex.group().length();
                    continue;
                }

                Matcher code = LEGACY_CODE_PATTERN.matcher(input.substring(i));
                if (code.lookingAt()) {
                    String tag = legacyCodeToMiniMessageTag(code.group(1).charAt(0));
                    if (tag != null) {
                        result.append(tag);
                        i += 2;
                        continue;
                    }
                }
            }

            if (c == '\u00A7') {
                Matcher sectionCode = SECTION_CODE_PATTERN.matcher(input.substring(i));
                if (sectionCode.lookingAt()) {
                    String tag = legacyCodeToMiniMessageTag(sectionCode.group(1).charAt(0));
                    if (tag != null) {
                        result.append(tag);
                        i += 2;
                        continue;
                    }
                }
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

    private static String legacyCodeToMiniMessageTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    /**
     * Substitutes a single {placeholder} with a raw value (which may itself contain
     * formatting) and renders the *combined* string in one pass, rather than rendering
     * the template and the value separately - rendering them separately is exactly how
     * the original bug happened, since the value's own color codes never got translated
     * when only the surrounding template was passed through ChatColor beforehand.
     *
     * Note: this still only works correctly if the template and the value use the SAME
     * format as each other (both MiniMessage, or both legacy) - mixing styles within one
     * combined string is the unsupported case described above. If a template uses '&'
     * codes and a user-set value uses MiniMessage tags, use renderTemplate() instead,
     * which composes them as separate Components rather than one combined string.
     */
    public static Component renderWithPlaceholder(String template, String placeholder, String value) {
        String combined = template.replace(placeholder, value == null ? "" : value);
        return render(combined);
    }

    /**
     * Renders a template string containing one or more {placeholder} or %placeholder%
     * tokens, where each placeholder's value is rendered as its OWN separate Component
     * and substituted in via MiniMessage's native placeholder resolver mechanism, in a
     * single deserialize() call.
     *
     * Bug fix: an earlier version of this method split the template into separate chunks
     * around each placeholder occurrence and rendered each chunk with its own independent
     * render() call, gluing the resulting Components together afterward. That broke tag
     * pairing across the split boundary: a template like
     * "<gradient:...>...<white>%warp%</white>!</gradient>" would have its "before" chunk
     * (which opens <gradient> and <white> but never closes them) and its "after" chunk
     * (</white>!</gradient>, which closes tags that were never opened IN THAT CHUNK)
     * parsed as two completely independent strings - so the closing tags in the "after"
     * chunk had nothing to close and came out as literal text instead of formatting.
     *
     * The fix: convert every {placeholder}/%placeholder% token in the template into
     * MiniMessage's own <placeholder_N> tag syntax, then parse the ENTIRE template in one
     * single deserialize() call with a Placeholder.component(...) resolver for each one.
     * This lets MiniMessage's own parser handle tag nesting/closing across the
     * placeholder's position correctly, since it sees the whole string as one continuous
     * document instead of pre-cut fragments.
     *
     * @param template the raw template string, containing literal {placeholder} or
     *                  %placeholder% tokens
     * @param placeholders alternating placeholder-token/value pairs, e.g.
     *                      ("{warp_name}", warp.getDisplayName(), "{owner}", ownerName)
     */
    public static Component renderTemplate(String template, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("renderTemplate placeholders must be in token/value pairs");
        }

        // Render every placeholder's value once up front (in its own format), and build
        // a MiniMessage TagResolver for each one under a safe, unique internal tag name
        // that can never collide with a real MiniMessage tag.
        java.util.List<net.kyori.adventure.text.minimessage.tag.resolver.TagResolver> resolvers = new java.util.ArrayList<>();
        String workingTemplate = template;
        for (int i = 0; i < placeholders.length; i += 2) {
            String token = placeholders[i];
            String value = placeholders[i + 1];
            String internalTagName = "uw_placeholder_" + (i / 2);
            workingTemplate = workingTemplate.replace(token, "<" + internalTagName + ">");
            resolvers.add(net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component(
                    internalTagName, render(value)));
        }

        // The template itself may still contain legacy '&'/'§' codes around the
        // placeholders - upgrade those the same way render() does, but don't call
        // render() directly here since that would also try to deserialize our freshly
        // inserted <uw_placeholder_N> tags before the resolvers are applied, which is
        // fine actually (they're valid, harmless tag names) - so this just reuses the
        // same upgrade step and does the single deserialize call with resolvers attached.
        String upgraded = upgradeLegacyToMiniMessage(workingTemplate);
        Component result = MINI_MESSAGE.deserialize(upgraded,
                net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.resolver(resolvers));

        return result.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
