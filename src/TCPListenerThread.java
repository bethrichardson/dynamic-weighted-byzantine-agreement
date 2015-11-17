import java.net.*; import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TCPListenerThread extends Thread {
    List<Thread> threadList = new ArrayList<>();
    ServerSocket tcpListener;
    SocketAddress socketAddress;
    MsgHandler msg;
    int numThreads = 0;

    /**
     * Listener thread listens for TCP client connections
     *
     * @param node The backend node to perform requests against
     */
    public TCPListenerThread(InetSocketAddress node, MsgHandler msg) throws IOException {
        tcpListener = new ServerSocket();
        this.msg = msg;
        socketAddress = new InetSocketAddress(InetAddress.getByName(node.getHostName()), node.getPort());
        tcpListener.bind(socketAddress);
    }

    @Override
    public void interrupt(){
        Iterator<Thread> iterator = threadList.iterator();
        while (iterator.hasNext()) {

            iterator.next().interrupt();
        }
        try {
            tcpListener.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.interrupt();
    }

    /**
     * Listen for incoming TCP requests and create a ResponseThread to handle
     * any incoming requests to backend. Pass off the socket for each new request to
     * a new ResponseThread to read the request and send response.
     */
    public void createSocketAndThread() {
        try {
            Socket s;
            while ( (s = tcpListener.accept()) != null) {
                numThreads++;
                Thread t = new ResponseThread(msg, s, threadList.size() + 1);
                threadList.add(t);
                t.start();
            }
            createSocketAndThread();

        } catch (IOException e) {
            //DO nothing.
        }
    }

    public void run() {
        createSocketAndThread();
    }
}


