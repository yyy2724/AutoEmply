package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class FormStructure {

    private String title;
    private int titleFontSize = 20;
    private List<FormSection> sections = new ArrayList<>();
    private List<FooterElement> footer = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getTitleFontSize() { return titleFontSize; }
    public void setTitleFontSize(int titleFontSize) { this.titleFontSize = titleFontSize; }
    public List<FormSection> getSections() { return sections; }
    public void setSections(List<FormSection> sections) { this.sections = sections; }
    public List<FooterElement> getFooter() { return footer; }
    public void setFooter(List<FooterElement> footer) { this.footer = footer; }
}
