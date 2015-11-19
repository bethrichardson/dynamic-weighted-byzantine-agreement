import java.util.List;
import java.net.InetSocketAddress;
import java.util.Random;
import static org.junit.Assert.*;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

public class TESTMultipleClientIntegration {
    List<InetSocketAddress> serverList;
    List<InetSocketAddress> coordinatorList;
    InetSocketAddress coordinatorAddress;
    Coordinator coordinator;
    int numNodes = 2;

    @BeforeClass
    public void setUp() throws Exception {
        coordinatorList = Utils.createServerList(9000, 1);
        serverList = Utils.createServerList(9001, numNodes);
        coordinatorAddress = coordinatorList.get(0);
        coordinator = Utils.setupNewTestServer(numNodes);
    }

    @AfterClass
    public void tearDown() throws Exception {
        coordinator.shutDown();

    }

    @org.testng.annotations.Test(threadPoolSize = 5, invocationCount = 5,  timeOut = 100)
    public void validPlacements() {
        Random rand = new Random();
        int n = rand.nextInt(1000);
        Client client = new Client(coordinatorList);

        String publisher = "10.0.0.1/8";
        String expectedResponse = client.responses.get("TRUE");
        String response = validatePlacement(client, publisher);

        assertEquals(expectedResponse, response);
    }

    public String validatePlacement(Client client, String name){
        return client.getResponse(client.validateNetwork(name));
    }
}
