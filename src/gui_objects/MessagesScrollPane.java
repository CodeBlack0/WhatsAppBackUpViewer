package gui_objects;

import java.util.ArrayList;
import java.util.List;

import gui_objects.MessagesPane.ScrBarValueSetter;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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

    private double lastMouseDragPosY = -1;
    
    private double maxWidth, maxHeight;
    
    double getMaxHeightBD(){return maxHeight;}
    
    private int insets = 10;
    
    private int curFrstDispMsg = 0;
    private int curLastDispMsg = 1;
    
    private int curLayoutY = 0;

    private ScrBarValueSetter scrBarValSetter;

    private class Dim{
        private double width, height, layoutY;
        private Dim(){}
        private Dim(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    private int backgrImgId = 1;
    private ImageView backgrImgVw;
    
    void messagesHaveChanged(List<Message> newMessages){
    	this.messages = messages;
    	evaluateDimensions();
    	render(true);
    }

    public MessagesScrollPane(List<Message> messages, final Scene scene, final ScrBarValueSetter scrBarValSetter){
        super();
        
        clipChildren(this);

        this.setBackground(new Background(new BackgroundFill(Color.web("#FFFFFF"), CornerRadii.EMPTY, Insets.EMPTY)));
        this.messages = messages;
        this.scene = scene;
        this.scrBarValSetter = scrBarValSetter;
        
        evaluateDimensions();

        this.setOnScroll(t->{
        	scrBarValSetter.setValueOffset( -(int)t.getDeltaY() ); 
    	});
        
        this.setOnMousePressed(t->{ lastMouseDragPosY = t.getSceneY(); });
        this.setOnMouseDragged(t->{
            double sceneY = t.getSceneY();
            double diff = sceneY - lastMouseDragPosY;
            scrBarValSetter.setValueOffset( -(int)diff );
            lastMouseDragPosY = sceneY;
        });
        this.setOnMouseExited(t->{ scene.setCursor(Cursor.DEFAULT); });

        backgrImgVw = new ImageView(new Image("file:pics/backgrnd1.jpg"));
        this.widthProperty().addListener(t->{ properBackgroundID();render(true); });
        this.heightProperty().addListener(t->{ properBackgroundID();render(true);  });

        this.heightProperty().addListener(t->{ evaluateDimensions(); });
        this.widthProperty().addListener(t->{ evaluateDimensions(); });
        
        this.getChildren().addAll( this.backgrImgVw, scrPneGrp );
        
        render();
    }
    
    private void evaluateDimensions(){
    	double width = this.getWidth() - 50;
    	double yOffs = insets;
    	messageDims.clear();
    	for(int i=0; i < messages.size(); i++){
    		MessageTD msgTD = new MessageTD(messages.get(i), null, width);
		
	        msgTD.setLayoutX(insets);
	        msgTD.setLayoutY(yOffs);
	        
	        Dim dim = new Dim();
	        dim.height = msgTD.getBoundsInLocal().getHeight();
	        dim.width  = msgTD.getBoundsInLocal().getWidth();
	        dim.layoutY = yOffs;
	        
	        messageDims.add(dim);
	        
	        yOffs += msgTD.getBoundsInLocal().getHeight() + insets;
    	}
    	if (messageDims.size() > 0){
    		maxHeight = messageDims.get(messageDims.size()-1).layoutY + messageDims.get(messageDims.size()-1).height + 100;
    	}else{
    		maxHeight = 0d;
    	}
    	
    	scrBarValSetter.setMaxValue(maxHeight);
    }

    private void properBackgroundID(){
        int xID = 0, yID = 0;
        
        if (scene.getWidth() > 1800) xID = 3;
        else if (scene.getWidth() > 1200) xID = 2;
        else xID = 1;
        
        if (scene.getHeight() > 1490) yID = 3;
        else if (scene.getHeight() > 990) yID = 2;
        else yID = 1;

        int id = Math.max(xID, yID);

        if (backgrImgId != id){
            backgrImgId = id;
            backgrImgVw.setImage(new Image("file:pics/backgrnd" + id + ".jpg"));
        }
    }
    private MessageTD createMessage(int id, double width){
    	return new MessageTD(messages.get(id), cursor->{
			            scene.setCursor(cursor);
			    }, width);
    }
    
    public static void clipChildren(Region region) {

        final Rectangle outputClip = new Rectangle();
        region.setClip(outputClip);

        region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
            outputClip.setWidth(newValue.getWidth());
            outputClip.setHeight(newValue.getHeight());
        });        
    }
    
    private void render(boolean repaintAnyway){
    	double width = this.getWidth() - 50;
    	
    	double yPuffer = 2000;
    	    	
    	double yStart = scrBarValSetter.getValue();
    	double yEnd = yStart + this.getHeight();
    	
    	scrPneGrp.setLayoutY(-curLayoutY);
    	

    	if (yStart < messageDims.get(curFrstDispMsg).layoutY ||
    			yEnd > messageDims.get(curLastDispMsg).layoutY ||
    			repaintAnyway){
    		    		    		
    		for(int i=0; i < scrPneGrp.getChildren().size(); i++){
    			((MessageTD)scrPneGrp.getChildren().get(i)).close();
    		}
    		scrPneGrp.getChildren().clear();
    		
	    	int startId = 0;
	    	for(int i=0; i < messageDims.size(); i++){
	    		if (messageDims.get(i).layoutY > yStart-yPuffer){
	    			startId = i-1 < 0 ? 0 : i-1;
	    			break;
	    		}
	    	}
	    	
	    	curFrstDispMsg = startId;
	    	curLastDispMsg = messages.size()-1;
	    	
	    	double yOffs = messageDims.get(startId).layoutY;
	    	
	    	for(int i=startId == 0 ? 1 : startId; i < messages.size()-1; i++){
	    		MessageTD msgTD = new MessageTD(messages.get(i), cursor->{
		                scene.setCursor(cursor);
		        }, width);
	    		
		        msgTD.setLayoutX(insets);
		        msgTD.setLayoutY(yOffs);
		        
		        scrPneGrp.getChildren().add(msgTD);
		        	        
		        yOffs += msgTD.getBoundsInLocal().getHeight() + insets;
		        
		        if (yOffs > yEnd + yPuffer){
		        	curLastDispMsg = i;
		        	break;
		        }
	    	}
    	}
    }
    
    private void render(){
    	render(false);
    }

    public void setVValueBD(int layYPos){
    	curLayoutY = layYPos;
    	render();
//        if (messages != null && msgId > -1 && msgId < messages.size()){
//            firstDispMsgIndx = msgId;
//            renderMessages();
//        }
    }

