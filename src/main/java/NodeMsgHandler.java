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
        if (messageType == MessageType.ALGORITHM) {
            server.setConsensusAlgorithm(Boolean.parseBoolean(request));
        } else if (messageType == MessageType.IS_FAULTY) {
            server.setFaultyBehavior(Boolean.parseBoolean(request));
        } else if (messageType == MessageType.FAULTY_SET) {
            server.consensusAlgorithm.addSuspectWeights(Utils.interpretStringAsList(request), src);
        } else if (messageType == MessageType.FAULTY_NODE) {
            server.setNodeFaulty(Integer.parseInt(request), src);
        } else if (messageType == MessageType.VALUE) {
            try {
                MsgHandler.debug("Node " + nodeIndex + " is setting the value for node " + src + " to " + request);
                server.consensusAlgorithm.setNodeValue(src, Value.valueOf(request));
            } catch (Exception e) {
                System.out.println(e.getMessage());
                MsgHandler.debug("Node " + nodeIndex + " did not receive value " + request + " from node " + src);
            }
        }
            else if (messageType == MessageType.LEADER_VALUE) {
                try {
                    MsgHandler.debug("Node " + nodeIndex + " is setting the leader value for node " + src + " to " + request);
                    server.consensusAlgorithm.setLeaderValue(src, Value.valueOf(request));
                } catch (Exception e) {
                    MsgHandler.debug("Node " + nodeIndex + " did not receive leader value " + request + " from node " + src);
                }
        } else if (messageType == MessageType.FAULT_VALUE) {
            try {
                MsgHandler.debug("Node " + nodeIndex + " is setting the fault value for node " + src + " to " + request);
                server.faultConsensusAlgorithm.setNodeValue(src, Value.valueOf(request));
            } catch (Exception e) {
                MsgHandler.debug("Node " + nodeIndex + " did not receive fault value " + request + " from node " + src);
            }
        }
    }

    @Override
    public ArrayList<String> actOnMsg(String request) {
        ArrayList<String> response = new ArrayList<>();

        server.accessBackend(request.split(",")[1]);
        return response;
    }
}