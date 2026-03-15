package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class PasMethodSpec {

    private String declaration;
    private List<String> body = new ArrayList<>();

    public String getDeclaration() { return declaration; }
    public void setDeclaration(String declaration) { this.declaration = declaration; }
    public List<String> getBody() { return body; }
    public void setBody(List<String> body) { this.body = body; }
}
