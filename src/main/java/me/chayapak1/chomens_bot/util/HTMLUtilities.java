package me.chayapak1.chomens_bot.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// https://github.com/Earthcomputer/clientcommands/blob/fabric/src/main/java/net/earthcomputer/clientcommands/features/WikiRetriever.java
public class HTMLUtilities {
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<\\s*(/)?\\s*(\\w+).*?>|<!--.*?-->|\n", Pattern.DOTALL);
    private static final String CODE_COLOR = "§2";
    private static final String DEFAULT_COLOR = "§a";

    public static String toFormattingCodes (String html) {
        Matcher matcher = HTML_TAG_PATTERN.matcher(html);
        StringBuilder raw = new StringBuilder();

        boolean bold = false, italic = false, underline = false, code = false;

        // -1 for not in list, 0 for unordered list, >= 1 for ordered list
        int listIndex = -1;

        while (matcher.find()) {
            matcher.appendReplacement(raw, "");

            boolean endTag = matcher.group(1) != null;
            String tagName = matcher.group(2);
            if (tagName == null) {
                // we're in a comment or newline
                continue;
            }
            tagName = tagName.toLowerCase(Locale.ENGLISH);

            if (!endTag) {
                switch (tagName) {
                    case "b":
                        raw.append("§l");
                        bold = true;
                        break;
                    case "i":
                        raw.append("§o");
                        italic = true;
                        break;
                    case "u":
                    case "dt":
                        raw.append("§n");
                        underline = true;
                        break;
                    case "code":
                        raw.append(CODE_COLOR);
                        if (bold) {
                            raw.append("§l");
                        }
                        if (italic) {
                            raw.append("§o");
                        }
                        if (underline) {
                            raw.append("§n");
                        }
                        code = true;
                        break;
                    case "dd":
                        raw.append("  ");
                        break;
                    case "ul":
                        listIndex = 0;
                        break;
                    case "ol":
                        listIndex = 1;
                        break;
                    case "li":
                        if (listIndex >= 1) {
                            raw.append("  ").append(listIndex).append(". ");
                            listIndex++;
                        } else {
                            raw.append("  • ");
                        }
                        break;
                    case "br":
                        raw.append("\n");
                }
            } else {
                switch (tagName) {
                    case "b":
                        if (code) {
                            raw.append(CODE_COLOR);
                        } else {
                            raw.append(DEFAULT_COLOR);
                        }
                        if (italic) {
                            raw.append("§o");
                        }
                        if (underline) {
                            raw.append("§n");
                        }
                        bold = false;
                        break;
                    case "i":
                        if (code) {
                            raw.append(CODE_COLOR);
                        } else {
                            raw.append(DEFAULT_COLOR);
                        }
                        if (bold) {
                            raw.append("§l");
                        }
                        if (underline) {
                            raw.append("§n");
                        }
                        italic = false;
                        break;
                    case "dt":
                        raw.append("\n");
                        //fallthrough
                    case "u":
                        if (code) {
                            raw.append(CODE_COLOR);
                        } else {
                            raw.append(DEFAULT_COLOR);
                        }
                        if (bold) {
                            raw.append("§l");
                        }
                        if (italic) {
                            raw.append("§o");
                        }
                        underline = false;
                        break;
                    case "code":
                        raw.append(DEFAULT_COLOR);
                        if (bold) {
                            raw.append("§l");
                        }
                        if (italic) {
                            raw.append("§o");
                        }
                        if (underline) {
                            raw.append("§n");
                        }
                        code = false;
                        break;
                    case "ul":
                    case "ol":
                        listIndex = -1;
                        break;
                    case "dd":
                    case "li":
                    case "br":
                    case "p":
                        raw.append("\n");
                        break;
                }
            }
        }
        matcher.appendTail(raw);

        if (raw.isEmpty()) {
            return null;
        }

        String rawStr = raw.toString();
        rawStr = rawStr.replace("&quot;", "\"");
        rawStr = rawStr.replace("&#39;", "'");
        rawStr = rawStr.replace("&lt;", "<");
        rawStr = rawStr.replace("&gt;", ">");
        rawStr = rawStr.replace("&amp;", "&");

        return rawStr;
    }
}
