package edu.lclark.orego.core;

/**
 * Coordinate system to convert between a short and other representations of a
 * location. There is no public constructor for this class; instead, use the
 * static method widthOf to get the appropriate instance.
 * <p>
 * A point is represented as a single short. This is an index into a
 * one-dimensional array representing the board, with a buffer of sentinel
 * points around the edges.
 * <p>
 * The standard idiom for accessing all points on the board is:
 * <pre>
 * for (short p : getAllPointsOnBoard()) {
 * 	// Do something with p
 * }
 * </pre>
 * The standard idiom for traversing all orthogonal neighbors of point p is:
 * <pre>
 * for (int i = FIRST_ORTHOGONAL_NEIGHBOR; i <= LAST_ORTHOGONAL_NEIGHBOR; i++) {
 * 	short n = getNeighbors(p)[i];
 * 	// Do something with n, which might be an off-board point
 * }
 * </pre>
 * To traverse diagonal neighbors, do the same, but with DIAGONAL substituted for ORTHOGONAL.
 * To traverse both, iterate from FIRST_ORTHOGONAL_NEIGHBOR to LAST_DIAGONAL_NEIGHBOR.
 * <p>
 * On those rare occasions where rows and columns are used, rows are always
 * zero-based from the top, columns from the left.
 */
public final class CoordinateSystem {

	/** Index into an array returned by getNeighbors. */
	public static final int NORTH_NEIGHBOR = 0;
	
	/** Index into an array returned by getNeighbors. */
	public static final int WEST_NEIGHBOR = 1;
	
	/** Index into an array returned by getNeighbors. */
	public static final int EAST_NEIGHBOR = 2;
	
	/** Index into an array returned by getNeighbors. */
	public static final int SOUTH_NEIGHBOR = 3;
	
	/** Index into an array returned by getNeighbors. */
	public static final int NORTHWEST_NEIGHBOR = 4;
	
	/** Index into an array returned by getNeighbors. */
	public static final int NORTHEAST_NEIGHBOR = 5;
	
	/** Index into an array returned by getNeighbors. */
	public static final int SOUTHWEST_NEIGHBOR = 6;
	
	/** Index into an array returned by getNeighbors. */
	public static final int SOUTHEAST_NEIGHBOR = 7;
	
	/** Index into an array returned by getNeighbors. */
	public static final int FIRST_ORTHOGONAL_NEIGHBOR = NORTH_NEIGHBOR;

	/** Index into an array returned by getNeighbors. */
	public static final int LAST_ORTHOGONAL_NEIGHBOR = SOUTH_NEIGHBOR;

	/** Index into an array returned by getNeighbors. */
	public static final int FIRST_DIAGONAL_NEIGHBOR = NORTHWEST_NEIGHBOR;

	/** Index into an array returned by getNeighbors. */
	public static final int LAST_DIAGONAL_NEIGHBOR = SOUTHEAST_NEIGHBOR;
	
	/** Added to a point to find the one to the east. */
	private static final short EAST = 1;

	/** Instances for various board widths. */
	private static final CoordinateSystem[] instances = new CoordinateSystem[20];

	/** Special value for no point. */
	public static final short NO_POINT = 0;

	/** Special value for passing. */
	public static final short PASS = 1;

	/** Special value for resigning. */
	public static final short RESIGN = 2;

	/** Returns the CoordinateSystem for the specified width. */
	public static CoordinateSystem forWidth(int width) {
		if (instances[width] == null) {
			instances[width] = new CoordinateSystem(width);
		}
		return instances[width];
	}
	
	/**
	 * @see #getAllPointsOnBoard()
	 */
	private final short[] allPointsOnBoard;

	/**
	 * @see #getNeighbors(int)
	 */
	private final short[][] neighbors;

	/** Added to a point to find the one to the south. */
	private final short south;
	
	/** Width of the board. */
	private final int width;

