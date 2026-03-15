package health.autoemplyserver.model;

public class FooterElement {

    private String elementType = "text";
    private String text;
    private String align = "Left";
    private boolean bold;
    private int fontSize = 9;
    private double xFraction;
    private double widthFraction = 0.5;

    public String getElementType() { return elementType; }
    public void setElementType(String elementType) { this.elementType = elementType; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getAlign() { return align; }
    public void setAlign(String align) { this.align = align; }
    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }
    public int getFontSize() { return fontSize; }
    public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    public double getXFraction() { return xFraction; }
    public void setXFraction(double xFraction) { this.xFraction = xFraction; }
    public double getWidthFraction() { return widthFraction; }
    public void setWidthFraction(double widthFraction) { this.widthFraction = widthFraction; }
}
