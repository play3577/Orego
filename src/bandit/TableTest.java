package bandit;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class TableTest {

	private Table table;
	
	@Before
	public void setUp() throws Exception {
		table = new Table();
	}

	@Test
	public void testStore() {
		assertEquals(0, table.getRunCount(34));
		assertEquals(0.5, table.getWinRate(34), 0.001);
		table.store(34, true);
		assertEquals(1, table.getRunCount(34));
		assertEquals(1.0, table.getWinRate(34), 0.001);
		table.store(34, true);
		assertEquals(2, table.getRunCount(34));
		assertEquals(1.0, table.getWinRate(34), 0.001);
		table.store(34, false);
		assertEquals(3, table.getRunCount(34));
		assertEquals(2.0 / 3.0, table.getWinRate(34), 0.001);
	}

}
