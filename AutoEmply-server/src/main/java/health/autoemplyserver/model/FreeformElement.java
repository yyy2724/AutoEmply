package health.autoemplyserver.model;

public class FreeformElement {

    private String elementType = "text";
    private String text;
    private double xFraction;
    private double yFraction;
    private double widthFraction = 0.1;
    private double heightFraction = 0.02;
    private String align = "Left";
    private boolean bold;
    private int fontSize = 9;

    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public double getXFraction() { return xFraction; }
    public void setXFraction(double xFraction) { this.xFraction = xFraction; }
    public double getYFraction() { return yFraction; }
    public void setYFraction(double yFraction) { this.yFraction = yFraction; }
    public double getWidthFraction() { return widthFraction; }
    public void setWidthFraction(double widthFraction) { this.widthFraction = widthFraction; }
    public double getHeightFraction() { return heightFraction; }
    public void setHeightFraction(double heightFraction) { this.heightFraction = heightFraction; }
    public String getAlign() { return align; }
    public void setAlign(String align) { this.align = align; }
    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
}
