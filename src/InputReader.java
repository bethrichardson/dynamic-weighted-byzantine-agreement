import java.util.ArrayList;
import java.util.Scanner;


public class InputReader {
    Scanner sc;

    public InputReader() {
        sc = new Scanner(System.in);
    }


    public ArrayList<Integer> inputNodeConfig() {
        ArrayList<Integer> serverConfig = new ArrayList<>();

        while (serverConfig.isEmpty()) {
            System.out.println("Please enter starting port and total numbers of nodes.");

            String serverInput = sc.nextLine();
            String[] serverInputs = serverInput.split("\\s+");

            try {
                serverConfig.add(Integer.parseInt(serverInputs[0]));
                serverConfig.add(Integer.parseInt(serverInputs[1]));
            } catch (NumberFormatException nfe) {
                Utils.badInputs();
                serverConfig.clear();
            }
            catch (ArrayIndexOutOfBoundsException oob) {
                Utils.badInputs();
                serverConfig.clear();
            }
        }

        return serverConfig;
    }

    public void waitForClientCommands(Client client) {
        String publisher = null;

        while (sc.hasNextLine()) {

            while (publisher == null) {
                publisher = sc.nextLine();
                client.validatePlacement(publisher);
                publisher = null;
                }
        }
    }
}
