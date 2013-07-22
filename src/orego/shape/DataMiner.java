package orego.shape;

import static orego.core.Colors.BLACK;
import static orego.core.Colors.NUMBER_OF_PLAYER_COLORS;
import static orego.core.Colors.VACANT;
import static orego.core.Colors.WHITE;
import static orego.core.Colors.charToColor;
import static orego.core.Coordinates.getBoardWidth;
import static orego.core.Coordinates.isOnBoard;
import static orego.core.Coordinates.reflect;
import static orego.core.Coordinates.rotate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import orego.core.Board;
import orego.core.Colors;
import orego.sgf.SgfParser;
import orego.util.IntSet;
import ec.util.MersenneTwisterFast;

/**
 * Extracts patterns from SGF files.
 */
public class DataMiner {

	/** Hash table containing the actual win rates for each pattern. */
	private HashMap<DensePattern, Float>[][] winRateMap;
	
	/** Hash table containing the actual count for each pattern. */
	private HashMap<DensePattern, Long>[][] countMap;
	
	private Cluster patterns;
	
	private MersenneTwisterFast random;
	
	protected static final int MAX_PATTERN_RADIUS = 1, MIN_PATTERN_RADIUS = 1;

	public static void main(String[] args) {
		new DataMiner().run(PatternExtractor.TEST_GAMES_DIRECTORY,"SgfFiles");
	}

	@SuppressWarnings("unchecked")
	public DataMiner() {
		winRateMap = new HashMap[MAX_PATTERN_RADIUS+1][NUMBER_OF_PLAYER_COLORS];
		countMap = new HashMap[MAX_PATTERN_RADIUS+1][NUMBER_OF_PLAYER_COLORS];
		for (int radius = MIN_PATTERN_RADIUS; radius <= MAX_PATTERN_RADIUS; radius ++){
			for (int color = 0; color < NUMBER_OF_PLAYER_COLORS; color++){
				winRateMap[radius][color]= new HashMap<DensePattern,Float>();
				countMap[radius][color]= new HashMap<DensePattern,Long>();
			}
		}
	}

