package com.ultimatewarps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFormat {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&x(&[A-Fa-f0-9]){6}");

    private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("&([0-9A-FK-ORa-fk-or])");

    private static final Pattern SECTION_CODE_PATTERN = Pattern.compile("\u00A7([0-9A-FK-ORa-fk-or])");

    private TextFormat() {}

    public static Component render(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();

        String upgraded = upgradeLegacyToMiniMessage(raw);
        Component component = MINI_MESSAGE.deserialize(upgraded);
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

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

    public static Component renderWithPlaceholder(String template, String placeholder, String value) {
        String combined = template.replace(placeholder, value == null ? "" : value);
        return render(combined);
    }

    public static Component renderTemplate(String template, String... placeholders) {
        if (placeholders.length % 2 != 0) {
            throw new IllegalArgumentException("renderTemplate placeholders must be in token/value pairs");
        }

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

        String upgraded = upgradeLegacyToMiniMessage(workingTemplate);
        Component result = MINI_MESSAGE.deserialize(upgraded,
                net.kyori.adventure.text.minimessage.tag.resolver.TagResolver.resolver(resolvers));

        return result.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
