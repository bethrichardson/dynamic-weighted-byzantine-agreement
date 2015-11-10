
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * If a node is currently answering in concert with other servers, then we give that node greater
 * weight in future consensus calculations. If a node is currently answering in conflict with other servers,
 * then we give that node less weight in future consensus calculations.
 */
public class Coordinator extends Server{
    int startPort;
    ArrayList<Node> nodeObjectList;
    Value[][] faultyMatrix;
    List<Double> nodeWeights;

    public Coordinator(int startPort, int numNodes){
        super(numNodes, Utils.createServerList(startPort, 1).get(0));
        MsgHandler.debug("Coordinator configured at " + coordinator.toString());
        this.nodeList = Utils.createServerList(startPort + 1, numNodes); //init to all nodes
        this.numNodes = numNodes;
        this.startPort = startPort;
        nodeObjectList = new ArrayList<>();
        faultyMatrix = new Value[numNodes][numNodes];
        nodeWeights = new ArrayList<>(numNodes);
        createNodeSet();

    }

    public void updateFaultyMatrix(List<String> responses) {
        // TODO Parse out faulty lists or change input parameter to something processed
    }

    public List<Double> createInitialWeights(){
        nodeWeights.clear();

        double normalizedWeight = 1.0/numNodes;

        for (int i = 0; i < numNodes; i++){
            nodeWeights.add(normalizedWeight);
        }

        return nodeWeights;
    }

    // TODO Make badass
    public List<Double> updateWeights() {
        for (int j = 0; j < numNodes; j++) {
            for (int i = 0; i < numNodes; i++) {
                if(faultyMatrix[i][j].equals(Value.TRUE)) {
                    nodeWeights.set(j, 0.0);
                    break;
                }
            }
        }

        return Utils.normalizeWeights(nodeWeights);
    }

    public void createNodeSet(){
        for (int i = 0; i < numNodes; i++){
            nodeObjectList.add(new Node(nodeList, i, numNodes, coordinator, createInitialWeights()));
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

    public void setAlgorithm(Boolean queenAlgorithm){
        msg.broadcastMsg("controlSetAlgorithm," + Boolean.toString(queenAlgorithm));
    }

    public void setNodeFaulty(Integer i, Boolean actFaulty){
        msg.sendMsg("controlSetFaulty," + Boolean.toString(actFaulty), i);
    }

    public void resetFaultyNodes(){
        msg.broadcastMsg("controlSetFaulty," + Boolean.toString(false));
    }

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

        ArrayList<Integer> faultyNodes = selectFaultyNodes(numFaultyNodes);

        for (int i = 0; i < numFaultyNodes; i++){
            setNodeFaulty(faultyNodes.get(i), true);
        }
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
