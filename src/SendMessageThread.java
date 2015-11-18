import sun.plugin2.message.Message;

import java.net.InetSocketAddress;
import java.util.ArrayList;

public class SendMessageThread extends Thread {
    MsgHandler msg;
    int receivingNode;
    String request;
    InetSocketAddress coordinator;
    MessageType messageType;
    Boolean expectResponse;

    public SendMessageThread(MsgHandler msg, int receivingNode, InetSocketAddress coordinator, MessageType messageType,
                             String request, Boolean expectResponse) {
        this.msg = msg;
        this.receivingNode = receivingNode;
        this.request = request;
        this.coordinator = coordinator;
        this.messageType = messageType;
        this.expectResponse = expectResponse;
    }

    public void run() {
        MsgHandler.debug("Starting new send message thread for node " + msg.nodeIndex + ".");
        String response = msg.sendMsg(messageType, request, receivingNode, expectResponse);
        msg.sendMsg(messageType, response, -1, expectResponse);
    }
}



