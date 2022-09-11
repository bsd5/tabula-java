package technology.tabula;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import technology.tabula.extractors.ExtractionAlgorithm;

@SuppressWarnings("serial")
public class Table extends Rectangle {

	public static final Table empty() { return new Table(""); }

	private Table(String extractionMethod) {
		this.extractionMethod = extractionMethod;
	}

	public Table(ExtractionAlgorithm extractionAlgorithm) {
		this(extractionAlgorithm.toString());
	}

	/* visible for testing */
	final TreeMap<CellPosition, RectangularTextContainer> cells = new TreeMap<>();
	/** Public Accessors **/
	public int getRowCount() { return rowCount; }
	public int getColCount() { return colCount; }
	public int getPageNumber() { return pageNumber; }
	public void setPageNumber(int pageNumber) {	this.pageNumber = pageNumber; }

	public String getExtractionMethod() { return extractionMethod; }

	public void add(RectangularTextContainer chunk, int row, int col) {
		this.merge(chunk);

		rowCount = Math.max(rowCount, row + 1);
		colCount = Math.max(colCount, col + 1);

		CellPosition cp = new CellPosition(row, col);

		RectangularTextContainer old = cells.get(cp);
		if (old != null) chunk.merge(old);
		cells.put(cp, chunk);

		this.memoizedRows = null;
	}
	public List<List<RectangularTextContainer>> getRows() {
		if (this.memoizedRows == null) this.memoizedRows = computeRows();
		return this.memoizedRows;
	}

	public RectangularTextContainer getCell(int i, int j) {
		RectangularTextContainer cell = cells.get(new CellPosition(i,j)); // JAVA_8 use getOrDefault()
		return cell != null ? cell : TextChunk.EMPTY;
	}

	/***** Private Attributes ******/
	private final String extractionMethod;

	private int rowCount = 0;
	private int colCount = 0;
	private int pageNumber = 0;
	private List<RectangularTextContainer> columnHeaders = new ArrayList<>();

	/***** Private Methods ******/
	private List<List<RectangularTextContainer>> memoizedRows = null;


	private List<List<RectangularTextContainer>> computeRows() {
		List<List<RectangularTextContainer>> rows = new ArrayList<>();

		RectangularTextContainer lastCell = cells.get(new CellPosition(0, 0));
		System.out.println("lastCell is "+ lastCell);

		columnHeaders = getColumnHeaders();
		for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {

			List<RectangularTextContainer> lastRow = new ArrayList<>();
			rows.add(lastRow);
			for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
				RectangularTextContainer columnHeader = columnHeaders.get(columnIndex);

				RectangularTextContainer cell = getCell(rowIndex, columnIndex); // JAVA_8 use getOrDefault()

				/**** Detect column members here? Perhaps the cell can look for it's row? Look at the last
				 * cell and see if it's in the column still */
				if(cell == TextChunk.EMPTY){
					/*  check to see if the previous cell is in the current column.
					  * If so the current cell is continuing the last */
					if(cellInColumn(lastCell, columnHeader)){
						cell = lastCell;
					}
				}
				lastRow.add(cell);
				lastCell = cell;
			}

		}
		return rows;
	}

	/* We're assuming that the header/first row defines the columns so none are null */
	private List<RectangularTextContainer> getColumnHeaders(){
		List<RectangularTextContainer> headerRow = new ArrayList<>();

		for (int columnIndex = 0; columnIndex < colCount; columnIndex++) {
			headerRow.add(cells.get(new CellPosition(0, columnIndex)));
		}
		return headerRow;
	}

	private boolean cellInColumn(RectangularTextContainer cell, RectangularTextContainer columnHeader) {
		double columnMidline = columnHeader.getCenterX();
		return (columnMidline > cell.getLeft() && columnMidline < cell.getRight());
	}

}

class CellPosition implements Comparable<CellPosition> {

	CellPosition(int row, int col) {
		this.row = row;
		this.col = col;
	}

	final int row, col;

	@Override public int hashCode() {
		return row + 101 * col;
	}

	@Override public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		CellPosition other = (CellPosition) obj;
		return row == other.row && col == other.col;
	}

	@Override public int compareTo(CellPosition other) {
		int rowdiff = row - other.row;
		return rowdiff != 0 ? rowdiff : col - other.col;
	}

}
