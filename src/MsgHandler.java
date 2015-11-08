import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MsgHandler {
    int numServers;
    List<InetSocketAddress> serverList;
    int nodeIndex;

    public MsgHandler(int numServers, List<InetSocketAddress> serverList, int nodeIndex){
        this.numServers = numServers;
        this.serverList = serverList;
        this.nodeIndex = nodeIndex;
    }

    public InetSocketAddress getNode(int serverIndex){
        return serverList.get(serverIndex);
    }

    public ArrayList<String> broadcastMsg(String request) {
        ArrayList<String> responses = new ArrayList<>();
        MsgHandler.debug("Broadcasting: " + request);
        for (int i = 0; i < numServers; i++) {
            responses.add(sendMsg(request, i));
        }
        return responses;
    }

    public static void debug(String log){
        if (Utils.debugger){
            System.out.println("DEBUG: " + log);
        }
    }

    public String sendMsg(String request, int serverId){
        InetSocketAddress server = getNode(serverId);
        String response = "";
        try {
            return makeServerRequest(server, request, true);
        } catch (IOException e) {
            System.out.println(e.getLocalizedMessage());
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
    public String makeServerRequest(InetSocketAddress server, String request, boolean expectResponse) throws IOException {
        Socket socket;
        Scanner din;
        PrintStream pout;
        String retValue = null;

        try {
            socket = new Socket();
            socket.connect(server, 100);
            socket.setReuseAddress(true);
        } catch (SocketTimeoutException | ConnectException e) {
            return null;
        }
            din = new Scanner(socket.getInputStream());

            pout = new PrintStream(socket.getOutputStream());
            pout.println(request);
            pout.flush();

            if (expectResponse) retValue = din.nextLine();

            socket.close();

            pout.close();

        return retValue;
    }
    public void interpretMessage(String request){
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        handleMsg(Integer.parseInt(requestList.get(1)), Integer.parseInt(requestList.get(2)), requestList.get(0));
    }

    public void handleMsg(int timeStamp, int src, String tag) {
       //Handle control messages
    }

    public ArrayList<String> actOnMsg(String request){
        ArrayList<String> response = new ArrayList<>();
        response.add("done");
        return response;
    }

    public ArrayList<String> routeMessage(String request){
        ArrayList<String> requestList =  Utils.interpretStringAsList(request);
        ArrayList<String> response = new ArrayList<>();
        String method = requestList.get(0);

        MsgHandler.debug("Accessing node with request: " + request);

        switch(method){
            case("control"):
                response.add("done");
                interpretMessage(request); //Control messages should be sent through here
                break;
            default:
                response = actOnMsg(request); //What would this be used for?
                break;

        }

        return response;
    }
}
