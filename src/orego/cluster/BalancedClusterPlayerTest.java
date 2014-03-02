package orego.cluster;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static orego.core.Coordinates.*;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

import orego.cluster.RMIStartup.RegistryFactory;
import orego.core.Coordinates;
import orego.util.IntSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class BalancedClusterPlayerTest {

	private BalancedClusterPlayer player;
	
	@Mock private RegistryFactory mockFactory; 
	
	@Mock private Registry mockRegistry;
	
	@Mock private TreeSearcher searcherA;
	
	@Mock private TreeSearcher searcherB;
	
	@Before
	public void setUp() throws Exception {
		
		when(mockFactory.getRegistry()).thenReturn(mockRegistry);
		
		ClusterPlayer.factory = mockFactory;
		
		player = new BalancedClusterPlayer();
		
		player.reset();
		
	}

	@Test
	public void testSoloSearcherGetsAllPoints() throws RemoteException {
		player.addSearcher(searcherA);
		
		IntSet allPoints = new IntSet(getFirstPointBeyondBoard());
		
		for(int idx = 0; idx < getFirstPointBeyondBoard(); idx++) allPoints.add(idx);
		
		verify(searcherA).setPointsToConsider(allPoints);
	}
	
	@Test
	public void testPointsDistributed() throws RemoteException {
		
		// The searchers need ids for points to be distributed 
		when(searcherA.getSearcherId()).thenReturn(0);
		when(searcherB.getSearcherId()).thenReturn(1);
		
		player.addSearcher(searcherA);		
		player.addSearcher(searcherB);
		
		IntSet firstHalf = new IntSet(getFirstPointBeyondBoard());
		
		for(int idx = 0; idx < getFirstPointBeyondBoard(); idx++) {
			if(idx % 2 == 0) firstHalf.add(idx);
		}
		
		verify(searcherA).setPointsToConsider(firstHalf);
		
	}

}