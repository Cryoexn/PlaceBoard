package place.client.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import place.PlaceColor;
import place.PlaceTile;
import place.model.ClientModel;
import place.model.Observer;
import place.network.NetworkClient;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Graphical user interface for changing tiles on a place board.
 *
 * @author David Pitoniak dhp6397@rit.edu
 */
public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {

    private final int HBOX_SIZE = 500 / 16;

    private ClientModel model;
    private NetworkClient serverConn;
    private String userName;
    protected PlaceColor currentColor;
    private SimpleDateFormat formatter;
    private int GRID_SIZE;
    private GridPane gridPane;
    private BorderPane borderPane;
    private HBox hBox;

    /**
     * Initialization phase
     */
    public void init(){
        try{
            this.gridPane = new GridPane();
            this.borderPane = new BorderPane();
            this.hBox = new HBox();
            List< String > args = getParameters().getRaw();

            // Get host info from command line
            String host = args.get( 0 );
            int port = Integer.parseInt(args.get( 1 ));
            this.userName = args.get(2);
            this.currentColor = PlaceColor.BLACK;
            this.formatter = new SimpleDateFormat("dd/MM/yyyy\nHH:mm:ss");

            //Create blank board
            this.model = new ClientModel();

            // Create the network connection.
            this.serverConn = new NetworkClient(host, port, this.model, this.userName);

            GRID_SIZE = 500 / this.model.getDIM();
        }catch(ArrayIndexOutOfBoundsException |
                NumberFormatException e ) {
            System.out.println( e );
            throw new RuntimeException( e );
        }
    }

    /**
     * creating and starting the GUI components.
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        initGrid();
        initHBox();
        this.borderPane.setCenter(this.gridPane);
        this.borderPane.setBottom(this.hBox);
        borderPane.setStyle("-fx-base: " + PlaceColor.BLACK.getName());
        Scene scene = new Scene(borderPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Place: " + this.userName);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event ->{
            this.model.close();
        });
        this.model.addObserver(this);
        this.serverConn.startListener();
    }

    /**
     * Initializes the grid of colored tiles.
     */
    private void initGrid(){
        for(int row = 0; row < this.model.getDIM(); row++){
            for(int col = 0; col < this.model.getDIM(); col++){
                PlaceTile tempTile = this.model.getTile(row, col);
                Rectangle tempRect = new Rectangle(GRID_SIZE, GRID_SIZE, Paint.valueOf(tempTile.getColor().getName()));
                Tooltip initTip = new Tooltip("(" +  tempTile.getRow() + ", " + tempTile.getCol() + ")\n"+ tempTile.getOwner() + "\n" + formatter.format(tempTile.getTime()));
                initTip.setGraphic(new Rectangle(GRID_SIZE/2,GRID_SIZE/2, Paint.valueOf(tempTile.getColor().getName())));
                initTip.setShowDelay(Duration.millis(100));
                Tooltip.install(tempRect, initTip);
                this.gridPane.add(tempRect, col, row);
                tempRect.setOnMouseClicked(event -> {
                    try{
                        Rectangle rect = (Rectangle)event.getSource();
                        Date date = new Date();
                        PlaceTile tile = new PlaceTile(gridPane.getRowIndex(rect), gridPane.getColumnIndex(rect), this.userName, this.currentColor, date.getTime());
                        this.serverConn.sendMove(tile);
                    }catch(IOException e){
                        System.out.println(e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Initializes the color selection bar.
     */
    private void initHBox(){
        for(int i = 0; i < PlaceColor.TOTAL_COLORS; i++) {
            Button btn = new Button(PlaceColor.getPlaceColor(i).toString());
            btn.setMaxSize(HBOX_SIZE, HBOX_SIZE);
            btn.setMinSize(HBOX_SIZE, HBOX_SIZE);
            btn.setStyle("-fx-base: " + PlaceColor.getPlaceColor(i).getName());
            btn.setUserData(PlaceColor.getPlaceColor(i));
            hBox.getChildren().add(btn);
            btn.setOnMouseClicked(event -> {
                this.currentColor = (PlaceColor) ((Button) event.getSource()).getUserData();
            });
        }
        hBox.setAlignment(Pos.CENTER);
    }

    /**
     * Changes the tile that is passed in for the GUI.
     *
     * @param tile
     */
    private void updateTile(PlaceTile tile){
        Rectangle rect = getRect(tile);
        rect.setFill(Paint.valueOf(tile.getColor().getName()));
        Tooltip tip = new Tooltip("(" +  tile.getRow() + ", " + tile.getCol() + ")\n"+ tile.getOwner() + "\n" + formatter.format(new Date()));
        tip.setGraphic(new Rectangle(GRID_SIZE/2,GRID_SIZE/2, Paint.valueOf(tile.getColor().getName())));
        tip.setShowDelay(Duration.millis(100));
        Tooltip.install(rect, tip);
    }

    /**
     * Returns the rectangle at a certain tile.
     *
     * @param tile
     * @return
     */
    private Rectangle getRect(PlaceTile tile){
        for(Node node : gridPane.getChildren()){
            if(gridPane.getRowIndex(node) == tile.getRow() && gridPane.getColumnIndex(node) == tile.getCol()){
                return (Rectangle)node;
            }
        }
        return null;
    }

    /**
     * Used to modify the GUI based in user interaction.
     *
     * @param tile
     */
    private void refresh(PlaceTile tile){
        if(this.model.getStatus().equals("notDone") && tile != null) {
            updateTile(tile);
        }else if(this.model.getStatus().equals("notDone") && tile == null){
            this.model.close();
        } else{
            endGame();
        }
    }

    /**
     * Notify the waiting thread.
     */
    private synchronized void endGame(){
        this.notify();
    }

    /**
     * Close the NetworkClient.
     */
    @Override
    public void stop() {
        this.serverConn.close();
        System.exit(1);
    }

    /**
     * Called by the observer list.
     *
     * @param model
     * @param tile
     */
    @Override
    public void update(ClientModel model, PlaceTile tile) {
        if(Platform.isFxApplicationThread()){
            this.refresh(tile);
        }else{
            Platform.runLater(() ->{
                this.refresh(tile);
            });
        }
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
