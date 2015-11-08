import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * If a node is currently answering in concert with other servers, then we give that node greater
 * weight in future consensus calculations. If a node is currently answering in conflict with other servers,
 * then we give that node less weight in future consensus calculations.
 */
public class Coordinator {
    int numNodes, startPort;
    ArrayList<Node> nodeList;
    List<InetSocketAddress> coordinatorAddressList, nodeAddressList;
    MsgHandler msg;
    InetSocketAddress coordinator;
    TCPListenerThread tcpThread;

    public Coordinator(int startPort, int numNodes){
        coordinatorAddressList = Utils.createServerList(startPort, 1); //first just create the coordinator
        this.coordinator = coordinatorAddressList.get(0); //set the socket for the coordinator
        MsgHandler.debug("Coordinator configured at " + coordinator.toString());
        nodeAddressList = Utils.createServerList(startPort + 1, numNodes); //init to all nodes
        this.numNodes = numNodes;
        this.startPort = startPort;
        nodeList = new ArrayList<>();
        createNodeSet();

    }

    public void createNodeSet(){
        for (int i = 0; i < numNodes; i++){
            nodeList.add(new Node(nodeAddressList, i, numNodes, this.coordinator));
        }

        this.msg = new CoordinatorMsgHandler(this, numNodes, nodeAddressList);

        //Set up listener thread for TCP
        try {
            tcpThread = new TCPListenerThread(coordinator, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.start();

    }

    public void shutDown() throws IOException {
        for (int i = 0; i < numNodes; i++){
            nodeList.get(i).shutDown();
        }
        tcpThread.interrupt();
    }

    public static void main (String[] args) {
        InputReader reader = new InputReader();

        ArrayList<Integer> serverConfig = reader.inputNodeConfig();

        int startPort = serverConfig.get(0);
        int numNodes = serverConfig.get(1);

        //Set up node pool
        new Coordinator(startPort, numNodes);
    }

}
