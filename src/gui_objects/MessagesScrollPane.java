package gui_objects;

import java.util.ArrayList;
import java.util.List;

import gui_objects.MessagesPane.ScrBarValueSetter;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import message.Message;

public class MessagesScrollPane extends Pane {
	private List<Message> messages;
	private List<MessageTD> curDipsMsgs = new ArrayList<>();
		
	private List<Integer> searchedMsgs = new ArrayList<>();
	private int curSearchIndx = 0;
	
	private List<Dim> messageDims = new ArrayList<>();
	
	private int firstDispMsgIndx = 0;
	
	private Scene scene;
	private Group scrPneGrp = new Group();
	
	private double maxWidth, maxHeight;
	
	private double lastMouseDragPosY = -1;
	
	private ScrBarValueSetter scrBarValSetter;
	
	private class Dim{
		private double width, height;
		private Dim(double width, double height){
			this.width = width;
			this.height = height;
		}
	}
	
	public MessagesScrollPane(List<Message> messages, final Scene scene, final ScrBarValueSetter scrBarValSetter){
		super();
		this.messages = messages;
		this.scene = scene;
		this.scrBarValSetter = scrBarValSetter;
		
		this.setOnScroll(t->{
			scrBarValSetter.setValue( t.getDeltaY() > 0 ? 1 : -1 );
		});
		// key up && down funktioniert noch nicht, da die toolbar staendig den fokus fuer sich beansrpucht...
//		this.setOnKeyPressed(t->{
//			if (t.getCode() == KeyCode.UP){
//				scroll(-1);
//			}else if (t.getCode() == KeyCode.DOWN){
//				scroll(1);
//			}
//		});
		this.setOnMousePressed(t->{
			lastMouseDragPosY = t.getSceneY();
		});
		this.setOnMouseDragged(t->{
			double sceneY = t.getSceneY();
			double diff = sceneY - lastMouseDragPosY;
			if (Math.abs(diff) > 10 ){
				scrBarValSetter.setValueOffset( diff < 0 ? 1 : -1 );
				lastMouseDragPosY = sceneY;
			}
		});
		this.setOnMouseExited(t->{
			scene.setCursor(Cursor.DEFAULT);
		});
		evalMessageDimensions();
		
		this.getChildren().add(scrPneGrp);
		
		this.heightProperty().addListener(t->{
			renderMessages();
		});
		
		renderMessages();
	}
	
	private void evalMessageDimensions(){
		if (messages != null && messageDims != null){
			maxWidth = 0;
			maxHeight = 0;
			messageDims.clear();
			for(int i=0; i < messages.size(); i++){
				MessageTD msgTD = new MessageTD(messages.get(i), null);
				messageDims.add( new Dim(msgTD.getBoundsInLocal().getWidth(),
										 msgTD.getBoundsInLocal().getHeight()));
				maxHeight += msgTD.getBoundsInLocal().getHeight();
				double curWidth = msgTD.getBoundsInLocal().getWidth();
				if (curWidth > maxWidth){
					maxWidth = curWidth;
				}
				msgTD.close();
			}
		}
		
	}
//	private int getMessageVvalID(double absVvalue){
//		double yOffs = 0;
//		for(int i=0; i < messageDims.size(); i++){
//			if (yOffs >= absVvalue){
//				return i;
//			}
//			yOffs += messageDims.get(i).height;
//		}
//		return -1;
//	}
	
	public void setVValueBD(int msgId){
//		double startHeight = vValue * (maxHeight-this.getHeight());
		
//		int msgId = getMessageVvalID(startHeight);
		if (messages != null &&
				msgId > -1 && msgId < messages.size()){
			firstDispMsgIndx = msgId;
			renderMessages();
		}
	}
	
	private void renderMessages(){
		int insets = 10;
		int yOffs = insets;
		int xOffs = 20;
				
		closeCurrDispMsgs();
		curDipsMsgs.clear();
		scrPneGrp.getChildren().clear();
		
		if (messages != null){
			for(int i=firstDispMsgIndx; i < messages.size(); i++){
				MessageTD msgTD = new MessageTD(messages.get(i), cursor->{
					scene.setCursor(cursor);
				});
				
				curDipsMsgs.add(msgTD);
				
				msgTD.setLayoutX(xOffs);
				msgTD.setLayoutY(yOffs);
				scrPneGrp.getChildren().add(msgTD);
				yOffs += msgTD.getBoundsInLocal().getHeight() + insets;
				
				if (yOffs > this.getHeight()){
					break;
				}
			}
		}
	}
	public void scroll(int msgOffs){
		focusMessage(firstDispMsgIndx + msgOffs);
	}
	public void focusMessage(int msgId){
		if (messages != null && messages.size() > 0){
			if (msgId > messages.size()){
				msgId = messages.size() -1;
			}
			if (msgId < 0){
				msgId = 0;
			}
			if (msgId != firstDispMsgIndx){
				firstDispMsgIndx = msgId;
				renderMessages();
			}
		}
	}
	public void focusMessage(Message msg){
		if (messages != null && msg != null){
			int newMsgIndx = messages.indexOf(msg);
			if (newMsgIndx > -1){
				focusMessage(newMsgIndx);
			}
		}
	}
	
	private void closeCurrDispMsgs(){
		for(int i=0; i < curDipsMsgs.size(); i++){
			curDipsMsgs.get(i).close();
		}
	}
	
	public void setSearchResults(List<Integer> treffer){
		this.searchedMsgs = treffer;
		curSearchIndx = 0;
		focusSearch();
	}
	
	public void focusNextSearchResult(){
		if (searchedMsgs != null && searchedMsgs.size() > 0){
			if (++curSearchIndx >= searchedMsgs.size()){
				curSearchIndx = 0;
			}
			focusSearch();
		}
	}
	public void focusPrevSearchResult(){
		if (searchedMsgs != null && searchedMsgs.size() > 0){
			if (--curSearchIndx < 0){
				curSearchIndx = searchedMsgs.size() -1;
			}
			focusSearch();
		}
	}
	private void focusSearch(){
		if (curSearchIndx >= 0 && curSearchIndx < searchedMsgs.size()
				&& searchedMsgs.get(curSearchIndx) > 0
				&& searchedMsgs.get(curSearchIndx) < messages.size()){
			focusMessage( messages.get( searchedMsgs.get(curSearchIndx) ) );
			scrBarValSetter.setValue( searchedMsgs.get(curSearchIndx) );
		}
	}
	
	public void close(){
		messageDims = null;
		messages = null;
		curDipsMsgs = null;
		searchedMsgs = null;
		closeCurrDispMsgs();
		scrPneGrp.getChildren().clear();
		scene = null;
		scrBarValSetter = null;
	}
}
