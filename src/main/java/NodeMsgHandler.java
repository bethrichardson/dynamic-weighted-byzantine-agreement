import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class NodeMsgHandler extends MsgHandler {
    Node server;
    int serverIndex;
    InetSocketAddress coordinator;

    public NodeMsgHandler(Node server, int numServers, int serverIndex, List<InetSocketAddress> serverList, InetSocketAddress coordinator) {
        super(numServers, serverList, serverIndex, coordinator);
        this.server = server;
        this.serverIndex = serverIndex;
        this.coordinator = coordinator;
    }

    @Override
    public ArrayList<String> broadcastMsg(MessageType messageType, String request, Boolean expectResponse) {
        ArrayList<String> responses = new ArrayList<>();
        MsgHandler.debug("Broadcasting: " + messageType.toString()  + ":" + request);
        for (int i = 0; i < numServers; i++) {
            if (i != nodeIndex)
                responses.add(sendMsg(messageType, request, i, expectResponse));
        }
        return responses;
    }

    @Override
    public void handleControlMessage(int src, MessageType messageType, String request) {
        if (messageType == MessageType.SetAlgorithm) {
            server.setAlgorithm(Boolean.parseBoolean(request));
        }
        if (messageType == MessageType.SetFaulty) {
            server.setFaultyBehavior(Boolean.parseBoolean(request));
        }
        if (messageType == MessageType.FaultyNode) {
            server.setNodeFaulty(Integer.parseInt(request), src);
        }
        if (messageType == MessageType.SetValue) {
            try {
                server.algorithm.setNodeValue(src, Value.valueOf(request));
            }
            catch(Exception e){
                System.out.println(e.getMessage());
                server.algorithm.setNodeValue(src, Value.UNDECIDED); //set bad values to Undecided
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
