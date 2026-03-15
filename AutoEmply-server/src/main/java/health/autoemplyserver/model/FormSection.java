package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class FormSection {

    private String sectionType = "table";
    private String label;
    private boolean hasHeaderBackground = true;
    private TableDef table;
    private List<FreeformElement> elements = new ArrayList<>();

    public String getSectionType() { return sectionType; }
    public void setSectionType(String sectionType) { this.sectionType = sectionType; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isHasHeaderBackground() { return hasHeaderBackground; }
    public void setHasHeaderBackground(boolean hasHeaderBackground) { this.hasHeaderBackground = hasHeaderBackground; }
    public TableDef getTable() { return table; }
    public void setTable(TableDef table) { this.table = table; }
    public List<FreeformElement> getElements() { return elements; }
    public void setElements(List<FreeformElement> elements) { this.elements = elements; }
}
