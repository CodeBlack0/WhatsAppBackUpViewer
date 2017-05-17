package gui_objects;

import java.util.List;

import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import message.Message;

public class MessagesPane extends GridPane{
//	private List<Message> messages;
	private MessagesScrollPane scrPne;
	
	interface ScrBarValueSetter{
		void setValue(int val);
		void setValueOffset(int offs);
	}
	
	public MessagesPane(List<Message> messages, Scene scene){
		super();
		
//		this.messages = messages;
		
		ScrollBar scrBr = new ScrollBar();
		scrBr.setMin(0);
		scrBr.setMax(messages.size());
		scrBr.setOrientation(Orientation.VERTICAL);
		
		scrPne = new MessagesScrollPane(messages, scene, 
				new ScrBarValueSetter(){
					@Override
					public void setValue(int val) {
						scrBr.setValue( val );
					}
					@Override
					public void setValueOffset(int offs) {
						scrBr.setValue( (int)scrBr.getValue() +offs );
					}
				}
		);
		
		scrBr.valueProperty().addListener(t->{
			scrPne.setVValueBD((int)scrBr.getValue());
		});
		
		this.setOnMouseMoved(t->{
			Platform.runLater(new Runnable() {
		        @Override
		        public void run() {
		        	MessagesPane.this.requestFocus();
		        }
		    });
		});
		
		this.add(scrPne, 0, 0);
		this.add(scrBr, 1, 0);
		
		DoubleBinding heightDB = new DoubleBinding(){
			{
				super.bind(MessagesPane.this.heightProperty());
			}
			@Override
			protected double computeValue() {
				return MessagesPane.this.getHeight();
			}
			
		};
		
		RowConstraints rowConstr = new RowConstraints();
		rowConstr.prefHeightProperty().bind(heightDB);
		
		ColumnConstraints ScrBarColConstr = new ColumnConstraints(20);
		DoubleBinding scrPneColDB = new DoubleBinding(){
			{
				super.bind(MessagesPane.this.widthProperty());
			}
			@Override
			protected double computeValue() {
				return MessagesPane.this.getWidth() - ScrBarColConstr.getPrefWidth();
			}
		};
		ColumnConstraints ScrPneColConstr = new ColumnConstraints();
		ScrPneColConstr.prefWidthProperty().bind(scrPneColDB);
		
		this.getRowConstraints().add(rowConstr);
		this.getColumnConstraints().addAll(ScrPneColConstr, ScrBarColConstr);
	}
	
	public void setSearchResults(List<Integer> treffer){
		scrPne.setSearchResults(treffer);
	}
	public void focusNextSearchResult(){
		scrPne.focusNextSearchResult();
	}
	public void focusPrevSearchResult(){
		scrPne.focusPrevSearchResult();
	}
	public void close(){
//		messages = null;
		scrPne.close();
		scrPne = null;
		this.getChildren().clear();
	}
}
