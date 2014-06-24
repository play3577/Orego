package edu.lclark.orego.experiment;

import static edu.lclark.orego.experiment.ExperimentConfiguration.EXPERIMENT;
import static edu.lclark.orego.experiment.SystemConfiguration.SYSTEM;

/** Plays a series of experimental games on one machine. */
public final class GameBatch implements Runnable {

	/**
	 * @param args
	 *            element 0 is the host name.
	 */
	public static void main(String[] args) {
		assert args.length == 1;
		System.out.println(java.util.Arrays.toString(args));
		try {
			for (int i = 0; i < EXPERIMENT.gamesPerHost; i++) {
				new Thread(new GameBatch(i, args[0])).start();
			}
		} catch (Throwable e) {
			e.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	/** Number of the batch (used as part of the filename). */
	private final int batchNumber;

	/**
	 * First part (before the first period) of the hostname (used as part of the
	 * filename).
	 */
	private String host;

	public GameBatch(int batchNumber, String hostname) {
		System.out.println("Creating game batch " + batchNumber + " on " + hostname);
		this.batchNumber = batchNumber;
		this.host = hostname.substring(0, hostname.indexOf('.'));
	}

	@Override
	public void run() {
		System.out.println("Running " + batchNumber);
		System.out.println("Conditions: " + EXPERIMENT.conditions.size());
		for (String condition : EXPERIMENT.conditions) {
			System.out.println("Running some games");
			String orego = SYSTEM.javaWithOregoClasspath + " -ea -Xmx1024M edu.lclark.orego.ui.Orego " + condition;
			runGames(orego, EXPERIMENT.gnugo);
			runGames(EXPERIMENT.gnugo, orego);
		}
		System.out.println("Done running " + batchNumber);
	}

	
	/** Runs several games with the specified black and white players. */
	public void runGames(String black, String white) {
		int[] wins = new int[3];
		System.out.println("Running games: " + EXPERIMENT.gamesPerColor);
		for (int i = 0; i < EXPERIMENT.gamesPerColor; i++) {
			String outFile = SYSTEM.resultsDirectory + host + "-b"
			+ batchNumber + "-" + System.currentTimeMillis() + ".sgf";
			System.out.println("Creating game " + outFile);
			Game game = new Game(outFile, EXPERIMENT.rules, black, white);				
			System.out.println("Starting game " + outFile);
			wins[game.play().index()]++;
			System.out.println("Finished game " + outFile);
		}
	}

}
