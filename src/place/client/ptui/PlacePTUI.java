package place.client.ptui;

import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;
import place.network.NetworkClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

/**
 * Plain Text User Interface.
 *
 * @author David Pitoniak dhp6397@rit.edu
 */
public class PlacePTUI extends ConsoleApplication implements Observer<ClientModel, PlaceTile> {

    private ClientModel model;
    private NetworkClient serverConn;
    private Scanner userIn;
    private PrintWriter userOut;
    private String userName;

    /**
     * Setup the model and connection
     */
    @Override
    public void init(){
        try {
            List< String > args = super.getArguments();

            // Get host info from command line
            String host = args.get( 0 );
            int port = Integer.parseInt(args.get( 1 ));
            this.userName = args.get(2);

            //Create blank board
            this.model = new ClientModel();

            // Create the network connection.
            this.serverConn = new NetworkClient(host, port, this.model, this.userName);
        }
        catch(ArrayIndexOutOfBoundsException |
                NumberFormatException e ) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }

    /**
     * Keeps the thread waiting while the game is not over.
     *
     * @param consoleIn  the source of the user input
     * @param consoleOut the destination where text output should be printed
     */
    @Override
    public synchronized void go(Scanner consoleIn, PrintWriter consoleOut) {
        this.userIn = consoleIn;
        this.userOut = consoleOut;

        // Connect UI to model. Can't do it sooner because streams not set up.
        this.model.addObserver( this );

        // Start the network listener thread
        this.serverConn.startListener();

        // Manually force a display of all board state, since it's too late
        // to trigger update().
        this.refresh(this.userName);
        while (this.model.getStatus().equals("notDone")) {
            try {
                this.wait();
            }
            catch( InterruptedException ie ) {}
        }
    }

    /**
     * Notifys waiting threads.
     */
    private synchronized void endConnection(){
        this.notify();
    }

    /**
     * Get user input and change the corresponding tile on the board.
     *
     * @param user
     */
    private void refresh(String user){
        boolean done = false;
        do {
            try{
                if(user == null) {
                    this.endConnection();
                    this.serverConn.close();
                    done = true;
                }else if(user.equals(this.userName)){
                    System.out.println(this.model);
                    this.userOut.print("type move as row column (int)color: ");
                    this.userOut.flush();
                    int row = this.userIn.nextInt();
                    if (row != -1) {
                        int col = this.userIn.nextInt();
                        String color = this.userIn.nextLine();
                        PlaceColor tempColor = PlaceColor.getPlaceColor(Integer.parseInt(color.strip()));
                        PlaceTile tempTile = new PlaceTile(row, col, this.userName, tempColor);
                        if (this.model.isValidMove(tempTile)) {
                            //Client requesting to change tile.
                            this.serverConn.sendMove(tempTile);
                            done = true;
                        }
                    } else {
                        this.model.close();
                        done = true;
                    }
                } else{
                    System.out.println(this.model);
                    this.refresh(this.userName);
                    done = true;
                }
            }catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } while (!done);
    }

    /**
     * Called by the observer list.
     *
     * @param model
     * @param tile
     */
    @Override
    public void update(ClientModel model, PlaceTile tile) {
        if(tile == null){
            this.refresh(null);
        }else{
            this.model = model;
            this.refresh(tile.getOwner());
        }
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceClient host port username");
            System.exit(0);
        }else{
            ConsoleApplication.launch(PlacePTUI.class, args);
        }
    }
}
