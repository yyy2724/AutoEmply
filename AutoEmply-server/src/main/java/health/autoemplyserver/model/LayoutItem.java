package health.autoemplyserver.model;

public class LayoutItem {

    private String name;
    private String type;
    private int left;
    private int top;
    private int width;
    private int height;
    private String caption;
    private String align;
    private Integer fontSize;
    private Boolean bold;
    private Boolean transparent;
    private String textColor;
    private String orientation;
    private Integer thickness;
    private String strokeColor;
    private String fillColor;
    private Boolean filled;
    private Boolean stretch;

    public LayoutItem copy() {
        LayoutItem copy = new LayoutItem();
        copy.name = name;
        copy.type = type;
        copy.left = left;
        copy.top = top;
        copy.width = width;
        copy.height = height;
        copy.caption = caption;
        copy.align = align;
        copy.fontSize = fontSize;
        copy.bold = bold;
        copy.transparent = transparent;
        copy.textColor = textColor;
        copy.orientation = orientation;
        copy.thickness = thickness;
        copy.strokeColor = strokeColor;
        copy.fillColor = fillColor;
        copy.filled = filled;
        copy.stretch = stretch;
        return copy;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getLeft() { return left; }
    public void setLeft(int left) { this.left = left; }
    public int getTop() { return top; }
    public void setTop(int top) { this.top = top; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }
    public String getAlign() { return align; }
    public void setAlign(String align) { this.align = align; }
    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
    public Boolean getBold() { return bold; }
    public void setBold(Boolean bold) { this.bold = bold; }
    public Boolean getTransparent() { return transparent; }
    public void setTransparent(Boolean transparent) { this.transparent = transparent; }
    public String getTextColor() { return textColor; }
    public void setTextColor(String textColor) { this.textColor = textColor; }
    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
    public Integer getThickness() { return thickness; }
    public void setThickness(Integer thickness) { this.thickness = thickness; }
    public String getStrokeColor() { return strokeColor; }
    public void setStrokeColor(String strokeColor) { this.strokeColor = strokeColor; }
    public String getFillColor() { return fillColor; }
    public void setFillColor(String fillColor) { this.fillColor = fillColor; }
    public Boolean getFilled() { return filled; }
    public void setFilled(Boolean filled) { this.filled = filled; }
    public Boolean getStretch() { return stretch; }
    public void setStretch(Boolean stretch) { this.stretch = stretch; }
}
