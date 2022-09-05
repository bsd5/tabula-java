package technology.tabula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.ListIterator;
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
            System.out.format("rowsOfCells.get(%d):\n", i);
            for(Cell rowCell: row){
                System.out.println("\t"+rowCell);

            }
            System.out.println("");

            Iterator<Cell> rowCells = row.iterator();
            Cell cell = rowCells.next();
            List<List<Cell>> others = rowsOfCells(
                    si.contains(
                            new Rectangle(cell.getBottom(), si.getBounds().getLeft(), cell.getLeft() - si.getBounds().getLeft(),
                                    si.getBounds().getBottom() - cell.getBottom())
                            ));
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

    private static List<List<Cell>> rowsOfCells(List<Cell> cells) {
        List<List<Cell>> rows = new ArrayList<>();
        List<Cell> firstRow = fetchFirstRow(cells);
        System.out.println("First row: "+firstRow.size());
        for(Cell c: firstRow){
            System.out.println(c);
        }
        System.out.println("********* Getting rowMidlineSet ********");
        
        double[] midPointSet = getRowMidlineSet(firstRow);
        System.out.println("********* DONE Getting rowMidlineSet ********");
        double rowHeight = midPointSet[0];
        double rowTop = midPointSet[1];
        double midPointOffset = midPointSet[2];
        double midline = rowTop + midPointOffset;

        // rows.add(firstRow);

        List<Cell> currentRow = new ArrayList<>();
        System.out.println("Fetching rows, starting at midline: "+midline);
        System.out.println("row.size(): "+currentRow.size());
        Cell currentCell;
        for(int i = 0; i < cells.size(); i++){
            currentCell = cells.get(i);
            if (currentCell.getBottom() > midline){
                if (currentCell.getTop() < midline){
                    System.out.println("Cell on midline: "+currentCell);
                    currentRow.add(currentCell);
                }
            }
        }
        
        return rows;
    }
    /*
    * Step 1. gather first row: we _know_ they're on the same row because they share a top.
    * Step 2. Binary search(ish) through the height of the first cell by halving until all cells
    *   in the row are included. * This is 1/2 your row hight.
    * Step 3. Previous midline + row height is the offset into the middle of each row
    *  - only works for even multiples of the first row height;
    *
    */
    private static double[] getRowMidlineSet(List<Cell> firstRow){
        System.out.println("Getting midline set for row:");
        System.out.println(firstRow);

        int firstIndex = 0;
        System.out.println("firstRow.size(): "+firstRow.size());
        
        Cell firstCell = firstRow.get(firstIndex);
        double rowTop = firstCell.getTop();
        double rowBottom = firstCell.getBottom();
        double rowHeight = firstCell.getHeight();
        double midPointOffset = rowHeight/2;
        double midPoint = (rowTop+midPointOffset);
        System.out.println("\nTrying first midpoint:  top:"+rowTop+ " bottom: "+rowBottom+" height: "+rowHeight+" midpoint: "+midPoint);

        while(!lineIncludesAll(midPoint, firstRow))
        {
            System.out.println("Trying next midpoint:  top:"+rowTop+ " height: "+rowHeight+" midpoint: "+midPoint);

            midPointOffset = midPointOffset/2;
            midPoint = rowTop+midPointOffset;
        }
        double[] midPointSet = new double[3];
        midPointSet[0] = rowHeight;
        midPointSet[1] = rowTop;
        midPointSet[2] = midPointOffset;

        return midPointSet;
    }

    private static boolean lineIncludesAll(double centerLine, List<Cell> firstRow){
        for(Cell cell: firstRow){
            if (!cellOnRow(centerLine, cell)){
                return false;
            }
        }
        return true;
    }

    private static List<Cell> cellsOnRow(double midline, List<Cell> cells){
        List<Cell> cellsOnRowMidline = new ArrayList<>();
        System.out.println("\nFinding cells on row for midline "+midline);
        System.out.println("cells.size()"+cells.size());
        
        System.out.println("\n\n\nCells:"+cells+"\n\n\n");

        // for (int i = 0; i < crunchifyList.size(); i++) {
        //     System.out.println(crunchifyList.get(i));
        // }
        Cell cell;
        for(int cellIndex=0; cellIndex < cells.size(); cellIndex++){
            System.out.println("cellIndex="+cellIndex);
            cell = cells.get(cellIndex);
            System.out.println("\nCell: "+ cell);
            if (cellOnRow(midline, cell)){
                System.out.println("\tAdding: "+ cell);
                cellsOnRowMidline.add(cell);
            }
            else{
                System.out.println("\tCell not on midline");
            }
        }
        System.out.println("Returning cellsOnRowMidline of length "+cellsOnRowMidline.size());

        return cellsOnRowMidline;
    }

    private static boolean cellOnRow(double midline, Cell cell) {
        System.out.println("\tChecking cell on midline"+cell+"\t\t"+midline+"\n");

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
                    return firstRow;
                }
            }
            firstRow.add(cell);
            previousX = cell.getX();
        }
        return firstRow;
    }

    /* Post-processing:
    * if all the cells in the first or last column are blank when squished, delete the column
    */
}