import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TESTIntegrationTest {
    Client client;
    List<InetSocketAddress> serverList;
    List<InetSocketAddress> coordinatorList;
    ArrayList<ServerThread> nodeList;
    InetSocketAddress coordinatorAddress;
    Coordinator coordinator;
    int numNodes = 6;

    @Before
    public void setUp() throws Exception {
        coordinatorList = Utils.createServerList(9000, 1);
        serverList = Utils.createServerList(9001, numNodes);
        client = new Client(coordinatorList);
        coordinatorAddress = coordinatorList.get(0);
        coordinator = Utils.setupNewTestServer(numNodes);
        nodeList = Utils.setupNewTestNodePool(numNodes, coordinatorAddress);
    }

    @After
    public void tearDown() throws Exception {
        coordinator.shutDown();
        for (ServerThread thread : nodeList) {
            thread.node.shutDown();
        }
    }

    @Test
    public void testValidLookupQueen() throws Exception {
        String name = "10.0.0.1/8";
        String expectedResponse = client.responses.get("TRUE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testInvalidLookupQueen() throws Exception {
        String name = "bananas.com";
        String expectedResponse = client.responses.get("FALSE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testValidLookupKing() throws Exception {
        for(int i = 0; i < numNodes; i ++) {
            nodeList.get(i).node.queenAlgorithm = false;
        }
        String name = "10.0.0.1/8";
        String expectedResponse = client.responses.get("TRUE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testInvalidLookupKing() throws Exception {
        for(int i = 0; i < numNodes; i ++) {
            nodeList.get(i).node.queenAlgorithm = false;
        }
        String name = "bananas.com";
        String expectedResponse = client.responses.get("FALSE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public synchronized void testCoordinatorCanSwitchFaultyNode() throws Exception {
        coordinator.setNodeFaulty(0, true);

        int delay = 100; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to node.");
        assertTrue(nodeList.get(0).node.actFaulty);

    }

    @Test
    public void testFaultyNodeAlwaysGivesUndecidedAnswers() throws Exception {
        String name = "banana";
        coordinator.setNodeFaulty(0, true);

        Value expectedResponse = Value.UNDECIDED;
        validateNetwork(name, client);
        Value response = nodeList.get(0).node.lastReply;

        assertEquals(expectedResponse, response);
    }

    @Ignore("Does not work correctly")
    @Test
    public void testFaultyNodeIsDetected() throws Exception {
        String name = "banana";
        coordinator.setNodeFaulty(0, true);

        int delay = 1000; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        validateNetwork(name, client);
        Utils.timedWait(5000, "TEST: Wait on message to nodes.");

        for (int i = 0; i < numNodes; i++) {
            assertEquals(Value.TRUE, nodeList.get(i).node.algorithm.faultySet[0]);
        }
    }

    @Ignore("Depends on testFaultyNodeIsDetected working correctly")
    @Test
    public void testUpdateWeights() throws Exception {
        String name = "banana";
        coordinator.setNodeFaulty(0, true);

        int delay = 1000; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        validateNetwork(name, client);
        Utils.timedWait(5000, "TEST: Wait on message to nodes.");

        for (int i = 0; i < numNodes; i++) {
            assertEquals(new Double(0.0), nodeList.get(i).node.algorithm.weights.get(0));
        }
    }

    @Test
    public void testCoordinatorCanResetAllFaultyNodes() throws Exception {
        coordinator.setNodeFaulty(0, true);
        coordinator.resetFaultyNodes();
        int delay = 10; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");
        for(int i = 0; i < numNodes; i ++) {
            assertFalse(nodeList.get(i).node.actFaulty);
        }
    }

    @Test
    public void testCoordinatorCanCreateFaultyNodeSet() throws Exception {
        int expectedNumFaultyNodes = 2;
        ArrayList<Integer> faultyNodes = new ArrayList<>(expectedNumFaultyNodes);

        coordinator.createFaultyNodes(expectedNumFaultyNodes);

        int delay = 20; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        for(int i = 0; i < numNodes; i ++) {
            if (nodeList.get(i).node.actFaulty)
                faultyNodes.add(i);
        }
        assertEquals(expectedNumFaultyNodes, faultyNodes.size());
    }

    @Test
    public void testCoordinatorCanSwitchAlgorithm() throws Exception {
        coordinator.setAlgorithm(false);
        int delay = 10; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to node.");
        assertFalse(nodeList.get(0).node.queenAlgorithm);
        coordinator.setAlgorithm(true);
        Utils.timedWait(delay, "TEST: Wait on message to node.");
        assertTrue(nodeList.get(0).node.queenAlgorithm);
    }

    @Test
    public void testCoordinatorSetsInitialNormalizedWeightsOnNodes() throws Exception {
        ArrayList<Double> weights = coordinator.createInitialWeights();
        double normalizedWeight = (1.0 / numNodes);
        for(int i = 0; i < numNodes; i ++) {
            assertEquals(weights.get(i), normalizedWeight, 0.001);
        }
        for(int i = 0; i < numNodes; i ++) {
            assertEquals(weights.get(i), nodeList.get(i).node.weights.get(i), 0.001);
        }
    }

    @Test
    public void testAnchorWithQueenAlgorithm() throws Exception {
    	List<Double> weights = nodeList.get(0).node.weights;
        Node node = nodeList.get(0).node;
		weights.set(0, 0.1);
		weights.set(1, 0.2);
		weights.set(2, 0.3);
		weights.set(3, 0.2);
		weights.set(4, 0.1);
        weights.set(5, 0.1);

        node.algorithm = new WeightedQueen(node.nodeIndex, numNodes, Value.TRUE, node.msg, weights);
		int anchor = nodeList.get(0).node.algorithm.calculateAnchor();

		assertEquals(1, anchor);
    }

    @Test
    public void testAnchorWithKingAlgorithm() throws Exception {
        List<Double> weights = nodeList.get(0).node.weights;
        Node node = nodeList.get(0).node;
        weights.set(0, 0.1);
        weights.set(1, 0.2);
        weights.set(2, 0.3);
        weights.set(3, 0.2);
        weights.set(4, 0.1);
        weights.set(5, 0.1);

        node.algorithm = new WeightedKing(node.nodeIndex, numNodes, Value.TRUE, node.msg, weights);
        int anchor = nodeList.get(0).node.algorithm.calculateAnchor();

        assertEquals(2, anchor);
    }

    @Test
    public void testFailoverToKingAlgorithm() throws Exception {
        coordinator.createFaultyNodes(2);

        String name = "10.0.0.1/8";
        String expectedResponse = client.responses.get("TRUE");
        String response = validateNetwork(name, client);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testNodeCanSendValueToOtherNode() throws Exception {
        for(int i = 0; i < numNodes; i ++) {
            Node node = nodeList.get(i).node;
            node.algorithm = new WeightedQueen(node.nodeIndex, numNodes, Value.TRUE, node.msg, coordinator.createInitialWeights());
        }

        nodeList.get(0).node.algorithm.broadcastValue(Value.TRUE);

        int delay = 10; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        for(int i = 1; i < numNodes; i ++) {
            assertEquals(Value.TRUE, nodeList.get(i).node.algorithm.values[0]);
        }
    }

    public String validateNetwork(String name, Client myClient){
        return myClient.getResponse(myClient.validateNetwork(name));
    }

}
