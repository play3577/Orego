package edu.lclark.orego.genetic;

import static edu.lclark.orego.genetic.Pattern.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.lclark.orego.core.Board;
import edu.lclark.orego.core.CoordinateSystem;
import edu.lclark.orego.feature.HistoryObserver;

public class PatternTest {

	private Board board;
	
	private Pattern pattern;
	
	private CoordinateSystem coords;
	
	@Before
	public void setUp() {
		board = new Board(9);
		coords = board.getCoordinateSystem();
		pattern = new Pattern(board, new HistoryObserver(board));
	}

	private short at(String label) {
		return coords.at(label);
	}
	
	@Test
	public void testEdges(){
		board.play("d2");
		board.play("b6");
		board.play("e5");
		board.play("a2");
		board.play("b1");
		board.play("c1");
		int [] rules = Pattern.makeRule(coords.getWidth(), 1, 
				".....",
				"..O..",
				"...#O",
				".....",
				".....");
		int friendly = rules[0] | YES;
		int enemy = rules[1];
		assertEquals(at("a1"), pattern.patternMatcher(friendly, enemy));
		assertEquals(0, pattern.patternMatcher(friendly, enemy));
//		System.out.println(board.toString());
	}

	@Test
	public void testEdges2(){
		board.play("d9");
		board.play("b9");
//		System.out.println(board);
		int [] rules = Pattern.makeRule(1, 3, 
				".....",
				".....",
				".O.#.",
				".....",
				".....");
		int friendly = rules[0] | YES;
		int enemy = rules[1];
		assertEquals(at("c9"), pattern.patternMatcher(friendly, enemy));
	}

	
	@Test
	public void testPatternMatcherSpace() {
		board.play("c5");
		board.play("d2");
		board.play("b2");
		board.play("e5");
		board.play("a3");
		board.play("b5");		
//		System.out.println(board.toString());
		int [] rules = Pattern.makeRule(0, 0, 
				".....",
				"O#.O.",
				".....",
				".....",
				"#.O..");
		int friendly = rules[0] | YES;
		int enemy = rules[1];
//		int friendly = 0b010000000000010000000000001000000;
//		int enemy =    0b000000000001000000000000100100000;
//		System.out.println(Integer.toBinaryString(friendly) + "\n" + Integer.toBinaryString(enemy));
		assertEquals(at("d4"), pattern.patternMatcher(friendly, enemy));
		assertEquals((short) 0, pattern.patternMatcher(friendly, enemy));
		assertNotEquals(at("d4"), pattern.patternMatcher(friendly, enemy));
	}

	@Test
	public void testPatternMatcherTime() {
		board.play("c5");
		board.play("d2");
		board.play("b2");
//		System.out.println(board.toString());
		int ultimate = 	0b11000000110000000000000001010010;
		assertEquals((short) 48, pattern.patternMatcher(ultimate, 0));
		assertNotEquals((short) 48, pattern.patternMatcher(ultimate, 0));
		assertEquals((short) 0, pattern.patternMatcher(ultimate));
		assertNotEquals((short) 65, pattern.patternMatcher(ultimate));
	}

	@Test
	public void testSelectAndPlayMove() {
		board.play("c5");
		board.play("d2");
		board.play("b2");
//		System.out.println(board);
		int p1 = 0b11100000110000001010100001010010;
		assertEquals(pattern.selectAndPlayMove(p1, 0), (short) 48);
		assertNotEquals(pattern.selectAndPlayMove(p1, 0), (short) 48);
	}

}
