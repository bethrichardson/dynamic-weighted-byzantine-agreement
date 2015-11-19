import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
public class ResponseThread extends Thread {
    MsgHandler msg;
    Socket theClient;
    int threadNumber;

    /**
     * Thread to access backend and respond for TCP requests to node
     * @param msg MessageHandler to which to route requests
     * @param s TCP socket upon which to respond
     * @param threadNumber Count for testing/logging multi-threading
     */
    public ResponseThread(MsgHandler msg, Socket s, int threadNumber) {
        this.msg = msg;
        theClient = s;
        this.threadNumber = threadNumber;
    }

    /**
     * Read in the request from the TCP socket.
     * Access the backend with requestString and then respond on same socket
     * with responseString from node.
     */
    public void run() {
        try {
//            MsgHandler.debug("Starting execution for thread " + Integer.toString(threadNumber) + " on node " +
//                    "" + Integer.toString(msg.nodeIndex));
            Scanner sc = new Scanner(theClient.getInputStream());
            PrintWriter pout = new PrintWriter(theClient.getOutputStream());
            String command = sc.nextLine();
            String responseString = msg.interpretMessage(command).toString();

            if (!responseString.equals("[]")){
                pout.print(responseString);
                pout.flush();
            }

            theClient.close();
//            MsgHandler.debug("Completing execution for thread " + Integer.toString(threadNumber) + " on node " +
//                    "" + Integer.toString(msg.nodeIndex));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}


