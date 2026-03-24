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
            builder.append("    procedure ").append(handler.name()).append("(Sender: TObject);\n");
        }

        builder.append("  private\n");
        builder.append("    { Private declarations }\n");
        builder.append("  public\n");
        builder.append("    { Public declarations }\n");

        // additional methods from AI (non-event)
        for (Map.Entry<String, NormalizedMethod> entry : methodMap.entrySet()) {
            if (eventHandlers.stream().noneMatch(eh -> eh.methodKey().equals(entry.getKey()))) {
                builder.append("    ").append(entry.getValue().declaration()).append('\n');
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
            builder.append("procedure T").append(className).append('.').append(handler.name()).append("(Sender: TObject);\n");
            builder.append("begin\n");
            for (String line : handler.body()) {
                builder.append(line.isBlank() ? "" : "  " + line).append('\n');
            }
            builder.append("end;\n\n");
        }

        // remaining methods (non-event)
        for (Map.Entry<String, NormalizedMethod> entry : methodMap.entrySet()) {
            if (eventHandlers.stream().noneMatch(eh -> eh.methodKey().equals(entry.getKey()))) {
                NormalizedMethod method = entry.getValue();
                builder.append(formatter.buildImplementationDeclaration(method.declaration(), className)).append('\n');
                builder.append("begin\n");
                for (String line : method.body()) {
                    builder.append(line.isBlank() ? "" : "  " + line).append('\n');
                }
                builder.append("end;\n\n");
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

            List<String> body = new ArrayList<>();
            List<String> sourceBody = method.getBody() == null ? List.of() : method.getBody();
            for (String line : sourceBody) {
                String trimmed = Objects.toString(line, "").trim();
                if (trimmed.equalsIgnoreCase("begin")
                    || trimmed.equalsIgnoreCase("end")
                    || trimmed.equalsIgnoreCase("end;")) {
                    continue;
                }
                body.add(trimmed);
            }
            if (body.isEmpty()) {
                body.add("// TODO");
            }

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
            List<String> body;
            if (matched != null) {
                body = matched.body();
            } else {
                body = List.of("// TODO");
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

    private record NormalizedMethod(String declaration, List<String> body) {
    }

    private record EventHandler(String name, String methodKey, String componentName, List<String> body) {
    }
}
