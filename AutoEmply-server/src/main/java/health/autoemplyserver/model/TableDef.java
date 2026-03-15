package health.autoemplyserver.model;

import java.util.ArrayList;
import java.util.List;

public class TableDef {

    private List<ColumnDef> columns = new ArrayList<>();
    private List<TableRow> rows = new ArrayList<>();
    private boolean fullWidth = true;
    private double leftFraction;
    private double widthFraction = 1.0;

    public List<ColumnDef> getColumns() { return columns; }
    public void setColumns(List<ColumnDef> columns) { this.columns = columns; }
    public List<TableRow> getRows() { return rows; }
    public void setRows(List<TableRow> rows) { this.rows = rows; }
    public boolean isFullWidth() { return fullWidth; }
    public void setFullWidth(boolean fullWidth) { this.fullWidth = fullWidth; }
    public double getLeftFraction() { return leftFraction; }
    public void setLeftFraction(double leftFraction) { this.leftFraction = leftFraction; }
    public double getWidthFraction() { return widthFraction; }
    public void setWidthFraction(double widthFraction) { this.widthFraction = widthFraction; }
}
