package com.demo.speech;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class TranscriptFormatterService {
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public String toHtmlDocument(String rawTranscript, String languageHint) {
        if (rawTranscript == null || rawTranscript.isBlank()) {
            return wrapHtml("<p>No transcript available.</p>");
        }

        String normalized = normalizeSpeechText(rawTranscript, languageHint);
        List<String> blocks = buildBlocks(normalized);
        return wrapHtml(String.join("", blocks));
    }

    private String normalizeSpeechText(String input, String languageHint) {
        String text = input.trim();
        text = text.replaceAll("(?i)\\b([a-z])\\s+for\\s+[a-z]+\\b", "$1");
        text = text.replaceAll("(?i)\\bfull\\s*stop\\b", ".");
        text = text.replaceAll("(?i)\\bcomma\\b", ",");
        text = text.replaceAll("(?i)\\bcolon\\b", ":");
        text = text.replaceAll("(?i)\\bsemi\\s*colon\\b", ";");
        text = text.replaceAll("(?i)\\bquestion\\s*mark\\b", "?");
        text = text.replaceAll("(?i)\\bexclamation\\s*mark\\b", "!");
        text = text.replaceAll("(?i)\\bslash\\b", "/");
        text = text.replaceAll("(?i)\\bstroke\\b", "/");
        text = text.replaceAll("(?i)\\bhyphen\\b", "-");
        text = text.replaceAll("(?i)\\bopen\\s+inverted\\s+commas\\b", "\"");
        text = text.replaceAll("(?i)\\bclosing\\s+inverted\\s+commas\\b", "\"");
        text = text.replaceAll("(?i)\\binverted\\s+commas\\b", "\"");

        text = text.replaceAll("(?i)\\b(on\\s+the\\s+)?next\\s+line\\b\\s*[:,]?", "\n");
        text = text.replaceAll("(?i)\\bnew\\s+line\\b\\s*[:,]?", "\n");
        text = text.replaceAll("(?i)\\b(new|next)\\s+paragraph\\b\\s*[:,]?", "\n\n");
        text = text.replaceAll("(?i)\\bparagraph\\b\\s*[:,]?", "\n\n");

        text = text.replaceAll("(?i)\\b(next|new)\\s+(heading|header|head(?:ing)?)\\b\\s*(is)?\\s*[:,]?", "\n[[HEADING]] ");
        text = text.replaceAll("(?i)\\b(end\\s+of\\s+the\\s+bullet\\s+points?)\\b\\s*[:,]?", "\n[[END_BULLETS]] ");
        text = text.replaceAll("(?i)\\b(new|next)?\\s*bullet\\s*points?\\b\\s*(table)?\\s*[:,]?", "\n[[BULLET]] ");

        // Language hint hook for future expansion.
        if (languageHint != null && languageHint.toLowerCase(Locale.ROOT).startsWith("en")) {
            text = text.replaceAll("(?i)\\bAM\\b", "a.m.");
            text = text.replaceAll("(?i)\\bPM\\b", "p.m.");
        }

        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    private List<String> buildBlocks(String normalized) {
        List<String> blocks = new ArrayList<>();
        List<String> bulletItems = new ArrayList<>();

        String[] lines = normalized.split("\\n");
        for (String rawLine : lines) {
            String line = MULTI_SPACE.matcher(rawLine).replaceAll(" ").trim();
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("[[END_BULLETS]]")) {
                flushBullets(blocks, bulletItems);
                continue;
            }

            if (line.startsWith("[[HEADING]]")) {
                flushBullets(blocks, bulletItems);
                String heading = cleanupHeading(line.replace("[[HEADING]]", "").trim());
                if (!heading.isBlank()) {
                    blocks.add("<h3>" + HtmlUtils.htmlEscape(capitalizeFirst(heading)) + "</h3>");
                }
                continue;
            }

            if (line.startsWith("[[BULLET]]")) {
                String item = cleanupSentence(line.replace("[[BULLET]]", "").trim());
                if (!item.isBlank()) {
                    bulletItems.add(item);
                }
                continue;
            }

            flushBullets(blocks, bulletItems);
            String paragraph = cleanupSentence(line);
            if (!paragraph.isBlank()) {
                blocks.add("<p>" + HtmlUtils.htmlEscape(paragraph) + "</p>");
            }
        }

        flushBullets(blocks, bulletItems);
        if (blocks.isEmpty()) {
            blocks.add("<p>" + HtmlUtils.htmlEscape(cleanupSentence(normalized)) + "</p>");
        }

        return blocks;
    }

    private void flushBullets(List<String> blocks, List<String> bulletItems) {
        if (bulletItems.isEmpty()) {
            return;
        }
        StringBuilder ul = new StringBuilder("<ul>");
        for (String item : bulletItems) {
            ul.append("<li>").append(HtmlUtils.htmlEscape(item)).append("</li>");
        }
        ul.append("</ul>");
        blocks.add(ul.toString());
        bulletItems.clear();
    }

    private String cleanupHeading(String heading) {
        String value = heading;
        value = value.replaceAll("(?i)\\b(please\\s+)?put\\s+that\\s+in\\b", "");
        value = value.replaceAll("(?i)\\b(bold|underline|underlined|aligned|not\\s+underlined|headed)\\b", "");
        value = value.replaceAll("(?i)^is\\b\\s*", "");
        value = value.replaceAll("\\s{2,}", " ").trim();
        return value;
    }

    private String cleanupSentence(String sentence) {
        String value = sentence;
        value = value.replaceAll("(?i)\\b(call\\s+stop|we['’]?ll\\s+stop)\\b", ".");
        value = value.replaceAll("(?i)\\s+\\.", ".");
        value = value.replaceAll("(?i)\\s+,", ",");
        value = value.replaceAll("(?i)\\s+;", ";");
        value = value.replaceAll("(?i)\\s+:", ":");
        value = value.replaceAll("\\s{2,}", " ").trim();
        return capitalizeFirst(value);
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private String wrapHtml(String body) {
        return "<div class=\"white-document\">" + body + "</div>";
    }
}
