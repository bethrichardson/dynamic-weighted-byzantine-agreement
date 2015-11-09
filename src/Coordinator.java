import java.io.IOException;
import java.util.ArrayList;

/**
 * If a node is currently answering in concert with other servers, then we give that node greater
 * weight in future consensus calculations. If a node is currently answering in conflict with other servers,
 * then we give that node less weight in future consensus calculations.
 */
public class Coordinator extends Server{
    int startPort;
    ArrayList<Node> nodeObjectList;

    public Coordinator(int startPort, int numNodes){
        super(numNodes, Utils.createServerList(startPort, 1).get(0));
        MsgHandler.debug("Coordinator configured at " + coordinator.toString());
        this.nodeList = Utils.createServerList(startPort + 1, numNodes); //init to all nodes
        this.numNodes = numNodes;
        this.startPort = startPort;
        nodeObjectList = new ArrayList<>();
        createNodeSet();

    }

    public int alpha (){
    	return 0;
    }
    
    public void createNodeSet(){
        for (int i = 0; i < numNodes; i++){
            nodeObjectList.add(new Node(nodeList, i, numNodes, coordinator));
        }

        this.msg = new CoordinatorMsgHandler(this, numNodes, nodeList);

        //Set up listener thread for TCP
        try {
            tcpThread = new TCPListenerThread(coordinator, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.start();

    }

    public void switchAlgorithm(Boolean queenAlgorithm){
        msg.broadcastMsg("controlSwitchAlgorithm," + Boolean.toString(queenAlgorithm));
    }

    @Override
    public void shutDown() throws IOException {
        for (int i = 0; i < numNodes; i++){
            nodeObjectList.get(i).shutDown();
        }
        super.shutDown();
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
