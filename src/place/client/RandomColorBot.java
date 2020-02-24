package place.client;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.network.NetworkClient;
import java.io.IOException;
import java.util.Random;

import static java.lang.Thread.sleep;


public class RandomColorBot {

    private static String userName;
    private static ClientModel model;
    private static NetworkClient serverConn;

    public static void go() {
        Random rand = new Random();

        while (true) {
            try {
                int movex = rand.nextInt(model.getDIM());
                int movey = rand.nextInt(model.getDIM());
                PlaceTile tempTile = new PlaceTile(movex, movey, userName, PlaceColor.getPlaceColor(rand.nextInt(10)+4));
                System.out.println(tempTile);
                serverConn.sendMove(tempTile);
                sleep(2000);
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

