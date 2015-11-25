import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class MsgHandler {
    int numServers;
    List<InetSocketAddress> serverList;
    int nodeIndex;
    InetSocketAddress coordinator;

    public MsgHandler(int numServers, List<InetSocketAddress> serverList, int nodeIndex, InetSocketAddress coordinator){
        this.numServers = numServers;
        this.serverList = serverList;
        this.nodeIndex = nodeIndex;
        this.coordinator = coordinator;
    }

    public InetSocketAddress getNode(int serverIndex){
        return serverList.get(serverIndex);
    }

    public ArrayList<String> broadcastMsg(MessageType messageType, String request, Boolean expectResponse) {
        ArrayList<String> responses = new ArrayList<>();
        MsgHandler.debug("Broadcasting from Node " + nodeIndex + ": " + messageType.toString()  + ":" + request);

        for (int i = 0; i < numServers; i++) {
            if (i != nodeIndex) {
                MsgHandler msgHandler = new MsgHandler(numServers, serverList, nodeIndex, coordinator);
                SendMessageThread sendThread = new SendMessageThread(msgHandler, i, coordinator, messageType, request, expectResponse);

                sendThread.start();
            }
        }
        return responses;
    }

    public static void debug(String log){
        if (Utils.debugger){
            System.out.println((System.currentTimeMillis() & 0xFFFFFFl) +": DEBUG: " + log);
        }
    }

    public static JsonObject buildPayload(MessageType messageType, String request, int serverId, int nodeIndex){
        Map<String, Object> config = new HashMap<String, Object>();
        //if you need pretty printing
        config.put("javax.json.stream.JsonGenerator.prettyPrinting", Boolean.valueOf(true));
        JsonBuilderFactory factory = Json.createBuilderFactory(config);
        JsonObject payload = factory.createObjectBuilder()
                .add("MessageType", messageType.toString())
                .add("Request", request)
                .add("ServerID", serverId)
                .add("SendingID", nodeIndex)
                .build();

        return payload;
    }

    public String sendMsg(MessageType messageType, String request, int serverId, Boolean expectResponse){
        InetSocketAddress server;
        String serverRequest;
        String response = "[]";
        if (request != null) {
            if (serverId == Constants.COORDINATOR_ID) {
                server = coordinator;
            } else {
                server = getNode(serverId);
            }

            serverRequest = buildPayload(messageType, request, serverId, this.nodeIndex).toString();

            try {
                return makeServerRequest(server, serverRequest, expectResponse);
            } catch (IOException e) {
                System.out.println(e.getLocalizedMessage());
            }
        }
        return response;
    }

    /**
     * Make a TCP request using a new socket
     *
     * @param server The destination of the request
     * @param request The request string formatted for the node
     * @param expectResponse Does the requester need to wait for a response?
     * @return the response from the node
     * @throws IOException
     */
    public String makeServerRequest(InetSocketAddress server, String request, boolean expectResponse)
            throws IOException {
        String retValue = null;

        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            socket.connect(server, Constants.CONNECTION_TIMEOUT);

            try (Scanner din = new Scanner(socket.getInputStream());
                 PrintStream pout = new PrintStream(socket.getOutputStream())) {
                pout.println(request);
                pout.flush();

                retValue = din.nextLine();
            }

            return retValue;
        } catch (SocketTimeoutException | ConnectException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<String> interpretMessage(String request){
        ArrayList<String> response = new ArrayList<>();
        try {
            JsonObject obj = Utils.jsonFromString(request);
            int sendingId = obj.getInt("SendingID");
            MessageType messageType = MessageType.valueOf(obj.getString("MessageType"));
            String requestString = obj.getString("Request");

            if (messageType == MessageType.CLIENT_REQUEST){
                response = actOnMsg(requestString);
            }
            else {
                handleControlMessage(sendingId, messageType, requestString);
            }
        } catch (JsonException e) {
            e.printStackTrace();
        }
        return response;
    }

    public void handleControlMessage(int src, MessageType messageType, String request) {}

    public ArrayList<String> actOnMsg(String request){
        ArrayList<String> response = new ArrayList<>();
        return response;
    }
}