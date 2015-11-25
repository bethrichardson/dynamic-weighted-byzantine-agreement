import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * If a node is currently answering in concert with other servers, then we give that node greater
 * weight in future consensus calculations. If a node is currently answering in conflict with other servers,
 * then we give that node less weight in future consensus calculations.
 */
public class Coordinator extends Server{
    int startPort;
    ArrayList<Node> nodeObjectList;
    Boolean failed = false;

    public Coordinator(int startPort, int numNodes){
        super(numNodes, Utils.createServerList(startPort, 1).get(0));
        MsgHandler.debug("Coordinator configured at " + coordinator.toString());
        this.nodeList = Utils.createServerList(startPort + 1, numNodes); //init to all nodes
        this.numNodes = numNodes;
        this.startPort = startPort;
        nodeObjectList = new ArrayList<>();
        establishCommunication();

    }

    public ArrayList<Double> createInitialWeights(){
    	ArrayList<Double> weights = new ArrayList<Double>();
        double normalizedWeight = 1.0/numNodes;
        for (int i = 0; i < numNodes; i++){
            weights.add(normalizedWeight);
        }
        return weights;
    }

    public void establishCommunication(){
        this.msg = new CoordinatorMsgHandler(this, numNodes, nodeList);

        //Set up listener thread for TCP
        try {
            tcpThread = new TCPListenerThread(coordinator, msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
        tcpThread.start();

    }

    public void setAlgorithm(Boolean queenAlgorithm){
        msg.broadcastMsg(MessageType.ALGORITHM, Boolean.toString(queenAlgorithm), false);
    }

    public void setNodeFaulty(Integer i, Boolean actFaulty){
        msg.sendMsg(MessageType.IS_FAULTY, Boolean.toString(actFaulty), i, false);
    }

    public void resetFaultyNodes(){
        msg.broadcastMsg(MessageType.IS_FAULTY, Boolean.toString(false), false);
    }

    // TODO Ensure faulty node weights do not go over rho
    public ArrayList<Integer> selectFaultyNodes(int numFaultyNodes){
        ArrayList<Integer> faultyNodes = new ArrayList<>(numFaultyNodes);
        int badNode = -1;
        Boolean foundUniqueNode;
        for (int i = 0; i < numFaultyNodes; i++){
            foundUniqueNode = false;

            while(!foundUniqueNode){
                badNode = ThreadLocalRandom.current().nextInt(0, numNodes);
                if (!faultyNodes.contains(badNode)){
                    foundUniqueNode = true;
                }
            }

            faultyNodes.add(badNode);
        }
        return faultyNodes;
    }

    public void createFaultyNodes(int numFaultyNodes){
        resetFaultyNodes();

        // TODO Won't FIFO take care of this?
        Utils.timedWait(Constants.VALUE_TIMEOUT, "Waiting for all nodes to become non-faulty.");

        ArrayList<Integer> faultyNodes = selectFaultyNodes(numFaultyNodes);

        for (int i = 0; i < numFaultyNodes; i++){
            setNodeFaulty(faultyNodes.get(i), true);
        }
    }

    public static void main (String[] args) {
        InputReader reader = new InputReader();

        ArrayList<Integer> serverConfig = reader.inputNodeConfig();

        int startPort = serverConfig.get(0);
        int numNodes = serverConfig.get(1);

        new Coordinator(startPort, numNodes);
    }

}
