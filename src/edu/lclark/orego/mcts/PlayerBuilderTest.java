package edu.lclark.orego.mcts;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class PlayerBuilderTest {

	private PlayerBuilder builder;
	
	@Before
	public void setUp() throws Exception {
		builder = new PlayerBuilder();
	}

	@Test
	public void testBoardSize() {
		builder.boardWidth(9);
		assertEquals(9, builder.build().getBoard().getCoordinateSystem().getWidth());
		builder.boardWidth(19);
		assertEquals(19, builder.build().getBoard().getCoordinateSystem().getWidth());
	}

}
