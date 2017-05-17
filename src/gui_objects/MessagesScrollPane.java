package gui_objects;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Pane;
import message.Message;

public class MessagesScrollPane extends Pane {
	List<Message> messages;
	List<MessageTD> curDipsMsgs = new ArrayList<>();
	
	List<Integer> searchedMsgs = new ArrayList<>();
	int curSearchIndx = 0;
	
	List<Dim> messageDims = new ArrayList<>();
	
	double maxWidth, maxHeight;
	
	private class Dim{
		private double width, height;
		private Dim(double width, double height){
			this.width = width;
			this.height = height;
		}
	}
	
	private int firstDispMsgIndx = 0;
	private final Scene scene;
	private Group scrPneGrp = new Group();
	
	public MessagesScrollPane(List<Message> messages, final Scene scene){
		super();
		this.messages = messages;
		this.scene = scene;
		
		evalMessageDimensions();
		
//		this.setVbarPolicy(ScrollBarPolicy.NEVER);

		this.getChildren().add(scrPneGrp);
//		this.setContent(scrPneGrp);
		
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
		
		System.out.printf("evalMessageDimensions: maxWidth: %s	maxHeight: %s%n", maxWidth, maxHeight);
	}
	private int getMessageVvalID(double absVvalue){
		double yOffs = 0;
		for(int i=0; i < messageDims.size(); i++){
			if (yOffs >= absVvalue){
				return i;
			}
			yOffs += messageDims.get(i).height;
		}
		return -1;
	}
	
	public void setVValueBD(double vValue){
		double startHeight = vValue * (maxHeight-this.getHeight());
		
		int msgId = getMessageVvalID(startHeight);
		if (msgId > -1){
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
	
	public void focusMessage(Message msg){
		int newMsgIndx = -1;
		for(int i=0; i < messages.size(); i++){
			if (msg.equals(messages.get(i))){
				newMsgIndx = i;
				break;
			}
		}
		if (newMsgIndx > -1 && newMsgIndx != firstDispMsgIndx){
			firstDispMsgIndx = newMsgIndx;
			renderMessages();
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
	private void focusSearch(){
		if (curSearchIndx >= 0 && curSearchIndx < searchedMsgs.size()
				&& searchedMsgs.get(curSearchIndx) > 0
				&& searchedMsgs.get(curSearchIndx) < messages.size()){
			focusMessage( messages.get( searchedMsgs.get(curSearchIndx) ) );
			curSearchIndx += 1;
		}
	}
	
	public void close(){
		messageDims = null;
		messages = null;
		curDipsMsgs = null;
		closeCurrDispMsgs();
		scrPneGrp.getChildren().clear();
	}
}
