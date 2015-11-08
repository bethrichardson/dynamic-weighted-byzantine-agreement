
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Node {
    public int numNodes = 1;
    public int nodeIndex;
    List<InetSocketAddress> nodeList;
    InetSocketAddress node;
    InetSocketAddress coordinator;
    TCPListenerThread tcpThread;
    MsgHandler msg;

    public ArrayList<String> knownPublishers;

    /**
     * Single node for calculating whether to bid on an ad placement based on a set of known publishers.
     *
     * @param nodeIndex Index in node list for current node
     * @param numNodes total number of nodes
     */
    public Node(List<InetSocketAddress> serverList, int nodeIndex, int numNodes, InetSocketAddress coordinator){
        this.numNodes = numNodes;
        this.nodeIndex = nodeIndex;
        this.knownPublishers = new ArrayList<>();
        initPublisherList();
        this.nodeList = serverList;
        this.coordinator = coordinator;
        this.msg = new NodeMsgHandler(this, numNodes, nodeIndex, serverList);


        this.node = msg.getNode(nodeIndex);
        MsgHandler.debug("Node configured at " + node.toString());

        //Set up listener thread for TCP
        try {
            tcpThread = new TCPListenerThread(node, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.start();
    }


    public void shutDown() throws IOException {
        tcpThread.interrupt();
    }

    public void initPublisherList(){
        knownPublishers.add("cnn.com");
        knownPublishers.add("ut.edu");
        knownPublishers.add("disney.com");
    }


    /**
     * Search for a publisher based on an input request
     *
     * @param name The string to search for in known publisher list
     * @return Whether the placement is a valid publisher.
     *      Returns -1 if not found.
     */
    public Boolean validatePlacement(String name){
        knownPublishers.contains(name);
        return knownPublishers.contains(name);
    }


    /**
     * Creates node responses for the Server class
     *
     * @param publisher client request to act upon.
     * @return String response to send back to client
     */
    public synchronized ArrayList<String> accessBackend(String publisher){
        ArrayList<String> response = new ArrayList<>();
        MsgHandler.debug("Accessing node with request: " + publisher);
        response.add(Boolean.toString(validatePlacement(publisher)));
        return response;
    }
}