package health.autoemplyserver.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.ArrayList;
import java.util.List;

public class TableRow {

    private String heightHint = "standard";
    private List<TableCell> cells = new ArrayList<>();
    private boolean headerRow;

    public String getHeightHint() { return heightHint; }
    public void setHeightHint(String heightHint) { this.heightHint = heightHint; }
    public List<TableCell> getCells() { return cells; }
    public void setCells(List<TableCell> cells) { this.cells = cells; }
    public boolean isHeaderRow() { return headerRow; }
    @JsonAlias("isHeaderRow")
    public void setHeaderRow(boolean headerRow) { this.headerRow = headerRow; }
}
