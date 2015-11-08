import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;


public class Server {
    int numNodes;
    List<InetSocketAddress> nodeList;
    MsgHandler msg;
    InetSocketAddress coordinator;
    TCPListenerThread tcpThread;

    public Server(int numNodes, InetSocketAddress coordinator){
        this.numNodes = numNodes;
        this.coordinator = coordinator;
    }

    public void shutDown() throws IOException {
        tcpThread.interrupt();
    }

}
