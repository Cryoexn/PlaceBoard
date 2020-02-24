package place.client;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.network.NetworkClient;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.Thread.sleep;


public class PictureBot {

    private static String userName;
    private static ClientModel model;
    private static NetworkClient serverConn;
    private static ArrayList<PlaceTile> tiles;

    public static void go() {
        while (true) {
            try {
                for(PlaceTile t : tiles){
                    serverConn.sendMove(t);
                    sleep(2000);
                }
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
            tiles = new ArrayList<>();

            tiles.add(new PlaceTile(0, 0, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(0, 1, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(0, 2, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(0, 3, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(0, 4, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(1, 0, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(1, 1, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(1, 2, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(1, 3, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(1, 4, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(2, 0, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(2, 1, userName, PlaceColor.BLACK));
            tiles.add(new PlaceTile(2, 2, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(2, 3, userName, PlaceColor.BLACK));
            tiles.add(new PlaceTile(2, 4, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(3, 0, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(3, 1, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(3, 2, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(3, 3, userName, PlaceColor.YELLOW));
            tiles.add(new PlaceTile(3, 4, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(4, 0, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(4, 1, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(4, 2, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(4, 3, userName, PlaceColor.RED));
            tiles.add(new PlaceTile(4, 4, userName, PlaceColor.RED));

            // Create the network connection.
            serverConn = new NetworkClient(host, port, model, userName);
            go();
        }
    }
}

