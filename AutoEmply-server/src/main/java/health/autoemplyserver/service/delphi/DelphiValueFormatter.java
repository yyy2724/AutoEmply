package health.autoemplyserver.service.delphi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class DelphiValueFormatter {

    private static final BigDecimal PIXEL_TO_MM = new BigDecimal("2.645833333333333");

    public String encodeString(String text) {
        if (text == null || text.isEmpty()) {
            return "''";
        }
        StringBuilder builder = new StringBuilder();
        StringBuilder ascii = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character >= ' ' && character <= '~') {
                ascii.append(character == '\'' ? "''" : character);
            } else {
                if (!ascii.isEmpty()) {
                    builder.append('\'').append(ascii).append('\'');
                    ascii.setLength(0);
                }
                builder.append('#').append((int) character);
            }
        }
        if (!ascii.isEmpty()) {
            builder.append('\'').append(ascii).append('\'');
        }
        return builder.isEmpty() ? "''" : builder.toString();
    }

    public String encodePascalStringLiterals(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        StringBuilder result = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        boolean inString = false;

        for (int index = 0; index < source.length(); index++) {
            char current = source.charAt(index);

            if (!inString) {
                if (current == '\'') {
                    inString = true;
                    literal.setLength(0);
                } else {
                    result.append(current);
                }
                continue;
            }

            if (current == '\'') {
                if (index + 1 < source.length() && source.charAt(index + 1) == '\'') {
                    literal.append('\'');
                    index++;
                } else {
                    result.append(encodeString(literal.toString()));
                    inString = false;
                }
                continue;
            }

            literal.append(current);
        }

        if (inString) {
            result.append('\'').append(literal);
        }

        return result.toString();
    }

    public String pixelsToMm(int value, int scale) {
        return PIXEL_TO_MM.multiply(BigDecimal.valueOf(value)).setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    public String toDelphiColor(String rawColor, String fallback) {
        if (rawColor == null || rawColor.isBlank()) {
            return fallback;
        }
        String value = rawColor.trim();
        if (value.startsWith("cl")) {
            return value;
        }
        if (value.startsWith("$") && value.length() == 9) {
            return value;
        }
        if (value.startsWith("#")) {
            String hex = value.length() == 9 ? value.substring(3) : value.substring(1);
            if (hex.length() == 6) {
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);
                return String.format("$00%02X%02X%02X", b, g, r);
            }
        }
        return fallback;
    }

    public String mapAlignment(String align) {
        if (align == null) {
            return "taLeftJustify";
        }
        return switch (align.trim().toLowerCase()) {
            case "center" -> "taCenter";
            case "right" -> "taRightJustify";
            default -> "taLeftJustify";
        };
    }

    public String ensureSemicolon(String declaration) {
        if (declaration == null) {
            return "";
        }
        String clean = declaration.trim();
        if (clean.isEmpty()) {
            return "";
        }
        return clean.endsWith(";") ? clean : clean + ";";
    }

    public String buildImplementationDeclaration(String declaration, String className) {
        String clean = ensureSemicolon(declaration);
        if (clean.isBlank()) {
            return "";
        }
        String prefix = "T" + className + ".";
        if (clean.contains(prefix)) {
            return clean;
        }

        String lowered = clean.toLowerCase();
        String[] keywords = {"class procedure ", "class function ", "procedure ", "function ", "constructor ", "destructor "};
        for (String keyword : keywords) {
            if (lowered.startsWith(keyword)) {
                return keyword.trim() + " " + prefix + clean.substring(keyword.length());
            }
        }
        return "procedure " + prefix + clean;
    }

    public String itemSizeValues(int left, int top, int width, int height) {
        return "Size.Values = (\n"
            + "  " + pixelsToMm(height, 12) + "\n"
            + "  " + pixelsToMm(left, 12) + "\n"
            + "  " + pixelsToMm(top, 12) + "\n"
            + "  " + pixelsToMm(width, 12) + ")";
    }

    public String bandSizeValues(int height, int width) {
        return "Size.Values = (\n"
            + "  " + pixelsToMm(height, 18) + "\n"
            + "  " + pixelsToMm(width, 18) + ")";
    }
}
