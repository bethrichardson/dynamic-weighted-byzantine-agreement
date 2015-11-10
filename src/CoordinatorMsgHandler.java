import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class CoordinatorMsgHandler extends MsgHandler {
    Coordinator coordinator;

    public CoordinatorMsgHandler(Coordinator coordinator, int numServers, List<InetSocketAddress> serverList){
        super(numServers, serverList, -1);
        this.coordinator = coordinator;
    }

    @Override
    public synchronized void handleMsg(int src, String tag, String request) {
        if (tag.equals("controlSetFaulty")) {
            coordinator.createFaultyNodes(Integer.parseInt(request));
        }
    }

    @Override
    public ArrayList<String> actOnMsg(String request){
        ArrayList<String> response = new ArrayList<>();
        ArrayList<String> responses = super.broadcastMsg(request);
        Boolean consensusDecision = true;
        String currentResponse;
        for (int i = 0; i < responses.size(); i++){
            responses.set(i, responses.get(i).replaceAll("\\[", "").replaceAll("\\]", ""));
            currentResponse = responses.get(i);
            MsgHandler.debug("Received from node " + Integer.toString(i) + ": " + currentResponse);
            if (i > 0){
                consensusDecision = (currentResponse.equals(responses.get(i-1))); //TODO: determine consensus
            }
        }
        if (consensusDecision) {
            response.add(responses.get(0));
        }
        else {
            coordinator.setAlgorithm(false);
            actOnMsg(request); // TODO Return this value and avoid recursion
        }

        // TODO Is this the right place for this?
        coordinator.updateFaultyMatrix(responses);
        coordinator.updateWeights();

        return response;
    }
}
