package health.autoemplyserver.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class TableCell {

    private String text = "";
    private String align = "Left";
    private boolean bold;
    private int fontSize = 9;
    private int colSpan = 1;
    private int rowSpan = 1;
    private boolean hasBackground;
    private String fieldName;
    private String textColor;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getAlign() { return align; }
    public void setAlign(String align) { this.align = align; }
    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public int getColSpan() { return colSpan; }
    public void setColSpan(int colSpan) { this.colSpan = colSpan; }
    public int getRowSpan() { return rowSpan; }
    public void setRowSpan(int rowSpan) { this.rowSpan = rowSpan; }
    public boolean isHasBackground() { return hasBackground; }
    @JsonAlias("hasBackground")
    public void setHasBackground(boolean hasBackground) { this.hasBackground = hasBackground; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
}
