package orego.heuristic;

import java.io.*;
import java.util.ArrayList;

import orego.patternanalyze.*;
import static orego.experiment.Debug.*;

import orego.core.Board;

public class DynamicPatternHeuristic extends Heuristic{

	private ArrayList<DynamicPattern> patternList;
	
	private static boolean test;
	
	public DynamicPatternHeuristic(int weight) {
		super(weight);
		patternList = new ArrayList<DynamicPattern>();
		if (test){
			extractPatternsFromFile(OREGO_ROOT_DIRECTORY+File.separator+"testFiles/testPattern8.dat");			
		}
		else {
			extractPatternsFromFile(OREGO_ROOT_DIRECTORY+File.separator+"testFiles/pattern8.dat");
		}
	}

	private void extractPatternsFromFile(String fileName) {

		ObjectInputStream input;
		try {
			input = new ObjectInputStream(new FileInputStream(
					new File(fileName)));
			DynamicPattern pattern = null;
			try {
				int counter = 0;
				while ((pattern = (DynamicPattern) input.readObject()) != null && counter < 100) {
					patternList.add(pattern);
					counter++;
				}
				input.close();
			} catch (EOFException ex) {
				input.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
	}

	@Override
	public void prepare(Board board) {
		int p = board.getMove(board.getTurn() - 1);
		long pattern1 = DynamicPattern.setupPattern(p, board, 8);
		long pattern2 = DynamicPattern.setupPattern(p, board, 12);
		long pattern3 = DynamicPattern.setupPattern(p, board, 20);
		long pattern4 = DynamicPattern.setupPattern(p, board, 24);
		for (DynamicPattern pattern : patternList) {
			if (pattern.match(pattern4, 24)) {
				return 4 * getWeight();
			}
			if (pattern.match(pattern3, 20)) {
				return 3 * getWeight();
			}
			if (pattern.match(pattern2, 12)) {
				return 2 * getWeight();
			}
			if (pattern.match(pattern1, 8)) {
				return 1 * getWeight();
			}
		}
		return 0;
	}

	public static void setTestMode(boolean value) {
		test = value;
	}
}