//    private void renderMessages(){
//        int insets = 10;
//        int yOffs = insets;
//        int xOffs = 20;
//
//        closeCurrDispMsgs();
//        curDipsMsgs.clear();
//        scrPneGrp.getChildren().clear();
//
//        if (messages != null){
//            for(int i=firstDispMsgIndx; i < messages.size(); i++){
//                MessageTD msgTD = new MessageTD(messages.get(i), cursor->{
//                        scene.setCursor(cursor);
//                }, this.getWidth()-50);
//
//                curDipsMsgs.add(msgTD);
//
//                msgTD.setLayoutX(xOffs);
//                msgTD.setLayoutY(yOffs);
//                scrPneGrp.getChildren().add(msgTD);
//                yOffs += msgTD.getBoundsInLocal().getHeight() + insets;
//
//                if (yOffs > this.getHeight()) break;
//            }
//        }
//    }
    public void scroll(int msgOffs){
        focusMessage(firstDispMsgIndx + msgOffs);
    }
    public void focusMessage(int msgId){
//        if (messages != null && messages.size() > 0){
//            if (msgId > messages.size()) msgId = messages.size() -1;
//            if (msgId < 0) msgId = 0;
//            if (msgId != firstDispMsgIndx) {
//                firstDispMsgIndx = msgId;
//                renderMessages();
//            }
//        }
    }
//    public void focusMessage(Message msg){
//        if (messages != null && msg != null){
//            int newMsgIndx = messages.indexOf(msg);
//            if (newMsgIndx > -1) focusMessage(newMsgIndx);
//        }
//    }

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
            if (++curSearchIndx >= searchedMsgs.size()) curSearchIndx = 0;
            focusSearch();
        }
    }
    public void focusPrevSearchResult(){
        if (searchedMsgs != null && searchedMsgs.size() > 0){
                if (--curSearchIndx < 0) curSearchIndx = searchedMsgs.size() -1;
                focusSearch();
        }
    }
    private void focusSearch(){
        if (curSearchIndx >= 0 
                && curSearchIndx < searchedMsgs.size()
                && searchedMsgs.get(curSearchIndx) > 0
                && searchedMsgs.get(curSearchIndx) < messages.size()) {
//            focusMessage( messages.get( searchedMsgs.get(curSearchIndx) ) );
        	double y = messageDims.get(searchedMsgs.get(curSearchIndx)).layoutY;
            scrBarValSetter.setValue( (int)y );
        }
    }

    public void close() {
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
