package place.server;

import place.PlaceBoard;
import place.PlaceTile;
import place.network.PlaceRequest;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import static java.lang.Thread.sleep;

/**
 * The Place server is run on the command line as:
 *
 * $ java PlaceServer port DIM
 *
 * Where port is the port number of the host and DIM is the square dimension
 * of the board.
 *
 * @author David Pitoniak dhp6397@rit.edu
 */
public class PlaceServer implements Closeable {
    private static int MAX_CLIENTS = 10;
    private static int MAX_SAME_IP = 3;

    private static ArrayList<PlaceServerClient> clients;
    private static PlaceBoard board;
    private static ServerSocket server;

    /**
     * Constructor for the server.
     *
     * @param port
     * @param dim
     * @throws IOException
     */
    public PlaceServer(int port, int dim) throws IOException {
        this.server = new ServerSocket(port);
        System.out.println("Server Socket: " + this.server);
        clients = new ArrayList<>();
        this.board = new PlaceBoard(dim);
    }

    /**
     * Takes a client thread and gets the Login request
     * from the client and sends the success message back
     * to the client. Then adds the client to the list
     * of clients and starts the Thread. Only if the username
     * is unique and there are not too many clients on the
     * server or if there are too many people on the same ip address.
     *
     * @param client client logging in
     */
    private static void loginClient(PlaceServerClient client){
        PlaceRequest<?> inReq = client.getRequest();
        if(inReq.getType() == PlaceRequest.RequestType.LOGIN){
            if(!uniqueUserName((String)inReq.getData())){
                client.sendRequest(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "User Name Not Unique."));
            }else if(!uniqueIP(client.getIp())){
                client.sendRequest(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Too many users on ip."));
            }else if(clients.size() + 1 > MAX_CLIENTS){
                client.sendRequest(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Too many users logged in."));
            }else{
                //Tell client it was logged in successfully.
                PlaceRequest<PlaceBoard> outReq = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN_SUCCESS, board);
                client.sendRequest(outReq);
                client.setUserName((String)inReq.getData());
                clients.add(client);
                client.start();
            }
        }else{
            client.sendRequest(new PlaceRequest<>(PlaceRequest.RequestType.ERROR, "Invalid login request."));
        }
        System.out.println(client);
    }

    /**
     * Checks all the clients usernames to see
     * if the passed in username is unique.
     *
     * @param userName username being checked
     * @return if the username was unique
     */
    private static boolean uniqueUserName(String userName){
        for(PlaceServerClient c : clients){
            if(c.getUserName().equals(userName)){
                return false;
            }
        }
        return true;
    }

    /**
     * Removes the client from the list of clients.
     *
     * @param client client disconnecting
     */
    protected void disconnectClient(PlaceServerClient client){
        this.clients.remove(client);
    }

    /**
     * Takes a request and updates the board
     * accordingly and sends the other clients
     * the change request.
     *
     * @param req request being processed
     * @throws InterruptedException
     */
    protected synchronized void processRequest(PlaceRequest<PlaceTile> req) throws InterruptedException {
        if(req.getType() == PlaceRequest.RequestType.CHANGE_TILE){
            PlaceTile tempTile = req.getData();
            if(this.board.isValid(tempTile)){
                this.board.setTile(tempTile);
                PlaceRequest<PlaceTile> outReq = new PlaceRequest<>(PlaceRequest.RequestType.TILE_CHANGED, tempTile);
                sendClientsUpdate(outReq);
                sleep(500);
            }
        }
    }

    /**
     * Sends a Place Request to all the client in a list.
     *
     * @param req
     */
    private void sendClientsUpdate(PlaceRequest<?> req){
        for(PlaceServerClient c : clients){
            c.sendRequest(req);
        }
    }

    /**
     * Checks if the ip that the user connected
     * with is under the max number of the same ip address.
     *
     * @param ip ip being compared
     * @return
     */
    private static boolean uniqueIP(String ip){
        int numSameIp = 0;

        for(PlaceServerClient c : clients){
            if(c.getIp().split(":")[0].equals(ip)){
                numSameIp++;
            }
        }
        if(numSameIp >= MAX_SAME_IP){
            return false;
        }
        return true;
    }

    /**
     * The main method starts the server and spawns client threads each time a new
     * client connects.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java PlaceServer port DIM");
        }

        try(PlaceServer serverTest = new PlaceServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]))){
            while(true){
                Socket socket = server.accept();
                PlaceServerClient client = new PlaceServerClient(socket, serverTest);
                loginClient(client);
            }
        }catch(Exception e){
        }
    }

    /**
     * closes the server and joins the client threads.
     */
    @Override
    public void close(){
        try{
            this.server.close();
            joinClients();
        }catch(IOException e){

        }
    }

    /**
     * Join all the client threads.
     */
    private void joinClients(){
        try{
            for(PlaceServerClient c : clients){
                c.join();
            }
        }catch (InterruptedException e){
            System.out.println(e.getMessage());
        }
    }
}