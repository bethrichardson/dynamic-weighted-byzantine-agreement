import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class NodeMsgHandler extends MsgHandler {
    Node server;
    InetSocketAddress coordinator;

    public NodeMsgHandler(Node server, int numServers, int nodeIndex, List<InetSocketAddress> serverList, InetSocketAddress coordinator) {
        super(numServers, serverList, nodeIndex, coordinator);
        this.server = server;
        this.coordinator = coordinator;
    }

    @Override
    public void handleControlMessage(int src, MessageType messageType, String request) {
        String[] values;

        switch (messageType) {
            case ALGORITHM:
                server.setConsensusAlgorithm(Boolean.parseBoolean(request));
                break;
            case IS_FAULTY:
                server.setFaultyBehavior(Boolean.parseBoolean(request));
                break;
            case FAULTY_SET:
                server.consensusAlgorithm.addSuspectWeights(Utils.interpretStringAsList(request), src);
                break;
            case PHASE_ONE_VALUE:
                values = request.split(",");
                server.consensusAlgorithm.setPhaseOneNodeValue(src, Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
            case PHASE_TWO_VALUE:
                values = request.split(",");
                server.consensusAlgorithm.setPhaseTwoNodeValue(src, Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
            case LEADER_VALUE:
                values = request.split(",");
                server.consensusAlgorithm.setLeaderValue(Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
            case FAULT_PHASE_ONE_VALUE:
                values = request.split(",");
                server.faultConsensusAlgorithms[Integer.parseInt(values[2])].setPhaseOneNodeValue(src, Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
            case FAULT_PHASE_TWO_VALUE:
                values = request.split(",");
                server.faultConsensusAlgorithms[Integer.parseInt(values[2])].setPhaseTwoNodeValue(src, Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
            case FAULT_LEADER_VALUE:
                values = request.split(",");
                server.faultConsensusAlgorithms[Integer.parseInt(values[2])].setLeaderValue(Value.valueOf(values[0]), Integer.parseInt(values[1]));
                break;
        }
    }

    @Override
    public ArrayList<String> actOnMsg(String request) {
        ArrayList<String> response = new ArrayList<>();

        server.accessBackend(request.split(",")[1]);
        return response;
    }
}