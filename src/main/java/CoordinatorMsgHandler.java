import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class CoordinatorMsgHandler extends MsgHandler {
    Coordinator coordinator;
    String[] responses;

    public CoordinatorMsgHandler(Coordinator coordinator, int numServers, List<InetSocketAddress> serverList){
        super(numServers, serverList, -1, coordinator.coordinator);
        this.coordinator = coordinator;
        responses = new String[serverList.size()];
    }

    @Override
    public synchronized ArrayList<String> handleControlMessage(int src, MessageType messageType, String request) {
        if (messageType == MessageType.IS_FAULTY) {
            coordinator.createFaultyNodes(Integer.parseInt(request));
        }
        if (messageType == MessageType.FINAL_VALUE) {
            responses[src] = request;

            MsgHandler.debug("Coordinator has received value " + request + "from node" + src);

            for (String s : responses) {
                if(s == null) {
                    return new ArrayList<>();
                }
            }

            MsgHandler.debug("Coordinator has received all values from nodes: " + Arrays.toString(responses));

            notify();
        }
        return new ArrayList<>();
    }

    // TODO Won't this be different for the two algos?
    // TODO Shouldn't we just be waiting for a minimum number of same responses? Keep a Map of value -> counts
    public Value determineConsensus(){
        double limitWeight = coordinator.failed ? 2.0/3 * numServers : .75 * numServers;
        double s0 = 0.0; double s1 = 0.0;
        Value currentResponse;
        for (int i = 0; i < responses.length; i++){
            currentResponse = Value.valueOf(responses[i]);
            MsgHandler.debug("Received from node " + i + ": " + currentResponse);
            if (currentResponse == Value.TRUE) s0++;
            if (currentResponse == Value.FALSE) s1++;
        }
        if (s0 >= limitWeight){
            System.out.println("CONSENSUS REACHED: Nodes decided True with weight: " + s0 + " and limit of: " + limitWeight);
            return Value.TRUE;
        }
        if (s1 >= limitWeight){
            System.out.println("CONSENSUS REACHED: Nodes decided False with weight: " + s1 + " and limit of: " + limitWeight);
            return Value.FALSE;
        }
        else {
            System.out.println("CONSENSUS NOT REACHED: Nodes Undecided. s0: " + s0 + ", s1: " + s1 + " and limit of: " + limitWeight);
            return Value.UNDECIDED;
        }
    }

    @Override
    public ArrayList<String> broadcastMsg(MessageType messageType, String request, Boolean expectResponse) {
        ArrayList<String> responses = new ArrayList<>();
        MsgHandler.debug("Broadcasting from Node " + nodeIndex + ": " + messageType.toString()  + ":" + request);

        for (int i = 0; i < numServers; i++) {
            if (i != nodeIndex) {
                SendMessageThread sendThread = new SendMessageThread(this, i, super.coordinator, messageType, request, expectResponse);

                sendThread.start();
            }
        }
        return responses;
    }

    @Override
    public ArrayList<String> actOnMsg(String request){
        ArrayList<String> responseToClient = new ArrayList<>();
        super.broadcastMsg(MessageType.CLIENT_REQUEST, request, false);

        waitForValues();

        Value response = determineConsensus();

        if (response != Value.UNDECIDED) {
            responseToClient.add(response.toString());
        } else if (!coordinator.failed){
            MsgHandler.debug("COORDINATOR: Switching to Weighted King due to Weighted Queen failure");

            coordinator.setAlgorithm(false);
            coordinator.failed = true;

            super.broadcastMsg(MessageType.CLIENT_REQUEST, request, false);

            waitForValues();

            response = determineConsensus();

            if (response != Value.UNDECIDED) {
                responseToClient.add(response.toString());
            } else {
                responseToClient.add(response.toString());
                Exception rte = new RuntimeException("Consensus Algorithm failed after failing over to Weighted King Algorithm.");
                System.out.println(rte.getMessage());
            }
        }

        return responseToClient;
    }

    public synchronized void waitForValues(){
        try {
            wait(Constants.COORDINATOR_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}