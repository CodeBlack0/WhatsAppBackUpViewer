package gui_objects;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import message.Message;

public class MainFrame {
    private Scene scene;
    private List<Message> messages;

    private Button nextSearchRsltBtn;
    private Button prevSearchRsltBtn;

    private MessagesPane scrPne ;

    public MainFrame(Stage primaryStage, List<Message> messages){
        this.messages = messages;

        GridPane vBox = new GridPane();

        scene = new Scene(vBox, 800, 600);

        ToolBar toolBar = getToolBar();

        scrPne = new MessagesPane(messages, scene);

        DoubleBinding scrPneDB = new DoubleBinding(){
            { super.bind(scene.heightProperty(), toolBar.heightProperty()); }
            @Override
            protected double computeValue() { return scene.getHeight() - toolBar.getHeight(); }
         };

        RowConstraints toolBarRowConstr = new RowConstraints(50);
        RowConstraints scrPneRowConstr = new RowConstraints();
        scrPneRowConstr.prefHeightProperty().bind(scrPneDB);

        DoubleBinding colDB = new DoubleBinding(){
            { super.bind(scene.widthProperty()); }
            @Override
            protected double computeValue() { return scene.getWidth(); }
        };

        ColumnConstraints colConstr = new ColumnConstraints();
        colConstr.prefWidthProperty().bind(colDB);

        vBox.getRowConstraints().addAll(toolBarRowConstr, scrPneRowConstr);
        vBox.getColumnConstraints().add(colConstr);

        vBox.add(toolBar, 0,0);
        vBox.add(scrPne , 0,1);

        primaryStage.setMinWidth(350);

        scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Button getButton(String pngPath){ return getButton(pngPath, 0); }
    
    private static Button getButton(String pngPath, int rotate){
            Image imageOk = new Image(String.format("file:%s", pngPath));
            ImageView imgVw = new ImageView(imageOk);
            imgVw.setRotate(rotate);
            return new Button("", imgVw);
    }
    private String lastSearchTxt = "";
    private ToolBar getToolBar(){
        ToolBar toolBar = new ToolBar();

        TextField seachTxtFld = new TextField("");
        seachTxtFld.setOnAction(t->{
            if ( lastSearchTxt.equals( seachTxtFld.getText()) ) scrPne.focusNextSearchResult();
            else {
                lastSearchTxt = seachTxtFld.getText();
                SearchForText( lastSearchTxt );
            }
        });

        scene.widthProperty().addListener(t->{
            double width = scene.getWidth() * 0.5;
            if (width > 600) width = 600;
            if (width < 50) width = 50;
            seachTxtFld.setPrefWidth(width);
        });


        Button loupeBtn = getButton("pics/loupe_small.png");
        loupeBtn.setOnAction(t->{
            if ( lastSearchTxt.equals( seachTxtFld.getText()) ) scrPne.focusNextSearchResult();
            else {
                lastSearchTxt = seachTxtFld.getText();
                SearchForText( lastSearchTxt );
            }
        });

        nextSearchRsltBtn = getButton("pics/next.png", 90);
        nextSearchRsltBtn.setOnAction(t->{  scrPne.focusNextSearchResult(); });
        prevSearchRsltBtn = getButton("pics/next.png", -90);
        prevSearchRsltBtn.setOnAction(t->{ scrPne.focusPrevSearchResult(); });

        Button loadDataBtn = getButton("pics/refresh_small.png");
        loadDataBtn.setOnAction(t->{
//          MainWAH.loadDataFromServer();
        });

        seachTxtFld.minHeightProperty().bind(loupeBtn.heightProperty());

        toolBar.getItems().addAll(
            seachTxtFld,
            loupeBtn,
            prevSearchRsltBtn,
            nextSearchRsltBtn,
            new Separator(Orientation.VERTICAL),
            loadDataBtn
        );
        return toolBar;
    }

    private void SearchForText(String searchTxt){
        searchTxt = searchTxt.toLowerCase();
        List<Integer> treffer = new ArrayList<>();
        for(int i=0; i < messages.size(); i++){
            if (messages.get(i).get_content().toLowerCase().contains(searchTxt)) treffer.add(i);
        }

        boolean disblSearBtns = treffer.size() == 0;
        nextSearchRsltBtn.setDisable(disblSearBtns);
        prevSearchRsltBtn.setDisable(disblSearBtns);

        scrPne.setSearchResults(treffer);
    }
}
