package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class PasSpec {

    private List<String> uses = new ArrayList<>();
    private List<PasMethodSpec> methods = new ArrayList<>();

    public List<String> getUses() { return uses; }
    public void setUses(List<String> uses) { this.uses = uses; }
    public List<PasMethodSpec> getMethods() { return methods; }
    public void setMethods(List<PasMethodSpec> methods) { this.methods = methods; }
}
