
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Node extends Server{
    public int nodeIndex;
    InetSocketAddress node;
    WeightedQueen queen;
    WeightedKing king;
    Boolean queenAlgorithm = true;
    Boolean actFaulty = false;
    Value lastReply = Value.FALSE; //Used to produce an iterating response when faulty
    ArrayList<Double> weights;

    public ArrayList<String> knownNetworks;

    /**
     * Single node for calculating whether to allow deployment to a given network based on a set of known networks.
     *
     * @param nodeIndex Index in node list for current node
     * @param numNodes total number of nodes
     */
    public Node(List<InetSocketAddress> serverList, int nodeIndex, int numNodes, InetSocketAddress coordinator, ArrayList<Double> initialWeights){
        super(numNodes, coordinator);
        this.nodeIndex = nodeIndex;
        this.knownNetworks = new ArrayList<>();
        initNetworkList();
        this.nodeList = serverList;
        this.coordinator = coordinator;
        this.msg = new NodeMsgHandler(this, numNodes, nodeIndex, serverList);
        this.weights = initialWeights;


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
    public Value validateNetwork(String name){
        if (knownNetworks.contains(name))
            return Value.TRUE;
        else
            return Value.FALSE;
    }

    public void setAlgorithm(Boolean queenAlgorithm){
        this.queenAlgorithm = queenAlgorithm;
    }

    public void setFaultyBehavior(Boolean actFaulty){
        this.actFaulty = actFaulty;
    }

    public Value returnBadAnswer(){
        switch (lastReply){
            case TRUE:
                if (Utils.isEven(nodeIndex))
                    return Value.FALSE;
                else
                    return Value.UNDECIDED;
            case FALSE:
                if (Utils.isEven(nodeIndex))
                    return Value.UNDECIDED;
                else
                    return Value.TRUE;
            default:
                if (Utils.isEven(nodeIndex))
                    return Value.TRUE;
                else
                    return Value.FALSE;
        }
    }

    public Value calculateResponse(String network){
        Value reply;
        if (!actFaulty){
            reply = validateNetwork(network);
        }
        else {
            reply = returnBadAnswer();
        }
        lastReply = reply;
        return reply;
    }

    public Value[] checkForFaultyNodes(ConsensusAlgorithm algorithm){
        ConsensusAlgorithm checkNodeAlgorithm;
        for (int j = 0; j < algorithm.faultySet.length; j++){
            checkNodeAlgorithm = new ConsensusAlgorithm(nodeIndex, numNodes, algorithm.faultySet[j], msg, algorithm.weights);
            int alpha = algorithm.calculateAnchor();    //TODO: Need to get a correct alpha calculation here. and add value based on run.
            checkNodeAlgorithm.run(alpha);
            algorithm.faultySet[j] = checkNodeAlgorithm.myValue;
        }

        return algorithm.faultySet;
    }

    public void updateWeights(){
        //TODO: Update weights based on algorithm.faultySet
    }

    /**
     * Creates node responses for the Server class
     *
     * @param network client request to act upon.
     * @return String response to send back to client
     */
    public synchronized ArrayList<String> accessBackend(String network){
        ArrayList<String> response = new ArrayList<>();
        MsgHandler.debug("Accessing backend for node " + Integer.toString(nodeIndex) + " with request: " + network);
        Value initialResponse = calculateResponse(network);

        if (queenAlgorithm){
            queen = new WeightedQueen(nodeIndex, numNodes, initialResponse, msg, weights);
//        int alpha = queen.calculateAlpha();    //TODO: Need to get a correct alpha calculation here. and add value based on run.
//        queen.run(anchor);
//            queen.faultySet = checkForFaultyNodes(queen);
            weights = queen.weights;
        }
        else {
            king = new WeightedKing(nodeIndex, numNodes, initialResponse, msg, weights);
//        int alpha = king.calculateAlpha();    //TODO: Need to get a correct alpha calculation here. and add value based on run.
//        king.run(anchor);
//            king.faultySet = checkForFaultyNodes(king);
            weights = king.weights;
        }

        response.add((calculateResponse(network).toString())); //TODO: Should instead return value from consensus algorithm
        return response;
    }
}