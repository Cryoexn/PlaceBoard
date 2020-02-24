package place.network;

import place.PlaceBoard;
import place.PlaceTile;
import place.model.ClientModel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.NoSuchElementException;

import static java.lang.Thread.sleep;

/**
 * The client side network interface to a Reversi game server.
 * Each of the two players in a game gets its own connection to the server.
 * This class represents the controller part of a model-view-controller
 * triumvirate, in that part of its purpose is to forward user actions
 * to the remote server.
 *
 * @author David Pitoniak dhp6397@rit.edu
 * @author Robert St Jacques @ RIT SE
 * @author Sean Strout @ RIT CS
 * @author James Heliotis @ RIT CS
 */
public class NetworkClient {

    private Socket sock;
    private ObjectInputStream networkIn;
    private ObjectOutputStream networkOut;
    private ClientModel board;
    private String userName;
    private boolean go;

    /**
     * Accessor that takes multithreaded access into account
     *
     * @return whether it ok to continue or not
     */
    private synchronized boolean goodToGo() {
        return this.go;
    }

    /**
     * Multithread-safe mutator
     */
    private synchronized void stop() {
        this.go = false;
    }

    /**
     * Hook up with a Reversi game server already running and waiting for
     * two players to connect. Because of the nature of the server
     * protocol, this constructor actually blocks waiting for the first
     * message from the server that tells it how big the board will be.
     * Afterwards a thread that listens for server messages and forwards
     * them to the game object is started.
     *
     * @param hostname the name of the host running the server program
     * @param port     the port of the server socket on which the server is
     *                 listening
     * @param model    the local object holding the state of the game that
     *                 must be updated upon receiving server messages
     */
    public NetworkClient( String hostname, int port, ClientModel model, String userName){
        try {
            this.sock = new Socket(hostname, port);
            this.networkIn  = new ObjectInputStream(sock.getInputStream());
            this.networkOut = new ObjectOutputStream(sock.getOutputStream());
            this.board = model;
            this.userName = userName;
            this.go = true;

            //Client requesting to log into server.
            PlaceRequest<String> outReq = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, this.userName);
            this.networkOut.writeUnshared(outReq);
            this.networkOut.flush();

            //Receiving server response to login request.
            PlaceRequest<?> inReq = (PlaceRequest<?>) this.networkIn.readUnshared();
            if (inReq.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS) {
                PlaceBoard pb = (PlaceBoard)inReq.getData();
                this.board.allocate(pb);
            }else{
                System.out.println(inReq.getData());
            }
        }
        catch(IOException | ClassNotFoundException e ) {
            System.out.println(e.getMessage());
        }
    }

    public void startListener() {
        // Run rest of client in separate thread.
        // This threads stops on its own at the end of the game and
        // does not need to rendezvous with other software components.
        Thread netThread = new Thread( () -> this.run() );
        netThread.start();
    }

    /**
     * Called when the server sends a message saying that
     * gameplay is damaged. Ends the game.
     *
     * @param arguments The error message sent from the reversi.server.
     */
    public void error( String arguments ) {
        this.stop();
    }

    /**
     * This method should be called at the end of the game to
     * close the client connection.
     */
    public void close() {
        sock = null;
    }

    /**
     * UI wants to send a new move to the server.
     * @param tile tile being sent to server.
     */
    public void sendMove(PlaceTile tile) throws IOException {
        PlaceRequest<PlaceTile> outReq = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);
        this.networkOut.writeUnshared(outReq);
        this.networkOut.flush();
    }

    /**
     * Run the main client loop. Intended to be started as a separate
     * thread internally. This method is made private so that no one
     * outside will call it or try to start a thread on it.
     */
    private void run() {
        while (this.goodToGo()) {
            try {
                if(this.board.getStatus().equals("notDone")){
                    //Receiving server response to tile change request. (Should be a tile).
                    PlaceRequest<?> inReq = (PlaceRequest<?>) this.networkIn.readUnshared();
                    if (inReq.getType() == PlaceRequest.RequestType.TILE_CHANGED) {
                        PlaceTile tempTile = (PlaceTile) inReq.getData();
                        sleep(500);
                        //Update the board model.
                        this.board.changeTile(tempTile);
                    }else{
                        this.stop();
                    }
                }
                else{
                    this.stop();
                }
            }catch( NoSuchElementException nse ) {
                // Looks like the connection shut down.
                this.error( "Lost connection to server." );
                this.stop();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            catch (Exception e) {
                this.error(e.getMessage() + '?');
                this.stop();
            }
        }
        this.close();
    }
}