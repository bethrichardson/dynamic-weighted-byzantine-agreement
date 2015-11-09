
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * This system works to validate a set of ad bidding servers which can be both faulty and accurate.
 * The system sends ad requests to the servers to determine if a bid should be made on an ad placement.
 */
public class Client {
    Scanner din;
    PrintStream pout;
    Map<String, String> responses = new HashMap<>();
    List<InetSocketAddress> serverList;
    int serverIndex;
    InetSocketAddress server;
    Socket tcpserver;

    /**
     *
     * @param serverList List of servers with hostnames and ports organized by proximity to client
     */
    public Client(List<InetSocketAddress> serverList){
        this.serverList = serverList;
        this.serverList = serverList;
        this.serverIndex = 0;
        this.server = this.serverList.get(serverIndex);
        System.out.println("Client node initialized to " + this.server.toString());
        initializeResponseSet();
    }


    /**
     * Sends request to node using TCP
     *
     * @param request request to send to the node
     */
    private ArrayList<String> tcpRequest(String request){
        String retstring = "[]";
        try {
            retstring =  makeTCPRequest(request);
        } catch (Exception e) {
            System.err.println("Server aborted:" + e);
        }
//        System.out.println("Received from Server:" + retstring);
        return Utils.interpretStringAsList(retstring);
    }


    private void initializeResponseSet (){
        responses.put("NoResponse", "No node response.");
        responses.put("FALSE", "Do not use network.");
        responses.put("TRUE", "Use network.");
    }

    /**
     * Interprets primary node responses and outputs relevant information to user
     * Response is composed of the following values:
     * 0: "True/False", Should we bid on the ad?
     *
     * @param response response to interpret
     */
    public String getResponse(ArrayList<String> response){
        String result;
        if (response.get(0).equals("done")){
            result = responses.get("NoResponse");
        }
        else {
            String bid = response.get(0);

            if (bid.equals("true"))
                result = responses.get("True");
            else {
                result = responses.get("False");
            }
        }
        return result;
    }


    /**
     * validateNetwork <network> – .
     * Initiate a request through the coordinator to node pool
     *
     * @param network the network for which to make the request
     *  **/
    public ArrayList<String> validateNetwork(String network) {
        String request = "validateNetwork," + network;

        System.out.println("Attempting to validate network " + network + ".");

        return sendRequest(request);
    }



    /**
     * Send the request to the node using TCP protocol
     *
     * @param request the request string formatted for node
     */
    private ArrayList<String> sendRequest(String request){
        ArrayList<String>response;

        response = tcpRequest(request);
        MsgHandler.debug(getResponse(response));
        return response;
    }

    /**
     * Access a new TCP socket
     *
     * @throws IOException
     */
    public void getSocket() throws IOException {
        while (true) {
            try {
            this.tcpserver = new Socket();
            tcpserver.connect(server, 100);
            din = new Scanner(tcpserver.getInputStream());
            pout = new PrintStream(tcpserver.getOutputStream());
            } catch (SocketTimeoutException | ConnectException e) {
                updateServer();
                continue;
            }

            break;
        }
    }

    /**
     * Make a TCP request using a new socket
     *
     * @param request The request string formatted for the node
     * @return the response from the node
     * @throws IOException
     */
    public String makeTCPRequest(String request)
            throws IOException {
        getSocket();
        pout.println(request);
        pout.flush();
        String retValue = din.nextLine();
        tcpserver.close();
        return retValue;
    }

    public void updateServer(){
        serverIndex = (serverIndex + 1) % serverList.size();
        server = serverList.get(serverIndex);
        System.out.println("Attempting access to node at " + server.toString());
    }

    public static void main (String[] args) {
        InputReader reader = new InputReader();
        ArrayList<Integer> serverConfig = reader.inputNodeConfig();

        int startPort = serverConfig.get(0);

        List<InetSocketAddress> coordinatorAddressList = Utils.createServerList(startPort, 1);

        Client client = new Client(coordinatorAddressList);
        reader.waitForClientCommands(client);
    }
}