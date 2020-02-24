package place.model;

import place.PlaceBoard;
import place.PlaceTile;

import java.util.LinkedList;
import java.util.List;

/**
 * The client side model that is used as the "M" in the MVC paradigm.  All client
 * side applications (PTUI, GUI, bots) are observers of this model.
 *
 * @author David Pitoniak dhp6397@rit.edu
 * @author Sean Strout @ RIT CS
 */
public class ClientModel {
    //the actual board that holds the tiles
    private PlaceBoard board;

    //Model status
    private String status;

    //observers of the model (PlacePTUI and PlaceGUI - the "views")
    private List<Observer<ClientModel, PlaceTile>> observers = new LinkedList<>();

    /**
     * Add a new observer.
     *
     * @param observer the new observer
     */
    public void addObserver(Observer<ClientModel, PlaceTile> observer) {
        this.observers.add(observer);
    }

    public void allocate(PlaceBoard board){
        this.board = board;
        this.status = "notDone";
    }

    public PlaceTile getTile(int row, int col){
        return this.board.getTile(row, col);
    }

    /**
     * Get the square dimension of the board
     *
     * @return number of rows
     */
    public int getDIM() {
        return this.board.DIM;
    }

    /**
     * The UI calls this to announce the player's choice of a move.
     */
    public void changeTile(PlaceTile tile) {
        this.board.setTile(tile);
        notifyObservers(tile);
    }

    /**
     * Returns if the model is still running.
     *
     * @return
     */
    public String getStatus(){
        return this.status;
    }

    /**
     * changes the status and notifies the observers.
     */
    public void close(){
        this.status = "done";
        this.notifyObservers(null);
    }

    /**
     * Will this move be accepted as valid by the server?
     * This method is added so that a bad move is caught before it is sent
     * to the server, and the server quits.
     *
     * @param tile tile being validated
     * @return true iff the chosen square is adjacent to an occupied square
     */
    public boolean isValidMove(PlaceTile tile) {
        int row = tile.getRow();
        int col = tile.getCol();
        return ( row >= 0 && row < this.board.DIM ) &&
                        ( col >= 0 && col < this.board.DIM );
    }

    /**
     * Notify observers the model has changed.
     */
    private void notifyObservers(PlaceTile tile){
        for (Observer<ClientModel, PlaceTile> observer: observers) {
            observer.update(this, tile);
        }
    }

    public String toString(){
        return this.board.toString();
    }
}
