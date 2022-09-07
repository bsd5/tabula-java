package technology.tabula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.lang.Thread;
import java.util.HashMap;
import java.util.ListIterator;
import java.lang.Float;
import technology.tabula.extractors.ExtractionAlgorithm;

@SuppressWarnings("serial")
public class TableWithRulingLines extends Table {

    List<Ruling> verticalRulings, horizontalRulings;
    RectangleSpatialIndex<Cell> si = new RectangleSpatialIndex<>();

    public TableWithRulingLines(Rectangle area, List<Cell> cells, List<Ruling> horizontalRulings, List<Ruling> verticalRulings, ExtractionAlgorithm extractionAlgorithm, int pageNumber) {
        super(extractionAlgorithm);
        this.setRect(area);
        this.verticalRulings = verticalRulings;
        this.horizontalRulings = horizontalRulings;
        this.addCells(cells);
        this.setPageNumber(pageNumber);
    }

    private void addCells(List<Cell> cells) {

        if (cells.isEmpty()) {
            return;
        }

        for (Cell ce: cells) {
            si.add(ce);
        }

        List<List<Cell>> rowsOfCells = rowsOfCells(cells);
        for (int i = 0; i < rowsOfCells.size(); i++) {
            List<Cell> row = rowsOfCells.get(i);
            Iterator<Cell> rowCells = row.iterator();
            if (row.size() == 0){
                continue;
            }
            Cell cell = rowCells.next();
            List<List<Cell>> others = rowsOfCells(
                si.contains(
                    new Rectangle(
                        cell.getBottom(),
                        si.getBounds().getLeft(),
                        cell.getLeft() - si.getBounds().getLeft(),
                        si.getBounds().getBottom() - cell.getBottom()
                    )
                )
            );
            int startColumn = 0;
            for (List<Cell> r: others) {
                startColumn = Math.max(startColumn, r.size());
            }
            this.add(cell, i, startColumn++);
            while (rowCells.hasNext()) {
                this.add(rowCells.next(), i, startColumn++);
            }
        }
    }
    private static int compareRounded(double d1, double d2) {
        float d1Rounded = Utils.round(d1, 2);
        float d2Rounded = Utils.round(d2, 2);
        if (d1Rounded < d2Rounded){
            return -1;
        }
        if (d1Rounded > d2Rounded){
            return 1;            // Neither val is NaN, thisVal is larger
        }
        return 0;
    }
    private static final Comparator<Cell> X_FIRST_CELL_COMPARATOR = (cell1, cell2) -> {
        int compareX = compareRounded(cell1.getX(), cell2.getX());
        return compareX;
    };

    /* Cell midline based row gathering
     *
     *  Find the vertical center of the shortest cell in a row, and use it to match the
     *  other cells on the same row.
     *
     *  TODO: Optionally join horizontally merged cells based on being on the horizontal center
     *  of a given column
     */
    private static List<List<Cell>> rowsOfCells(List<Cell> cells) {

        List<List<Cell>> rows = new ArrayList<>();
        if (cells.size() == 0){
            return rows;
        }
        List<Cell> firstRow = fetchFirstRow(cells);

        if(firstRow.size() == 0){
            return rows;
        }
        int firstIndex = 0;
        Cell firstCell = firstRow.get(firstIndex);
        double rowTop = firstCell.getTop();
        double rowHeight = firstCell.getHeight();
        double midPointOffset = rowHeight/2;
        double midPoint = (rowTop+midPointOffset);

        while(!lineIncludesAll(midPoint, firstRow))
        {
            midPointOffset = midPointOffset/2;
            midPoint = rowTop+midPointOffset;
            if (midPointOffset < 1){
                System.out.println("midPointOffset < 1 !!!");
                System.exit(0);
            }
        }

        double shortestCellHeight = 10000;
        double midline = midPoint;
        double nextMidline=0;
        double cellMidline =0;
        String cellText = "";
        double lastCellBottom = 0;
        for(Cell mb: cells){
            if (mb.getBottom() > lastCellBottom){
                lastCellBottom = mb.getBottom();
            }
        }
        for(int j = 0; (midline < lastCellBottom); j++){
            List<Cell> currentRow = new ArrayList<>();
            Cell currentCell;
            for(int i = 0; i < cells.size(); i++){
                currentCell = cells.get(i);
                cellMidline = currentCell.getBottom()-(currentCell.getHeight()/2);

                if (currentCell.getBottom() > midline){
                    if (currentCell.getTop() < midline){
                        if(currentCell.getWidth() >2){
                            currentRow.add(currentCell);
                            if(currentCell.getHeight() < shortestCellHeight){
                                shortestCellHeight = currentCell.getHeight();
                                nextMidline = cellMidline+shortestCellHeight;
                            }
                        }
                    }
                }
            }
            currentRow.sort(X_FIRST_CELL_COMPARATOR);
            if(currentRow.size()>0){
                for(Cell sortedCell: currentRow){
                    cellText = sortedCell.getText().trim();
                    System.out.format("%16s", cellText);
                }
                System.out.println("\n");
            } else {
                System.err.println("MTROW");
            }
            rows.add(currentRow);
            midline = nextMidline;
            if(midline > lastCellBottom){
                break;
            }
            shortestCellHeight = 1000;

        }

        return rows;
    }
    private static boolean lineIncludesAll(double centerLine, List<Cell> firstRow){
        for(Cell cell: firstRow){
            if (!cellOnRow(centerLine, cell)){
                return false;
            }
        }
        return true;
    }

    private static boolean cellOnRow(double midline, Cell cell) {

        return (midline > cell.getTop() && midline < cell.getBottom());
    }
    private static List<Cell> fetchFirstRow(List<Cell> allCells){
        List<Cell> firstRow = new ArrayList<Cell>();
        double previousX = 0;
        for (Cell cell : allCells) {
            if (cell.getX() < previousX) {
                if(firstRow.get(0).getHeight() < 5){
                    firstRow.clear();
                }
                else{
                    for(Cell maybeYuk: firstRow){
                        if ((maybeYuk.getHeight() >=5) && (maybeYuk.getWidth() >5)){
                            return firstRow;
                        }
                    }
                    firstRow.clear();
                    System.err.println("Returning empty row");
                    return firstRow;
                }
            }
            firstRow.add(cell);
            previousX = cell.getX();
        }
        for(Cell maybeYuk: firstRow){
            if ((maybeYuk.getHeight() >=5) && (maybeYuk.getWidth() >5)){
                return firstRow;
            }
        }
        firstRow.clear();
        System.out.println("Returning empty row");
        return firstRow;
    }
}