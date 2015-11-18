import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by brichardson on 11/12/15.
 */
public class ServerThread extends Thread {
    public int nodeIndex;
    Node node;
    ArrayList<Double> weights;
    int numNodes;
    List<InetSocketAddress> nodeList;
    MsgHandler msg;
    InetSocketAddress coordinator;


    /**
     * Server thread functions as an independent node
     *
     */
    public ServerThread(List<InetSocketAddress> serverList, int nodeIndex, int numNodes, InetSocketAddress coordinator, ArrayList<Double> initialWeights) throws IOException {
        this.nodeIndex = nodeIndex;
        this.nodeList = serverList;
        this.coordinator = coordinator;
        this.weights = initialWeights;
        this.numNodes = numNodes;
    }

    public void createNode() {
        this.node = new Node(nodeList, nodeIndex, numNodes, coordinator, weights);
    }

    public void run() {
        MsgHandler.debug("Starting new node thread for node " + nodeIndex + ".");
        createNode();
    }
}


