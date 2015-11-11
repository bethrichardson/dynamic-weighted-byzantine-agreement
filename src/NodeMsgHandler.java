import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by brichardson on 11/7/15.
 */
public class NodeMsgHandler extends MsgHandler {
    Node server;
    int serverIndex;

    public NodeMsgHandler(Node server, int numServers, int serverIndex, List<InetSocketAddress> serverList) {
        super(numServers, serverList, serverIndex);
        this.server = server;
        this.serverIndex = serverIndex;
    }

    @Override
    public ArrayList<String> broadcastMsg(String request) {
        ArrayList<String> responses = new ArrayList<>();
        MsgHandler.debug("Broadcasting: " + request);
        for (int i = 0; i < numServers; i++) {
            if (i != nodeIndex)
                responses.add(sendMsg(request, i));
        }
        return responses;
    }

    public void sendMsgToCoordinator(String request){
        InetSocketAddress coordinator = server.coordinator;
        try {
            super.makeServerRequest(coordinator, request, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleMsg(int src, String tag, String request) {
        if (tag.equals("controlPrepareRound")) {
            server.prepareRound(request);
        }
        if (tag.equals("controlSetAlgorithm")) {
            server.setAlgorithm(Boolean.parseBoolean(request));
        }
        if (tag.equals("controlSetFaulty")) {
            server.setFaultyBehavior(Boolean.parseBoolean(request));
        }
        if (tag.equals("controlSetValue")) {
            server.algorithm.setNodeValue(src, Value.valueOf(request));
        }
    }

    @Override
    public ArrayList<String> actOnMsg(String request) {
        return server.accessBackend(request.split(",")[2]);
    }


}
