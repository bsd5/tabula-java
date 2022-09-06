package technology.tabula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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

    private static List<List<Cell>> rowsOfCells(List<Cell> cells) {
        List<List<Cell>> rows = new ArrayList<>();
        if (cells.size() == 0){
            return rows;
        }
        List<Cell> firstRow = fetchFirstRow(cells);

        int firstIndex = 0;
        System.out.println("firstRow.size(): "+firstRow.size());

        Cell firstCell = firstRow.get(firstIndex);
        double rowTop = firstCell.getTop();
        double rowBottom = firstCell.getBottom();
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

        // = "%-3.3s";  // fixed size 3 characters, left aligned

        // System.out.format("midPointOffset %.2f\n", midPointOffset);
        // System.out.format("midPoint %.2f\n\n", midPoint);
        double shortestCellHeight = 10000;
        double midline = midPoint;
        double nextMidline=0;
        double cellMidline =0;
        double midlineDelta=0;
        String cellText = "";
        String cellDebugText = "";
        for(int j = 0; j <10; j++){
            // System.out.format("\nmidline: %.2f\n\ttext: ", midline);

            List<Cell> currentRow = new ArrayList<>();
            Cell currentCell;
            for(int i = 0; i < cells.size(); i++){
                currentCell = cells.get(i);
                cellMidline = currentCell.getBottom()-(currentCell.getHeight()/2);

                if (currentCell.getBottom() > midline){
                    if (currentCell.getTop() < midline){
                        if(currentCell.getWidth() >2){
                            midlineDelta = (midline-cellMidline);
                            currentRow.add(currentCell);
                            if(currentCell.getHeight() < shortestCellHeight){
                                shortestCellHeight = currentCell.getHeight();
                                nextMidline = cellMidline+shortestCellHeight;
                            }
                        }
                    }
                }
            }
            /* next_given_midline = shortestCell.top+(shortestCell.height/2); */
            currentRow.sort(X_FIRST_CELL_COMPARATOR);
            for(Cell sortedCell: currentRow){
                cellText = sortedCell.getText().trim();
                System.out.format("%16s", cellText);
            }
            System.out.println("\n");
            
            rows.add(currentRow);
            midline = nextMidline;
            shortestCellHeight = 1000;

        }
        System.out.println("\n\n**********************************\n\tRETURNING ROWS:\n"+rows+"\n***************************************");

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
                    return firstRow;
                }
            }
            firstRow.add(cell);
            previousX = cell.getX();
        }
        return firstRow;
    }


    // /*
    // * Step 1. gather first row: we _know_ they're on the same row because they share a top.
    // * Step 2. Binary search(ish) through the height of the first cell by halving until all cells
    // *   in the row are included. * This is 1/2 your row hight.
    // * Step 3. Previous midline + row height is the offset into the middle of each row
    // *  - only works for even multiples of the first row height;
    // *
    // */
    // private static double[] getRowMidlineSet(List<Cell> firstRow){
    //     System.out.println("Getting midline set for row:");
    //     System.out.println(firstRow);

    //     int firstIndex = 0;
    //     System.out.println("firstRow.size(): "+firstRow.size());

    //     Cell firstCell = firstRow.get(firstIndex);
    //     double rowTop = firstCell.getTop();
    //     double rowBottom = firstCell.getBottom();
    //     double rowHeight = firstCell.getHeight();
    //     double midPointOffset = rowHeight/2;
    //     double midPoint = (rowTop+midPointOffset);
    //     System.out.println("\nTrying first midpoint:  top:"+rowTop+ " bottom: "+rowBottom+" height: "+rowHeight+" midpoint: "+midPoint);

    //     while(!lineIncludesAll(midPoint, firstRow))
    //     {
    //         System.out.println("Trying next midpoint:  top:"+rowTop+ " height: "+rowHeight+" midpoint: "+midPoint);

    //         midPointOffset = midPointOffset/2;
    //         midPoint = rowTop+midPointOffset;
    //     }
    //     double[] midPointSet = new double[3];
    //     midPointSet[0] = rowHeight;
    //     midPointSet[1] = rowTop;
    //     midPointSet[2] = midPointOffset;

    //     return midPointSet;
    // }



    // private static List<Cell> cellsOnRow(double midline, List<Cell> cells){
    //     List<Cell> cellsOnRowMidline = new ArrayList<>();
    //     System.out.println("\nFinding cells on row for midline "+midline);
    //     System.out.println("cells.size()"+cells.size());

    //     System.out.println("\n\n\nCells:"+cells+"\n\n\n");


    //     Cell cell;
    //     for(int cellIndex=0; cellIndex < cells.size(); cellIndex++){
    //         System.out.println("cellIndex="+cellIndex);
    //         cell = cells.get(cellIndex);
    //         System.out.println("\nCell: "+ cell);
    //         if (cellOnRow(midline, cell)){
    //             System.out.println("\tAdding: "+ cell);
    //             cellsOnRowMidline.add(cell);
    //         }
    //         else{
    //             System.out.println("\tCell not on midline");
    //         }
    //     }
    //     System.out.println("Returning cellsOnRowMidline of length "+cellsOnRowMidline.size());

    //     return cellsOnRowMidline;
    // }
 /* Post-processing:
    * if all the cells in the first or last column are blank when squished, delete the column
    */
}