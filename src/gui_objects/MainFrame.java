package gui_objects;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import gui_objects.MainFrame.Interrupter;
import javafx.beans.binding.DoubleBinding;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import message.Message;
import whatsappbackupviewer.MainWAH;

public class MainFrame {
	private Scene scene;
	private List<Message> messages;
	
	private Button nextSearchRsltBtn;
	private Button prevSearchRsltBtn;
	
	private MessagesPane scrPne ;
	
	private VBox mainLayout = new VBox();
	
	private final FancyWaitingBar waitBar;
	private final AtomicBoolean isDownloading = new AtomicBoolean(false);
	
	public MainFrame(Stage primaryStage, List<Message> messages){
		this.messages = messages;
		
		mainLayout.setBackground(new Background(new BackgroundFill(Color.web("#000000"), CornerRadii.EMPTY, Insets.EMPTY)));
				
		scene = new Scene(mainLayout, 800, 600);
		
		ToolBar toolBar = getToolBar();

		scrPne = new MessagesPane(messages, scene);
		
		waitBar = new FancyWaitingBar(20, 20);
		waitBar.setRectanglesHeight(20);
		waitBar.setArcFactor(0d);
		
		scene.widthProperty().addListener(t->{
			waitBar.setWidth(scene.getWidth());
		});
		
		mainLayout.getChildren().add(toolBar);
//		vBox.getChildren().add(waitBar);
		mainLayout.getChildren().add(scrPne);
		
		primaryStage.setMinWidth(350);
		
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	public interface Interrupter{
		public void interrupted();
		public void finished(List<Message> newMessages);
		public void failed();
	}
	
	private static Button getButton(String pngPath){
		return getButton(pngPath, 0);
	}
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
			if ( lastSearchTxt.equals( seachTxtFld.getText()) ){
				scrPne.focusNextSearchResult();
			}else{
				lastSearchTxt = seachTxtFld.getText();
				SearchForText( lastSearchTxt );
			}
		});
		
		scene.widthProperty().addListener(t->{
			double width = scene.getWidth() * 0.5;
			if (width > 600)
				width = 600;
			if (width < 50)
				width = 50;

			seachTxtFld.setPrefWidth(width);
		});
		
				
		Button loupeBtn = getButton("pics/loupe_small.png");
		loupeBtn.setOnAction(t->{
			if ( lastSearchTxt.equals( seachTxtFld.getText()) ){
				scrPne.focusNextSearchResult();
			}else{
				lastSearchTxt = seachTxtFld.getText();
				SearchForText( lastSearchTxt );
			}
		});
		
		nextSearchRsltBtn = getButton("pics/next.png", 90);
		nextSearchRsltBtn.setOnAction(t->{
			scrPne.focusNextSearchResult();
		});
		prevSearchRsltBtn = getButton("pics/next.png", -90);
		prevSearchRsltBtn.setOnAction(t->{
			scrPne.focusPrevSearchResult();
		});
		
		Button loadDataBtn = getButton("pics/refresh_small.png");
		loadDataBtn.setOnAction(t->{
			mainLayout.getChildren().add(1, waitBar);
			isDownloading.set(true);
			waitBar.run();
			
			this.looooongTakingTestMethond(
//	    	MainWAH.loadDataFromServer( <- diese Methode ist die eigentliche download-methode die dann ablaufen muss!
	    			new Interrupter(){
						@Override
						public void interrupted() {
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
						}
						@Override
						public void finished(List<Message> newMessages){
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
							scrPne.messagesHaveChanged(newMessages);
						}
						@Override
						public void failed(){
							waitBar.interruptBar();
							mainLayout.getChildren().remove(waitBar);
							isDownloading.set(false);
						}
	    			});
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
	
	private void looooongTakingTestMethond(Interrupter interrupter){
		Task<List<Message>> tsk = new Task<List<Message>>(){
			@Override
			protected List<Message> call() throws Exception {
				Thread.sleep(10000);
				return null;
			}	
			
		};
		tsk.setOnSucceeded(t->{
			List<Message> loadedMessags = tsk.getValue();
			if (loadedMessags != null){
				interrupter.finished(messages);
			}else{
				interrupter.failed();
			}
		});
		tsk.setOnCancelled(t->{
			interrupter.failed();
		});
		tsk.setOnFailed(t->{
			interrupter.failed();
		});
		
		
		Thread thrd = new Thread(tsk);
		thrd.setDaemon(true);
		thrd.start();
	}
	
	private void SearchForText(String searchTxt){
		searchTxt = searchTxt.toLowerCase();
		List<Integer> treffer = new ArrayList<>();
		for(int i=0; i < messages.size(); i++){
			if (messages.get(i).get_content().toLowerCase().contains(searchTxt)){
				treffer.add(i);
			}
		}
		
		boolean disblSearBtns = treffer.size() == 0;
		nextSearchRsltBtn.setDisable(disblSearBtns);
		prevSearchRsltBtn.setDisable(disblSearBtns);
		
		System.out.printf("treffer: %s%n", treffer);
		
		scrPne.setSearchResults(treffer);
	}
}
