package health.autoemplyserver.service.delphi;

import health.autoemplyserver.model.PasMethodSpec;
import health.autoemplyserver.model.PasSpec;
import health.autoemplyserver.service.ComponentRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DelphiPasWriter {

    private final DelphiValueFormatter formatter;

    public DelphiPasWriter(DelphiValueFormatter formatter) {
        this.formatter = formatter;
    }

    public String write(String formName, String className, List<ComponentRef> components, PasSpec pasSpec) {
        List<String> units = buildUsesList(pasSpec);
        Map<String, NormalizedMethod> methodMap = buildMethodMap(pasSpec);
        List<EventHandler> eventHandlers = collectEventHandlers(components, methodMap);

        StringBuilder builder = new StringBuilder();

        // unit header
        builder.append("unit ").append(formName).append(";\n\n");
        builder.append("interface\n\n");

        // uses clause
        appendUsesClause(builder, units);

        // type declaration
        builder.append("\ntype\n");
        builder.append("  T").append(className).append(" = class(TForm)\n");
        for (ComponentRef component : components) {
            builder.append("    ").append(component.name()).append(": ").append(component.type()).append(";\n");
        }

        // event handler declarations in published section (before private)
        for (EventHandler handler : eventHandlers) {
            builder.append("    ").append(buildEventDeclaration(handler.name())).append('\n');
        }

        builder.append("  private\n");
        builder.append("    { Private declarations }\n");
        builder.append("  public\n");
        builder.append("    { Public declarations }\n");

        // additional methods from AI (non-event)
        for (Map.Entry<String, NormalizedMethod> entry : methodMap.entrySet()) {
            if (eventHandlers.stream().noneMatch(eh -> eh.methodKey().equals(entry.getKey()))) {
                builder.append("    ").append(stripClassPrefix(entry.getValue().declaration(), className)).append('\n');
            }
        }

        builder.append("  end;\n\n");

        // var section
        builder.append("var\n");
        builder.append("  ").append(className).append(": T").append(className).append(";\n\n");

        // implementation
        builder.append("implementation\n\n");
        builder.append("{$R *.dfm}\n\n");

        // event handler implementations
        for (EventHandler handler : eventHandlers) {
            builder.append(formatter.buildImplementationDeclaration(buildEventDeclaration(handler.name()), className)).append('\n');
            appendMethodBody(builder, handler.body());
            builder.append('\n');
        }

        // remaining methods (non-event)
        for (Map.Entry<String, NormalizedMethod> entry : methodMap.entrySet()) {
            if (eventHandlers.stream().noneMatch(eh -> eh.methodKey().equals(entry.getKey()))) {
                NormalizedMethod method = entry.getValue();
                builder.append(formatter.buildImplementationDeclaration(method.declaration(), className)).append('\n');
                appendMethodBody(builder, method.body());
                builder.append('\n');
            }
        }

        builder.append("end.");
        return builder.toString();
    }

    private List<String> buildUsesList(PasSpec pasSpec) {
        List<String> units = new ArrayList<>(List.of(
            "Windows", "Messages", "SysUtils", "Variants", "Classes",
            "Graphics", "Controls", "Forms", "Dialogs",
            "QuickRpt", "QRCtrls", "ExtCtrls"
        ));
        if (pasSpec != null && pasSpec.getUses() != null) {
            pasSpec.getUses().stream()
                .map(unit -> unit == null ? "" : unit.trim().replaceAll(";$", "").trim())
                .filter(unit -> !unit.isBlank())
                .filter(unit -> units.stream().noneMatch(existing -> existing.equalsIgnoreCase(unit)))
                .forEach(units::add);
        }
        return units;
    }

    private void appendUsesClause(StringBuilder builder, List<String> units) {
        builder.append("uses\n");
        int lineLength = 0;
        builder.append("  ");
        for (int i = 0; i < units.size(); i++) {
            String unit = units.get(i);
            boolean isLast = i == units.size() - 1;
            String suffix = isLast ? ";" : ",";

            if (lineLength > 0 && lineLength + unit.length() + 2 > 70) {
                builder.append("\n  ");
                lineLength = 0;
            }
            if (lineLength > 0) {
                builder.append(' ');
                lineLength++;
            }
            builder.append(unit).append(suffix);
            lineLength += unit.length() + suffix.length();
        }
        builder.append('\n');
    }

    private Map<String, NormalizedMethod> buildMethodMap(PasSpec pasSpec) {
        Map<String, NormalizedMethod> map = new LinkedHashMap<>();
        if (pasSpec == null || pasSpec.getMethods() == null || pasSpec.getMethods().isEmpty()) {
            return map;
        }

        for (PasMethodSpec method : pasSpec.getMethods()) {
            String declaration = formatter.ensureSemicolon(method.getDeclaration());
            if (declaration.isBlank()) {
                continue;
            }

            MethodBody body = normalizeMethodBody(method.getBody());

            String key = extractMethodName(declaration).toLowerCase();
            map.put(key, new NormalizedMethod(declaration, body));
        }
        return map;
    }

    private List<EventHandler> collectEventHandlers(List<ComponentRef> components, Map<String, NormalizedMethod> methodMap) {
        List<EventHandler> handlers = new ArrayList<>();
        for (ComponentRef component : components) {
            if (component.onPrintHandler() == null) {
                continue;
            }
            String handlerName = component.onPrintHandler();
            String key = handlerName.toLowerCase();

            // find matching AI-provided method body
            NormalizedMethod matched = methodMap.get(key);
            MethodBody body;
            if (matched != null) {
                body = matched.body();
            } else {
                body = MethodBody.todo();
            }

            handlers.add(new EventHandler(handlerName, key, component.name(), body));
        }
        return handlers;
    }

    private String extractMethodName(String declaration) {
        String clean = declaration.replaceAll(";$", "").trim();
        // remove keyword prefix
        String[] keywords = {"class procedure ", "class function ", "procedure ", "function ", "constructor ", "destructor "};
        for (String keyword : keywords) {
            if (clean.toLowerCase().startsWith(keyword)) {
                clean = clean.substring(keyword.length()).trim();
                break;
            }
        }
        // remove class prefix (TClassName.)
        int dotIndex = clean.indexOf('.');
        if (dotIndex >= 0) {
            clean = clean.substring(dotIndex + 1).trim();
        }
        // remove parameters
        int parenIndex = clean.indexOf('(');
        if (parenIndex >= 0) {
            clean = clean.substring(0, parenIndex).trim();
        }
        // remove return type
        int colonIndex = clean.indexOf(':');
        if (colonIndex >= 0) {
            clean = clean.substring(0, colonIndex).trim();
        }
        return clean;
    }

    private int leadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private String stripClassPrefix(String declaration, String className) {
        String prefix = "T" + className + ".";
        String result = declaration;
        // "procedure TFormQREmply03.FormClose(..." → "procedure FormClose(..."
        int prefixIndex = result.indexOf(prefix);
        if (prefixIndex >= 0) {
            result = result.substring(0, prefixIndex) + result.substring(prefixIndex + prefix.length());
        }
        return formatter.ensureSemicolon(result);
    }

    private String buildEventDeclaration(String handlerName) {
        return "procedure " + handlerName + "(Sender: TObject; var Value: String);";
    }

    private void appendMethodBody(StringBuilder builder, MethodBody body) {
        for (String line : body.declarations()) {
            builder.append(formatter.encodePascalStringLiterals(line)).append('\n');
        }
        builder.append("begin\n");
        for (String line : body.statements()) {
            String encodedLine = formatter.encodePascalStringLiterals(line);
            builder.append(encodedLine.isBlank() ? "" : "  " + encodedLine).append('\n');
        }
        builder.append("end;\n");
    }

    private MethodBody normalizeMethodBody(List<String> sourceBody) {
        List<String> lines = new ArrayList<>();
        for (String line : sourceBody == null ? List.<String>of() : sourceBody) {
            lines.add(Objects.toString(line, ""));
        }

        int firstContent = firstContentIndex(lines);
        if (firstContent < 0) {
            return MethodBody.todo();
        }

        int lastContent = lastContentIndex(lines);
        int beginIndex = findStandaloneBegin(lines, firstContent, lastContent);
        if (beginIndex >= 0 && isStandaloneEnd(lines.get(lastContent))) {
            List<String> declarations = normalizeIndentedLines(lines.subList(0, beginIndex));
            List<String> statements = normalizeIndentedLines(lines.subList(beginIndex + 1, lastContent));
            return new MethodBody(
                declarations,
                statements.isEmpty() ? List.of("// TODO") : statements
            );
        }

        List<String> trimmed = trimBlankEdges(lines);
        List<String> declarations = new ArrayList<>();
        int index = collectLeadingDeclarations(trimmed, declarations);
        List<String> statements = normalizeIndentedLines(trimmed.subList(index, trimmed.size()));
        return new MethodBody(
            normalizeIndentedLines(declarations),
            statements.isEmpty() ? List.of("// TODO") : statements
        );
    }

    private int collectLeadingDeclarations(List<String> lines, List<String> declarations) {
        int index = 0;
        while (index < lines.size()) {
            String trimmed = lines.get(index).trim();
            if (trimmed.isEmpty() || isCommentLine(trimmed)) {
                declarations.add(lines.get(index));
                index++;
                continue;
            }
            if (trimmed.equalsIgnoreCase("var")) {
                declarations.add(lines.get(index++));
                while (index < lines.size()) {
                    String declarationLine = lines.get(index);
                    String declarationTrimmed = declarationLine.trim();
                    if (declarationTrimmed.isEmpty() || isCommentLine(declarationTrimmed) || looksLikeVariableDeclaration(declarationTrimmed)) {
                        declarations.add(declarationLine);
                        index++;
                        continue;
                    }
                    return index;
                }
                return index;
            }
            return index;
        }
        return index;
    }

    private boolean looksLikeVariableDeclaration(String line) {
        if (!line.endsWith(";")) {
            return false;
        }
        if (line.contains(":=") || line.contains("=")) {
            return false;
        }
        return line.contains(":");
    }

    private boolean isCommentLine(String line) {
        return line.startsWith("//") || line.startsWith("{") || line.startsWith("(*");
    }

    private int findStandaloneBegin(List<String> lines, int from, int to) {
        for (int index = from; index <= to; index++) {
            if (lines.get(index).trim().equalsIgnoreCase("begin")) {
                return index;
            }
        }
        return -1;
    }

    private boolean isStandaloneEnd(String line) {
        String trimmed = line.trim();
        return trimmed.equalsIgnoreCase("end") || trimmed.equalsIgnoreCase("end;");
    }

    private int firstContentIndex(List<String> lines) {
        for (int index = 0; index < lines.size(); index++) {
            if (!lines.get(index).trim().isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private int lastContentIndex(List<String> lines) {
        for (int index = lines.size() - 1; index >= 0; index--) {
            if (!lines.get(index).trim().isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private List<String> trimBlankEdges(List<String> lines) {
        int start = firstContentIndex(lines);
        if (start < 0) {
            return List.of();
        }
        int end = lastContentIndex(lines);
        return new ArrayList<>(lines.subList(start, end + 1));
    }

    private List<String> normalizeIndentedLines(List<String> lines) {
        List<String> trimmed = trimBlankEdges(lines);
        if (trimmed.isEmpty()) {
            return List.of();
        }
        int minIndent = trimmed.stream()
            .filter(line -> !line.trim().isEmpty())
            .mapToInt(this::leadingSpaces)
            .min()
            .orElse(0);
        return trimmed.stream()
            .map(line -> {
                if (line.trim().isEmpty()) {
                    return "";
                }
                int strip = Math.min(minIndent, leadingSpaces(line));
                return line.substring(strip);
            })
            .toList();
    }

    private record NormalizedMethod(String declaration, MethodBody body) {
    }

    private record EventHandler(String name, String methodKey, String componentName, MethodBody body) {
    }

    private record MethodBody(List<String> declarations, List<String> statements) {

        private static MethodBody todo() {
            return new MethodBody(List.of(), List.of("// TODO"));
        }
    }
}
