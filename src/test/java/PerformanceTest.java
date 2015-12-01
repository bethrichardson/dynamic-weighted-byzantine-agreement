import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PerformanceTest {
    Client client;
    List<InetSocketAddress> serverList;
    List<InetSocketAddress> coordinatorList;
    ArrayList<ServerThread> nodeList;
    InetSocketAddress coordinatorAddress;
    Coordinator coordinator;
    String name = "10.0.0.1/8";
    long startTime;
    int numNodes = 4;

    public void createServerConfig(){
        coordinatorList = Utils.createServerList(9000, 1);
        serverList = Utils.createServerList(9001, numNodes);
        client = new Client(coordinatorList);
        coordinatorAddress = coordinatorList.get(0);
        coordinator = Utils.setupNewTestServer(numNodes);
        nodeList = Utils.setupNewTestNodePool(numNodes, coordinatorAddress);
    }

    public void startWatch(){
        startTime = System.nanoTime();
    }

    public void useKingAlgorithm(){
        for(int i = 0; i < nodeList.size(); i ++) {
            nodeList.get(i).node.queenAlgorithm = false;
        }
    }

    public long stopWatch(String testName){
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;

        System.out.println(testName + " took " + duration + " milliseconds.");
        return duration;
    }

    @After
    public void tearDown() throws Exception {
        coordinator.shutDown();
        for (ServerThread thread : nodeList) {
            thread.node.shutDown();
        }
    }


    @Test
    public void testNonFaultyLookupQueen() throws Exception {
        createServerConfig();
        String name = "10.0.0.1/8";
        startWatch();
        validateNetwork(name, client);
        stopWatch("Non-faulty Queen");
    }

    @Test
    public void testNonFaultyLookupKing() throws Exception {
        createServerConfig();
        useKingAlgorithm();

        startWatch();
        validateNetwork(name, client);

        stopWatch("Non-faulty King");
    }

    @Test
    public void testFaultyLookupQueen() throws Exception {
        createServerConfig();
        coordinator.setNodeFaulty(0, true);

        int delay = 1000;
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        startWatch();
        validateNetwork(name, client);
        stopWatch("Faulty Queen");
    }

    @Test
    public void testFaultyLookupKing() throws Exception {
        createServerConfig();
        coordinator.setNodeFaulty(0, true);
        useKingAlgorithm();

        int delay = 1000; //milliseconds
        Utils.timedWait(delay, "TEST: Wait on message to nodes.");

        startWatch();
        validateNetwork(name, client);
        stopWatch("Faulty King");
    }

    @Test
    public void testFailoverToKingAlgorithm() throws Exception {
        createServerConfig();
        coordinator.createFaultyNodes(1);

        startWatch();
        validateNetwork(name, client);
        stopWatch("Failover");
    }


    public String validateNetwork(String name, Client myClient){
        return myClient.getResponse(myClient.validateNetwork(name));
    }

}
