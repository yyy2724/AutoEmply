package health.autoemplyserver.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public class ColumnDef {

    private double widthFraction;
    private String header;
    private boolean headerColumn;

    public double getWidthFraction() { return widthFraction; }
    public void setWidthFraction(double widthFraction) { this.widthFraction = widthFraction; }
    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    public boolean isHeaderColumn() { return headerColumn; }
    @JsonAlias("isHeaderColumn")
    public void setHeaderColumn(boolean headerColumn) { this.headerColumn = headerColumn; }
}
