import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class CoordinatorMsgHandler extends MsgHandler {
    Coordinator coordinator;
    ArrayList<String> responses;

    public CoordinatorMsgHandler(Coordinator coordinator, int numServers, List<InetSocketAddress> serverList){
        super(numServers, serverList, -1, coordinator.coordinator);
        this.coordinator = coordinator;
        responses = new ArrayList<>();
    }

    @Override
    public void handleControlMessage(int src, MessageType messageType, String request) {
        if (messageType == MessageType.SetFaulty) {
            coordinator.createFaultyNodes(Integer.parseInt(request));
        }
        if (messageType == MessageType.FinalValue) {
            responses.add(request);
        }
    }

    public Value determineConsensus(){
        double s0 = 0.0; double s1 = 0.0;
        Value currentResponse;
        for (int i = 0; i < responses.size(); i++){
            currentResponse = Value.valueOf(responses.get(i));
            MsgHandler.debug("Received from node " + i + ": " + currentResponse);
            if (currentResponse == Value.TRUE) s0++;
            if (currentResponse == Value.FALSE) s1++;
        }
        if (s0 >= 3.0/4) return Value.TRUE;
        if (s1 >= 3.0/4) return Value.FALSE;
        else return Value.UNDECIDED;
    }

    @Override
    public ArrayList<String> actOnMsg(String request){
        ArrayList<String> responseToClient = new ArrayList<>();
        responses = new ArrayList<>();
        super.broadcastMsg(MessageType.ClientRequest, request, false);

        Utils.timedWait(10000, "Waiting in Coordinator for responses.");

        Value response = determineConsensus();
        if (response != Value.UNDECIDED) {
            responseToClient.add(response.toString());
        }
        else if (!coordinator.failed){
            coordinator.setAlgorithm(false);
            return actOnMsg(request);
        }
        else {
            throw new RuntimeException("Consensus Algorithm failed after failing over to Weighted King Algorithm.");
        }
        return responseToClient;
    }
}
