package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class LayoutSpec {

    private List<LayoutItem> items = new ArrayList<>();
    private PasSpec pas;

    public List<LayoutItem> getItems() { return items; }
    public void setItems(List<LayoutItem> items) { this.items = items; }
    public PasSpec getPas() { return pas; }
    public void setPas(PasSpec pas) { this.pas = pas; }
}
