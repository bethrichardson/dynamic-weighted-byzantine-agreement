import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node extends Server{
    public int nodeIndex;
    InetSocketAddress node;
    ConsensusAlgorithm consensusAlgorithm;
    ConsensusAlgorithm faultConsensusAlgorithm;
    Boolean queenAlgorithm = true;
    Boolean actFaulty = false;
    Value lastReply = Value.FALSE; //Used to produce an iterating response when faulty
    List<Double> weights;

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
        this.msg = new NodeMsgHandler(this, numNodes, nodeIndex, serverList, coordinator);
        this.weights = initialWeights;


        this.node = msg.getNode(nodeIndex);
        MsgHandler.debug("Node " + nodeIndex + " configured at " + node.toString());

        if (queenAlgorithm){
            consensusAlgorithm = new WeightedQueen(nodeIndex, numNodes, null, msg, MessageType.VALUE, weights, actFaulty);
            faultConsensusAlgorithm = new WeightedQueen(nodeIndex, numNodes, null, msg, MessageType.FAULT_VALUE, weights, actFaulty);
        } else {
            consensusAlgorithm = new WeightedKing(nodeIndex, numNodes, null, msg, MessageType.VALUE, weights, actFaulty);
            faultConsensusAlgorithm = new WeightedKing(nodeIndex, numNodes, null, msg, MessageType.FAULT_VALUE, weights, actFaulty);
        }

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

    public void setConsensusAlgorithm(Boolean queenAlgorithm){
        this.queenAlgorithm = queenAlgorithm;
    }

    public void setFaultyBehavior(Boolean actFaulty){
        this.actFaulty = actFaulty;
    }

    public void setNodeFaulty(int j, int reporter){
        Double weight = this.consensusAlgorithm.weights.get(reporter);
        this.consensusAlgorithm.setNodeSuspectWeight(j, weight);
    }

    public void returnBadAnswer(){
        MsgHandler msgHandler = new MsgHandler(msg.numServers, msg.serverList, msg.nodeIndex, msg.coordinator);
        SendMessageThread sendThread = new SendMessageThread(msgHandler, Constants.COORDINATOR_ID, coordinator, MessageType.FINAL_VALUE, Value.UNDECIDED.toString(), false);

        sendThread.start();

        lastReply = Value.UNDECIDED;
    }

    public Value calculateResponse(String network){
        return validateNetwork(network);
    }

    public void checkForFaultyNodes(){
        MsgHandler.debug("Node " + nodeIndex + " has this faultySet before fault consensus: " + Arrays.toString(consensusAlgorithm.faultySet));

        consensusAlgorithm.gatherFaultyNodes();

        faultConsensusAlgorithm.weights = weights;

        int anchor = faultConsensusAlgorithm.calculateAnchor();

        for (int j = 0; j < consensusAlgorithm.faultySet.length; j++){
            faultConsensusAlgorithm.V = consensusAlgorithm.faultySet[j];
            faultConsensusAlgorithm.actFaulty = actFaulty;

            for (int k = 0; k < anchor; k++) {
                faultConsensusAlgorithm.runPhaseOne();
                faultConsensusAlgorithm.runPhaseTwo();
                faultConsensusAlgorithm.runLeaderPhase(k);
            }

            consensusAlgorithm.setNodeFaultyState(j, faultConsensusAlgorithm.V);

            MsgHandler.debug("Node " + nodeIndex  + " has this faultySet after consensus on node " + j + ": " + Arrays.toString(consensusAlgorithm.faultySet));
        }
    }

    public void updateWeights(){
        WeightUpdate.flat(consensusAlgorithm.faultySet, weights);
        consensusAlgorithm.weights = weights;

        MsgHandler.debug("Updated weights for node " + nodeIndex);
        MsgHandler.debug("New weights: " + weights);
        MsgHandler.debug("Based on faults: " + Arrays.toString(consensusAlgorithm.faultySet));
    }


    /**
     * Creates node responses for the Server class
     *
     * @param network client request to act upon.
     * @return String response to send back to client
     */
    public ArrayList<String> accessBackend(String network){
        ArrayList<String> response = new ArrayList<>();

        consensusAlgorithm.V = calculateResponse(network);
        consensusAlgorithm.actFaulty = actFaulty;

        int anchor = consensusAlgorithm.calculateAnchor();

        for (int k = 0; k < anchor; k++) {
            consensusAlgorithm.runPhaseOne();
            consensusAlgorithm.runPhaseTwo();
            consensusAlgorithm.runLeaderPhase(k);
            consensusAlgorithm.runFaultyNodePhase(k);
        }

//        checkForFaultyNodes();
        updateWeights();

        if (!actFaulty) {
            MsgHandler msgHandler = new MsgHandler(msg.numServers, msg.serverList, msg.nodeIndex, msg.coordinator);
            SendMessageThread sendThread = new SendMessageThread(msgHandler, Constants.COORDINATOR_ID, coordinator, MessageType.FINAL_VALUE, consensusAlgorithm.V.toString(), false);

            sendThread.start();

            lastReply = consensusAlgorithm.V;
        } else {
            returnBadAnswer();
        }

        return response;
    }

    public static void main (String[] args) {
        InputReader reader = new InputReader();

        ArrayList<Integer> serverConfig = reader.inputNodeConfig();

        int startPort = serverConfig.get(0);
        int numNodes = serverConfig.get(1);
        List<InetSocketAddress> nodeList;
        InetSocketAddress coordinator = Utils.createServerList(startPort, 1).get(0);
        ArrayList<Thread> nodeThreadList;

        ArrayList<Double> weights = new ArrayList<Double>();
        double normalizedWeight = 1.0/numNodes;
        for (int i = 0; i < numNodes; i++){
            weights.add(normalizedWeight);
        }

        //Set up node pool

        nodeList = Utils.createServerList(startPort + 1, numNodes); //init to all nodes
        nodeThreadList = new ArrayList<>();
        for (int i = 0; i < numNodes; i++){
            try {
                ServerThread t = new ServerThread(nodeList, i, numNodes, coordinator, weights);
                nodeThreadList.add(t);
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
