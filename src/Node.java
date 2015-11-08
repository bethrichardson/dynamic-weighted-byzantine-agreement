
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Node extends Server{
    public int nodeIndex;
    InetSocketAddress node;

    public ArrayList<String> knownNetworks;

    /**
     * Single node for calculating whether to allow deployment to a given network based on a set of known networks.
     *
     * @param nodeIndex Index in node list for current node
     * @param numNodes total number of nodes
     */
    public Node(List<InetSocketAddress> serverList, int nodeIndex, int numNodes, InetSocketAddress coordinator){
        super(numNodes, coordinator);
        this.nodeIndex = nodeIndex;
        this.knownNetworks = new ArrayList<>();
        initNetworkList();
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

    public void initNetworkList(){
        knownNetworks.add("10.0.12.1/24");
        knownNetworks.add("10.0.0.1/8");
        knownNetworks.add("10.0.0.7/8");
    }


    /**
     * Search for a network based on an input request
     *
     * @param name The string to search for in known publisher list
     * @return Whether the placement is a valid publisher.
     *      Returns -1 if not found.
     */
    public Boolean validateNetwork(String name){
        knownNetworks.contains(name);
        return knownNetworks.contains(name);
    }

    /**
     * Creates node responses for the Server class
     *
     * @param network client request to act upon.
     * @return String response to send back to client
     */
    public synchronized ArrayList<String> accessBackend(String network){
        ArrayList<String> response = new ArrayList<>();
        MsgHandler.debug("Accessing node with request: " + network);
        response.add(Boolean.toString(validateNetwork(network)));
        return response;
    }
}