	/**
	 * Extracts patterns from all files in a directory.
	 * 
	 * @param in Full path to directory containing SGF files.
	 * @param out Directory (within OREGO_ROOT_DIRECTORY) to store output and load Cluster from, usually "SgfFiles" or "SgfTestFiles".
	 */
	public void run(String in, String out2) {
		String out = orego.experiment.Debug.OREGO_ROOT_DIRECTORY + out2
				+ File.separator + getBoardWidth() + File.separator;
		try {
			random = new MersenneTwisterFast(0L);
			setUp(in);
			
			try {
				ObjectOutputStream ow = new ObjectOutputStream(
						new FileOutputStream(new File(out + "actual-win-rate.data")));
				ow.writeObject(winRateMap);
				ow.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			try {
				ObjectOutputStream ow = new ObjectOutputStream(
						new FileOutputStream(new File(out + "actual-count.data")));
				ow.writeObject(countMap);
				ow.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
			(new DataToCSV()).run(in, out2);
			
//			loadCluster(out);
//			
//			for(int radius = MIN_PATTERN_RADIUS; radius<=MAX_PATTERN_RADIUS; radius++){
//				PrintWriter bw = new PrintWriter(new FileWriter(new File(
//						out+"results_for_radius"+radius+".csv")));
//				StringBuilder output = new StringBuilder("");
//				for(DensePattern key : winRateMap[radius][BLACK].keySet()){
//					String key2 = key.toString();
//					long hash = keyToHash(key2,radius);
//					output.append(key2);
//					output.append(',');
//					output.append(hash);
//					output.append(',');
//					output.append(winRateMap[radius][BLACK].get(key));
//					output.append(',');
//					output.append(countMap[radius][BLACK].get(key));
//					output.append(',');
//					output.append(patterns.getPatternWinRate(hash, BLACK, radius));
//					output.append(',');
//					output.append(patterns.getPatternCount(hash, BLACK, radius));
//					output.append("\n");
//				}
//				bw.println(output.toString());
//				bw.close();
//			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Takes a directory of SGF files and walks through them, counting how often
	 * patterns are seen and played.
	 */
	public void setUp(String directory) {
		try {
			File dir = new File(directory);
			System.out.println("-->Directory: " + dir.getAbsolutePath());
			String[] dirList = dir.list();
			for (int i = 0; i < dirList.length; i++) {
				String filename = directory + File.separator + dirList[i];
				File file = new File(filename);
				if (file.isDirectory()) {
					setUp(filename);
				} else if (dirList[i].toLowerCase().endsWith(".sgf")) {
					checkForPatterns(file);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Check for the patterns in a particular file.
	 */
	public void checkForPatterns(File file) {
		Board board = SgfParser.sgfToBoard(file);
		if (board.getInitialBlackStones().size()!=0||board.getInitialWhiteStones().size()!=0){
			//handicap game, so ignore it
			return;
		}
		int turn = board.getTurn();
		int currentTurn = 0;
		Board[][][] patternBoard = new Board[4][2][2];
		for (int rotation = 0; rotation < 4; rotation++) {
			for (int reflection = 0; reflection < 2; reflection++) {
				for (int color = 0; color < 2; color++) {
					patternBoard[rotation][reflection][color] = new Board();
				}
				patternBoard[rotation][reflection][WHITE].setColorToPlay(WHITE);
			}
		}
		while (currentTurn < turn) {
			int goodMove = board.getMove(currentTurn);
			if (isOnBoard(goodMove)) {
				// Choose a random move to store as bad
				IntSet possibleMoves = patternBoard[0][0][0].getVacantPoints();
				int badMove;
				do {
					badMove = possibleMoves.get(random.nextInt(possibleMoves
							.size()));
				} while (!patternBoard[0][0][0].isLegal(badMove)
						|| badMove == goodMove);
				// Store in all 16 rotations, reflections, and color inversions
				for (int rotation = 0; rotation < 4; rotation++) {
					for (int reflection = 0; reflection < 2; reflection++) {
						for (int color = 0; color < 2; color++) {
							for(int radius = MIN_PATTERN_RADIUS; radius<= MAX_PATTERN_RADIUS; radius++){
								DensePattern key = new DensePattern(patternBoard[rotation][reflection][color].patternToArray(radius, goodMove));
								if(winRateMap[radius][color].containsKey(key)){
									float tempWinRate = winRateMap[radius][color].get(key);
									long tempCount = countMap[radius][color].get(key);
									winRateMap[radius][color].put(key, ((tempWinRate * tempCount) + 1) / (tempCount + 1));
									countMap[radius][color].put(key, tempCount+1);
								} else{
									winRateMap[radius][color].put(key, 1.0f);
									countMap[radius][color].put(key, 1L);
								}
								key = new DensePattern(patternBoard[rotation][reflection][color].patternToArray(radius, badMove));
								if(winRateMap[radius][color].containsKey(key)){
									float tempWinRate = winRateMap[radius][color].get(key);
									long tempCount = countMap[radius][color].get(key);
									winRateMap[radius][color].put(key, ((tempWinRate * tempCount) + 0) / (tempCount + 1));
									countMap[radius][color].put(key, tempCount+1);
								} else{
									winRateMap[radius][color].put(key, 0.0f);
									countMap[radius][color].put(key, 1L);
								}
							}
						}
						goodMove = reflect(goodMove);
						badMove = reflect(badMove);
					}
					goodMove = rotate(goodMove);
					badMove = rotate(badMove);
				}
			}
			// Play the move
			for (int rotation = 0; rotation < 4; rotation++) {
				for (int reflection = 0; reflection < 2; reflection++) {
					for (int color = 0; color < 2; color++) {
						patternBoard[rotation][reflection][color]
								.play(goodMove);
					}
					if (isOnBoard(goodMove))
						goodMove = reflect(goodMove);
				}
				if (isOnBoard(goodMove))
					goodMove = rotate(goodMove);
			}
			currentTurn++;
		}
	}
}
