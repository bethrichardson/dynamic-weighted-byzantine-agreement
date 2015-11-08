import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.*;

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

    public static Coordinator setupNewTestServer(){
        int numNodes = 2;
        int startPort = 9000;
        return new Coordinator(startPort, numNodes);
    }
}
