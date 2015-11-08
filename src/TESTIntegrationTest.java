import org.junit.*;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.Assert.*;

public class TESTIntegrationTest {
    Client client;
    List<InetSocketAddress> serverList;
    List<InetSocketAddress> coordinatorList;
    InetSocketAddress coordinatorAddress;
    Coordinator coordinator;

    @Before
    public void setUp() throws Exception {
        coordinatorList = Utils.createServerList(9000, 1);
        serverList = Utils.createServerList(9001, 2);
        client = new Client(serverList);
        coordinatorAddress = coordinatorList.get(0);
        coordinator = Utils.setupNewTestServer();
    }

    @After
    public void tearDown() throws Exception {
        coordinator.shutDown();

    }

    @Test
    public void testValidLookup() throws Exception {
        String name = "10.0.0.1/8";
        String expectedResponse = client.responses.get("True");
        String response = validateNetwork(name);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testInvalidLookup() throws Exception {
        String name = "bananas.com";
        String expectedResponse = client.responses.get("False");
        String response = validateNetwork(name);

        assertEquals(expectedResponse, response);
    }


    public String validateNetwork(String name){
        return client.getResponse(client.validateNetwork(name));
    }

}
