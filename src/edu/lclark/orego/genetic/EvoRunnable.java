package edu.lclark.orego.genetic;

import static edu.lclark.orego.core.CoordinateSystem.PASS;
import static edu.lclark.orego.core.CoordinateSystem.NO_POINT;
import static edu.lclark.orego.core.NonStoneColor.VACANT;
import static edu.lclark.orego.core.StoneColor.BLACK;
import static edu.lclark.orego.core.StoneColor.WHITE;
import static edu.lclark.orego.experiment.Logging.log;

import java.util.Comparator;

import edu.lclark.orego.core.Board;
import edu.lclark.orego.core.Color;
import edu.lclark.orego.core.CoordinateSystem;
import edu.lclark.orego.core.StoneColor;
import edu.lclark.orego.feature.Conjunction;
import edu.lclark.orego.feature.HistoryObserver;
import edu.lclark.orego.feature.Predicate;
import edu.lclark.orego.feature.StoneCountObserver;
import edu.lclark.orego.mcts.CopiableStructure;
import edu.lclark.orego.move.Mover;
import edu.lclark.orego.score.ChinesePlayoutScorer;
import edu.lclark.orego.thirdparty.MersenneTwisterFast;
import edu.lclark.orego.util.ShortSet;

/**
 * Players use this class to perform multiple Monte Carlo coevolution in
 * different threads.
 */
public class EvoRunnable implements Runnable {

	private final Board board;

	private final CoordinateSystem coords;

	private Mover fallbackMover;

	private final Predicate filter;

	private final HistoryObserver history;

	/** Indices of the tournament losers. */
	private int[] loserIndices;

	private final StoneCountObserver mercyObserver;

	/** phenotypes[c][i] is phenotype number i of color c for the current tournament. */
	private Phenotype[][] phenotypes;

	private final Player player;
	
	private Population[] populations;

	private final MersenneTwisterFast random;

	private final ChinesePlayoutScorer scorer;

	public EvoRunnable(Player player, CopiableStructure stuff) {
		random = new MersenneTwisterFast();
		final CopiableStructure copy = stuff.copy();
		this.board = copy.get(Board.class);
		coords = board.getCoordinateSystem();
		history = copy.get(HistoryObserver.class);
		filter = copy.get(Conjunction.class);
		fallbackMover = copy.get(Mover.class);
		scorer = copy.get(ChinesePlayoutScorer.class);
		mercyObserver = copy.get(StoneCountObserver.class);
		loserIndices = new int[2];
		this.player = player;
	}

	public short bestMove(Phenotype phenotype) {
		return bestMove(phenotype, history.get(board.getTurn() - 2),
				history.get(board.getTurn() - 1));
	}

	public short bestMove(Phenotype phenotype, short penultimate, short ultimate) {
		short reply = phenotype.replyToTwoMoves(penultimate, ultimate);
		if (isValidMove(reply)) {
			return reply;
		}
//		reply = phenotype.replyToOneMove(ultimate);
//		if (isValidMove(reply)) {
//			return reply;
//		}
//		reply = phenotype.followUp(penultimate);
//		if (isValidMove(reply)) {
//			return reply;
//		}
//		reply = phenotype.playBigPoint();
//		if (isValidMove(reply)) {
//			return reply;
//		}
		return NO_POINT;
	}

	public short vote(StoneColor c) {
		board.copyDataFrom(player.getBoard());
		final int[] votes = getVotes(c, history.get(board.getTurn() - 2), history.get(board.getTurn() - 1));
		short best = PASS;
		ShortSet vacantPoints = board.getVacantPoints();
		for (int i = 0; i < vacantPoints.size(); i++) {
			short p = vacantPoints.get(i);
			if (votes[p] > votes[best]){
				best = p;
			}
		}
		return best;
	}
	
	/**
	 * Returns the number of votes for each point on the board, given the last
	 * two moves.
	 */
	public int[] getVotes(StoneColor c, short penultimate, short ultimate) {
		// TODO Let's not make this array every time
		int[] result = new int[coords.getFirstPointBeyondBoard()];
		for (Genotype g : populations[c.index()].getIndividuals()) {
			phenotypes[0][0].installGenes(-1, g);
			result[bestMove(phenotypes[0][0], penultimate, ultimate)]++;
		}
		for (short p : coords.getAllPointsOnBoard()) {
			if (result[p] > 0) {
				System.out.println(coords.toString(p) + ": " + result[p]);
			}
		}
		return result;
	}

	public Board getBoard() {
		return board;
	}

	Phenotype getPhenotype(StoneColor color, int index) {
		return phenotypes[color.index()][index];
	}

	/** Returns the number of playouts completed by this runnable. */
	public long getPlayoutsCompleted() {
		return playoutsCompleted;
	}

	/** Returns true if p is move not excluded by a priori criteria. */
	boolean isValidMove(short p) {
		return coords.isOnBoard(p) && (board.getColorAt(p) == VACANT)
				&& filter.at(p) && board.isLegal(p);
	}

