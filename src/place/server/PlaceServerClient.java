package place.server;

import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Class for the client thread on the PlaceServer.
 *
 * @author David Pitoniak dhp6397@rit.edu
 */
public class PlaceServerClient extends Thread implements Closeable {
    private String userName;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Socket socket;
    private PlaceServer server;
    private boolean connected;

    /**
     * Constructor sets the client socket and gets the input and output streams.
     *
     * @param socket
     * @param server
     */
    public PlaceServerClient(Socket socket, PlaceServer server){
        this.socket = socket;
        this.server = server;
        this.userName = null;
        try{
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
            connected = true;
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * Sets the username for the client.
     *
     * @param userName
     */
    public void setUserName(String userName){
        this.userName = userName;
    }

    /**
     * Run method for the thread that gets requests
     * from the client and processes them on the server
     */
    @Override
    public void run(){
        try{
            while(connected){
                try{
                    PlaceRequest<?> tempReq = getRequest();
                    if(tempReq.getType() == PlaceRequest.RequestType.CHANGE_TILE){
                        this.server.processRequest((PlaceRequest<PlaceTile>)tempReq);
                    }
                    sleep(500);
                }catch(NullPointerException e){
                    this.server.disconnectClient(this);
                    connected = false;
                    this.join();
                }

            }
        }catch (InterruptedException e){
            //squash.
        }
    }

    /**
     * Sends a request back to the client attached to the client socket.
     *
     * @param req
     */
    public void sendRequest(PlaceRequest<?> req){
        try{
            this.out.writeUnshared(req);
            this.out.flush();
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
    }

    /**
     * Gets requests from the client attached to the client socket.
     *
     * @return a place request from the client
     */
    public PlaceRequest<?> getRequest(){
        try{
            return (PlaceRequest<?>)this.in.readUnshared();
        }catch (ClassNotFoundException | IOException e){
            System.out.println(e.getMessage());
        }
        return null;
    }

    /**
     * gets the username from the client thread
     *
     * @return username
     */
    public String getUserName(){
        return this.userName;
    }

    /**
     * Gets the ip of the client socket
     *
     * @return ip string
     */
    public String getIp(){
        return this.socket.getRemoteSocketAddress().toString().split(":")[0];
    }

    /**
     * closes the socket to the user.
     */
    @Override
    public void close(){
        try{
            this.socket.close();
        }catch (IOException e){
            //squash
        }
    }

    /**
     * string representation of PlaceServerClient
     *
     * @return
     */
    public String toString(){
        return this.socket.toString();
    }
}
