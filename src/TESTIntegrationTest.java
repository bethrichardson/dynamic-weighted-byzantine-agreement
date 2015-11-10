import org.junit.*;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TESTIntegrationTest {
    Client client;
    List<InetSocketAddress> serverList;
    List<InetSocketAddress> coordinatorList;
    InetSocketAddress coordinatorAddress;
    Coordinator coordinator;
    int numNodes = 5;

    @Before
    public void setUp() throws Exception {
        coordinatorList = Utils.createServerList(9000, 1);
        serverList = Utils.createServerList(9001, numNodes);
        client = new Client(coordinatorList);
        coordinatorAddress = coordinatorList.get(0);
        coordinator = Utils.setupNewTestServer(numNodes);
    }

    @After
    public void tearDown() throws Exception {
        coordinator.shutDown();

    }

    @Test
    public void testValidLookup() throws Exception {
        String name = "10.0.0.1/8";
        String expectedResponse = client.responses.get("TRUE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testInvalidLookup() throws Exception {
        String name = "bananas.com";
        String expectedResponse = client.responses.get("FALSE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testCoordinatorCanSwitchFaultyNode() throws Exception {
        coordinator.setNodeFaulty(0, true);
        assertTrue(coordinator.nodeObjectList.get(0).actFaulty);
    }

    @Test
    public void testFaultyNodeGivesInconsistentAnswers() throws Exception {
        String name = "banana";
        coordinator.setNodeFaulty(0, true);

        Value expectedResponse = Value.TRUE;
        validateNetwork(name, client);
        Value firstResponse = coordinator.nodeObjectList.get(0).lastReply;
        validateNetwork(name, client);
        Value secondResponse = coordinator.nodeObjectList.get(0).lastReply;

        assertEquals(expectedResponse, firstResponse);
        assertNotEquals(firstResponse, secondResponse);
    }

    @Test
    public void testCoordinatorCanResetAllFaultyNodes() throws Exception {
        coordinator.setNodeFaulty(0, true);
        coordinator.resetFaultyNodes();
        for(int i = 0; i < numNodes; i ++) {
            assertFalse(coordinator.nodeObjectList.get(i).actFaulty);
        }
    }

    @Test
    public void testCoordinatorCanCreateFaultyNodeSet() throws Exception {
        int expectedNumFaultyNodes = 2;
        ArrayList<Integer> faultyNodes = new ArrayList<>(expectedNumFaultyNodes);

        coordinator.createFaultyNodes(expectedNumFaultyNodes);
        for(int i = 0; i < numNodes; i ++) {
            if (coordinator.nodeObjectList.get(i).actFaulty)
                faultyNodes.add(i);
        }
        assertEquals(expectedNumFaultyNodes, faultyNodes.size());
    }

    @Test
    public void testCoordinatorCanSwitchAlgorithm() throws Exception {
        coordinator.setAlgorithm(false);
        assertFalse(coordinator.nodeObjectList.get(0).queenAlgorithm);
        coordinator.setAlgorithm(true);
        assertTrue(coordinator.nodeObjectList.get(0).queenAlgorithm);
    }

    @Test
    public void testCoordinatorSetsInitialNormalizedWeightsOnNodes() throws Exception {
        ArrayList<Double> weights = coordinator.createInitialWeights();
        double normalizedWeight = (1.0 / numNodes);
        for(int i = 0; i < numNodes; i ++) {
            assertEquals(weights.get(i), normalizedWeight, 0.001);
        }
        for(int i = 0; i < numNodes; i ++) {
            assertEquals(weights.get(i), coordinator.nodeObjectList.get(i).weights.get(i), 0.001);
        }
    }
    
    @Test
    public void testAnchorWithAlphaGreaterThanOne() throws Exception {
    	ArrayList<Double> weights = coordinator.nodeObjectList.get(0).weights;
		weights.set(0, 0.1);
		weights.set(1, 0.2);
		weights.set(2, 0.3);
		weights.set(3, 0.2);
		weights.set(4, 0.2);
		
		int anchor = coordinator.nodeObjectList.get(0).queen.calculateAnchor();
		
		assertEquals(2, anchor);
    }

    public String validateNetwork(String name, Client myClient){
        return myClient.getResponse(myClient.validateNetwork(name));
    }

}