	/**
	 * Chooses two random members of the population and has them play a game
	 * against each other.
	 *
	 * @param mercy
	 *            True if we should abandon the playout when one color has many
	 *            more stones than the other.
	 * @return The winning color, although this is only used in tests.
	 */
	public Color performPlayout(boolean mercy) {
		Phenotype black = phenotypes[BLACK.index()][0];
		black.installGenes(-1, populations[BLACK.index()].randomGenotype(random));
		Phenotype white = phenotypes[WHITE.index()][0];
		white.installGenes(-1, populations[WHITE.index()].randomGenotype(random));
		return playAgainst(black, white, mercy);
	}

	/**
	 * Plays a game against that. Assumes that this Phenotype is black, that is
	 * white.
	 * 
	 * @param mercy
	 *            True if we should abandon the playout when one color has many
	 *            more stones than the other.
	 * @return The color of the winner, or VACANT if the game had no winner.
	 */
	public Color playAgainst(Phenotype black, Phenotype white, boolean mercy) {
		playoutsCompleted++;
		board.copyDataFrom(player.getBoard());
		do {
			if (board.getTurn() >= coords.getMaxMovesPerGame()) {
				// Playout ran out of moves, probably due to superko
				return VACANT;
			}
			if (board.getPasses() < 2) {
				if (board.getColorToPlay() == BLACK) {
					selectAndPlayOneMove(black, true);
				} else {
					selectAndPlayOneMove(white, true);
				}
			}
			if (board.getPasses() >= 2) {
				// Game ended
				return scorer.winner();
			}
			if (mercy) {
				final Color mercyWinner = mercyObserver.mercyWinner();
				if (mercyWinner != null) {
					// One player has far more stones on the board
					return mercyWinner;
				}
			}
		} while (true);
	}

	/** Number of contestants of each color in a tournament. */
	private int contestants;
			
	/**
	 * Plays a tournament among two pairs of individuals -- one from each
	 * population. The indices of the individuals with the fewest wins are
	 * stored in loserIndices.
	 */
	public void playTournament(boolean mercy) {
		// Choose contestants and install their genes in phenotypes
		for (int color = 0; color < 2; color++) {
			for (int i = 0; i < contestants; i++) {
				int j;
				do {
					j = random.nextInt(populations[BLACK.index()].size());
				} while (!populations[color].getIndividuals()[j].getLock()
						.tryLock());
				phenotypes[color][i].installGenes(j,
						populations[color].getIndividuals()[j]);
			}
		}
		// Run the tournament
		for (int b = 0; b < contestants; b++) {
			Phenotype black = getPhenotype(BLACK, b);
			for (int w = 0; w < contestants; w++) {
				Phenotype white = getPhenotype(WHITE, w);
				Color winner = playAgainst(black, white, mercy);
				winCounts[winner.index()]++;
				if (winner == WHITE) {
					white.setWinCount(getPhenotype(WHITE, w).getWinCount() + 1);
				} else if (winner == BLACK) {
					black.setWinCount(getPhenotype(BLACK, b).getWinCount() + 1);
				}
			}
		}
		// Determine losers
		java.util.Arrays.sort(phenotypes[BLACK.index()]);
		java.util.Arrays.sort(phenotypes[WHITE.index()]);
		// Replace the losers
		for (int c = 0; c < 2; c++) {
			populations[c].replaceLoser(phenotypes[c][0].getPopulationIndex(),
					phenotypes[c][phenotypes[c].length - 1]
							.getPopulationIndex(),
					phenotypes[c][phenotypes[c].length - 2]
							.getPopulationIndex(), random);
		}
		// Release locks on contestants
		for (int color = 0; color < 2; color++) {
			for (int i = 0; i < contestants; i++) {
				populations[color].getIndividuals()[phenotypes[color][i].getPopulationIndex()].getLock().unlock();
			}
		}
	}

	private long playoutsCompleted;
	
	private int[] winCounts;
	
	@Override
	public void run() {
		playoutsCompleted = 0;
		winCounts = new int[2];
		while (player.shouldKeepRunning()) {
			playTournament(true); // Should that mercy value be read from somewhere?
		}
		log("Playouts completed: " + playoutsCompleted);
//		System.out.println("Playouts completed: " + playoutsCompleted);
//		System.out.println("Black wins " + winCounts[0] + " games, white " + winCounts[1]);
		player.notifyMcRunnableDone();
	}

	public short selectAndPlayOneMove(Phenotype phenotype, boolean fast) {
		short p = bestMove(phenotype);
		if (p != NO_POINT) {
			board.play(p);
		} else {
			return fallbackMover.selectAndPlayOneMove(random, fast);
		}
		return p;
	}

	public void setContestants(int contestants) {
		this.contestants = contestants;
		phenotypes = new Phenotype[2][contestants];
		for (int color = 0; color < phenotypes.length; color++) {
			for (int i = 0; i < contestants; i++) {
				phenotypes[color][i] = new Phenotype(coords);
			}
		}
	}

	public void setPopulations(Population[] populations) {
		this.populations = populations;
	}

}
