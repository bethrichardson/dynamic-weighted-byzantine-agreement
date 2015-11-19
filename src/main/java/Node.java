
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class Node extends Server{
    public int nodeIndex;
    InetSocketAddress node;
    ConsensusAlgorithm algorithm;
    Boolean queenAlgorithm = true;
    Boolean actFaulty = false;
    Value lastReply = Value.FALSE; //Used to produce an iterating response when faulty
    ArrayList<Double> weights;
    int round;

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

    public void setNodeFaulty(int j, int reporter){
        Double weight = this.algorithm.weights.get(reporter);
        this.algorithm.setNodeSuspectWeight(j, weight);
    }

    public Value returnBadAnswer(){
        return Value.UNDECIDED;
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

    public void checkForFaultyNodes(){
        ConsensusAlgorithm checkNodeAlgorithm;

        algorithm.gatherFaultyNodes();

        for (int j = 0; j < algorithm.faultySet.length; j++){
            checkNodeAlgorithm = new WeightedQueen(nodeIndex, numNodes, algorithm.faultySet[j], msg, algorithm.weights);
            int anchor = algorithm.calculateAnchor();

            for (int k = 0; k < anchor; k++) {
                checkNodeAlgorithm.runPhaseOne();
                checkNodeAlgorithm.runPhaseTwo();
                checkNodeAlgorithm.runLeaderPhase(k);
            }
                algorithm.faultySet[j] = checkNodeAlgorithm.V;
        }
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
    public ArrayList<String> accessBackend(String network){
        Value initialResponse = calculateResponse(network);

        if (queenAlgorithm){
            algorithm = new WeightedQueen(nodeIndex, numNodes, initialResponse, msg, weights);
        }
        else {
            algorithm = new WeightedKing(nodeIndex, numNodes, initialResponse, msg, weights);
        }

        ArrayList<String> response = new ArrayList<>();
        int anchor = algorithm.calculateAnchor();
        for (int k = 0; k < anchor; k++) {
            algorithm.runPhaseOne();
            algorithm.runPhaseTwo();
            algorithm.runLeaderPhase(k);
        }
        checkForFaultyNodes();
        updateWeights(); //TODO: Update the weights based on algorithm.faultySet
        weights = algorithm.weights;

        msg.sendMsg(MessageType.FinalValue, algorithm.V.toString(), -1, false);
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