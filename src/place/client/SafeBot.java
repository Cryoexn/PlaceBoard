package place.client;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.network.NetworkClient;
import java.io.IOException;

import static java.lang.Thread.sleep;


public class SafeBot {

    private static String userName;
    private static ClientModel model;
    private static NetworkClient serverConn;

    public static void go() {
        while (true) {
            try {
                PlaceTile tempTile = new PlaceTile(0, 0, userName, PlaceColor.getPlaceColor("green"));
                serverConn.sendMove(tempTile);
                sleep(3000);
            }catch (IOException | InterruptedException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceBot host port username");
            System.exit(0);
        }else{
            // Get host info from command line
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            userName = args[2];

            //Create blank board
            model = new ClientModel();

            // Create the network connection.
            serverConn = new NetworkClient(host, port, model, userName);
            go();
        }
    }
}


