import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Utils {
    public static Boolean debugger = true;

    /**
     * Read in a CSV string and convert to ArrayList of Strings.
     * Used for reading client requests.
     *
     * @param str the CSV string to convert to list
     * @return the ArrayList version of the string
     */
    public static ArrayList<String> interpretStringAsList(String str){
        ArrayList<String> resultList = new ArrayList<>();
        str = str.replace("[", "").replace("]", "");
        resultList.addAll(Arrays.asList(str.split("\\s*,\\s*")));
        return resultList;
    }

    public synchronized static void myWait(Object obj, String myMessage) {
        MsgHandler.debug("waiting: " + myMessage);
        try {
            obj.wait(10);
        } catch (InterruptedException e) {
        }
    }

    public static void timedWait(int delay, String message){
        MsgHandler.debug(message);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void badInputs(){
        System.out.println("Please re-enter your inputs.");
    }

    public static List<InetSocketAddress> createServerList(int startPort, int numNodes){
        ArrayList<InetSocketAddress> ServerList = new ArrayList<>();
        try {
            for (int i = 0; i < numNodes; i ++)
            ServerList.add(new InetSocketAddress(InetAddress.getLocalHost(), startPort + i));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ServerList;
    }

    public static Coordinator setupNewTestServer(int numNodes){
        int startPort = 9000;
        return new Coordinator(startPort, numNodes);
    }

    public static ArrayList<Double> getInitialWeights(int numNodes){
        ArrayList<Double> weights = new ArrayList<Double>();
        double normalizedWeight = 1.0/numNodes;
        for (int i = 0; i < numNodes; i++){
            weights.add(normalizedWeight);
        }
        return weights;
    }


    public static ArrayList<ServerThread> setupNewTestNodePool(int numNodes, InetSocketAddress coordinator){
        int startPort = 9000;
        ArrayList<ServerThread>nodeThreadList = new ArrayList<>();
        for (int i = 0; i < numNodes; i++){
            try {
                ServerThread t = new ServerThread(createServerList(startPort + 1, numNodes), i, numNodes, coordinator, Utils.getInitialWeights(numNodes));
                nodeThreadList.add(t);
                t.run();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return nodeThreadList;
    }

    public static Boolean isEven(int i){
        return (i & 1) == 0;
    }

    public static JsonObject jsonFromString(String jsonObjectStr) {

        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }
}
