package health.autoemplyserver.service.delphi;

import health.autoemplyserver.model.PasMethodSpec;
import health.autoemplyserver.model.PasSpec;
import health.autoemplyserver.service.ComponentRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DelphiPasWriter {

    private final DelphiValueFormatter formatter;

    public DelphiPasWriter(DelphiValueFormatter formatter) {
        this.formatter = formatter;
    }

    public String write(String formName, String className, List<ComponentRef> components, PasSpec pasSpec) {
        List<String> units = new ArrayList<>(List.of("Windows", "Messages", "SysUtils", "Variants", "Classes", "Graphics", "Controls", "Forms", "Dialogs", "QuickRpt", "QRCtrls", "ExtCtrls"));
        if (pasSpec != null && pasSpec.getUses() != null) {
            pasSpec.getUses().stream()
                .map(unit -> unit == null ? "" : unit.trim().replaceAll(";$", "").trim())
                .filter(unit -> !unit.isBlank())
                .filter(unit -> units.stream().noneMatch(existing -> existing.equalsIgnoreCase(unit)))
                .forEach(units::add);
        }

        List<NormalizedMethod> methods = normalizeMethods(pasSpec);

        StringBuilder builder = new StringBuilder();
        builder.append("unit ").append(formName).append(";\n\n")
            .append("interface\n\n")
            .append("uses\n");
        for (int index = 0; index < units.size(); index++) {
            builder.append("  ").append(units.get(index)).append(index == units.size() - 1 ? ";\n" : ",\n");
        }

        builder.append("\n")
            .append("type\n")
            .append("  T").append(className).append(" = class(TForm)\n");
        for (ComponentRef component : components) {
            builder.append("    ").append(component.name()).append(": ").append(component.type()).append(";\n");
        }
        builder.append("  private\n")
            .append("  public\n");
        for (NormalizedMethod method : methods) {
            builder.append("    ").append(method.declaration()).append('\n');
        }
        builder.append("  end;\n\n")
            .append("var\n")
            .append("  ").append(formName).append(": T").append(className).append(";\n\n")
            .append("implementation\n\n")
            .append("{$R *.dfm}\n\n");

        for (NormalizedMethod method : methods) {
            builder.append(formatter.buildImplementationDeclaration(method.declaration(), className)).append('\n')
                    .append("begin\n");
            for (String line : method.body()) {
                builder.append(line.isBlank() ? "" : "  " + line.trim()).append('\n');
            }
            builder.append("end;\n\n");
        }

        builder.append("end.");
        return builder.toString();
    }

    private List<NormalizedMethod> normalizeMethods(PasSpec pasSpec) {
        if (pasSpec == null || pasSpec.getMethods() == null || pasSpec.getMethods().isEmpty()) {
            return List.of();
        }

        List<NormalizedMethod> methods = new ArrayList<>();
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

            methods.add(new NormalizedMethod(declaration, body));
        }
        return methods;
    }

    private record NormalizedMethod(String declaration, List<String> body) {
    }
}