	/** Other classes should use forWidth to get an instance. */
	private CoordinateSystem(int width) {
		this.width = width;
		south = (short)(width + 1);
		int boardArea = width * width;
		int extendedBoardArea = (width + 1) * (width + 2) + 1;
		allPointsOnBoard = new short[boardArea];
		int i = 0;
		for (int r = 0; r < width; r++) {
			for (int c = 0; c < width; c++) {
				allPointsOnBoard[i] = at(r, c);
				i++;
			}
		}
		neighbors = new short[extendedBoardArea][];
		for (short p : allPointsOnBoard) {
			neighbors[p] = new short[] {(short)(p - south),
									(short)(p - EAST),
									(short)(p + EAST),
									(short)(p + south),
									(short)(p - south - EAST),
									(short)(p - south + EAST),
									(short)(p + south - EAST),
									(short)(p + south + EAST)};
		}
	}
	
	/** Returns the short representation of the point at row r, column c. */
	public short at(int r, int c) {
		assert isValidOneDimensionalCoordinate(r) : "Invalid row: " + r;
		assert isValidOneDimensionalCoordinate(c) : "Invalid column: " + c;
		return (short)((r + 1) * south + (c + 1) * EAST);
	}
	
	/**
	 * Returns the short representation of the point described by label, which
	 * might be something like "A5", "b3", or "PASS".
	 */
	public short at(String label) {
		label = label.toUpperCase();
		if (label.equals("PASS")) {
			return PASS;
		}
		if (label.equals("RESIGN")) {
			return RESIGN;
		}
		int r = Integer.parseInt(label.substring(1));
		r = width - r;
		int c;
		char letter = label.charAt(0);
		if (letter <= 'H') {
			c = letter - 'A';
		} else {
			c = letter - 'B';
		}
		return at(r, c);
	}

	/** Returns the column of point p. */
	public int column(short p) {
		return p % south - 1;
	}

	/** Returns a String representation of column c. */
	public String columnToString(int column) {
		return "" + "ABCDEFGHJKLMNOPQRST".charAt(column);
	}

	/** Returns an array of all the points on the board, for iterating through. */
	public short[] getAllPointsOnBoard() {
		return allPointsOnBoard;
	}

	/** Returns the index of the first point beyond the board. This is useful as the size of any array that must have an entry for any point on the board. */
	public short getFirstPointBeyondBoard() {
		return (short)(width * (south + EAST) + 1);
	}

	/**
	 * Returns an array of p's four orthogonal neighbors (indices 0-3) and four
	 * diagonal neighbors (4-7). If a point is at the edge (corner) of the
	 * board, one (two) of its neighbors are off-board points. The neighbors of
	 * an off-board point are not defined.
	 * <p>
	 * The neighbors are ordered like this:
	 * 
	 * <pre>
	 * 405
	 * 1 2
	 * 637
	 * </pre>
	 */
	public short[] getNeighbors(short p) {
		return neighbors[p];
	}

	/** Returns true if p is on the board. */
	public boolean isOnBoard(short p) {
		return isValidOneDimensionalCoordinate(row(p)) && isValidOneDimensionalCoordinate(column(p));
	}

	/** Returns true if p is on the third or fourth line. */
	public boolean isOnThirdOrFourthLine(short p) {
		int line = line(p);
		return ((line >= 3) && (line <= 4));
	}

	/** Returns true if c is a valid row or column index. */
	private boolean isValidOneDimensionalCoordinate(int c) {
		return (c >= 0) & (c < width);
	}

	/**
	 * Returns p's line (1-based) from the edge of the board
	 */
	private int line(short p) {
		int r = Math.min(row(p), width - row(p) - 1);
		int c = Math.min(column(p), width - column(p) - 1);
		return 1 + Math.min(r, c);		
	}

	/** Returns the Manhattan distance from p to q. */
	public int manhattanDistance(short p, short q) {
		int rowd = Math.abs(row(p) - row(q));
		int cold = Math.abs(column(p) - column(q));
		return rowd + cold;
	}

	/** Returns a String representation of point. */
	public String pointToString(short p) {
		if (p == PASS) {
			return "PASS";
		} else if (p == NO_POINT) {
			return "NO_POINT";
		} else if (p == RESIGN) {
			return "RESIGN";
		} else {
			return columnToString(column(p)) + rowToString(row(p));
		}
	}

	/** Returns the row of point p. */
	public int row(short p) {
		return p / south - 1;
	}

	/** Returns a String representation of row r. */
	public String rowToString(int row) {
		return "" + (width - row);
	}

}
