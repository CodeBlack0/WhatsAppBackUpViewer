package gui_objects;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.filechooser.FileSystemView;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import message.Message;
import message.TextMessage;
import message.AttachmentMessage;
import message.ServerMessage;

public class MessageTD extends Group{
	private Message message;
	private CursorSetter cursorSetter;
	
	public static interface CursorSetter{
		public void setCursor(Cursor cursor);
	}
	
	private String getStringDate(long dateL){
		Date date = new Date(dateL);
		SimpleDateFormat df2 = new SimpleDateFormat("dd.MM.yy HH:mm:ss");
        return df2.format(date);
	}
	public MessageTD(Message message, CursorSetter cursorSetter){
		super();
		this.message = message;
		if (message != null){
		
			if (message.getClass() == AttachmentMessage.class){
				String att = ((AttachmentMessage)message).get_content();
				
//	//			System.out.printf("att: %s	endsWith(.jpg): %s%n", att, att.endsWith(".jpg"));
//				
//	//			if (att.contains("attached")){
//	//				StringBuilder strB1 = new StringBuilder();
//	//				StringBuilder strB2 = new StringBuilder();
//	//				for(int i=0; i < att.length(); i++){
//	//					strB1.append(String.format("	%s	", (int)att.charAt(i)));
//	//					strB2.append("	" + att.charAt(i) + "	");
//	//				}
//	//				System.out.printf("strB1: %s%n",  strB1);
//	//				System.out.printf("strB2: %s%n",  strB2);
//	//				System.out.println();
//	//			}
				
				String actor = message.get_actor();
		        String dateStr = getStringDate(message.get_timestamp());
				Label lbl;
				if (actor != null)
					lbl = new Label(String.format("Am: %s | von: %s%n%s", dateStr,
																  actor,
																  att));
				else{
					lbl = new Label(String.format("Am: %s%n%s", dateStr,
							  								 att));
				}
				
				final File file = new File(String.format("data/%s", att));
				Node node; 
				if (att.endsWith(".jpg") || att.endsWith(".png")){
									
					Image img = new Image(String.format("file:data/%s", att));
					ImageView imgVw = new ImageView(img);
					
					double maxImgWdth = 200;
					double maxImgHght = 200;
					double facX = img.getWidth()  / maxImgWdth;
					double facY = img.getHeight() / maxImgHght;
					
					double imgHght = img.getHeight();
					if (facX > 1d || facY > 1d){
						double scaleFactor = 1 / (facX > facY ?  facX : facY);
						
						imgHght *= scaleFactor;
						
						Scale scle = new Scale();
						scle.setX(scaleFactor);
						scle.setY(scaleFactor);
						imgVw.getTransforms().add(scle);
	//					imgVw.setScaleX(scaleFactor);
	//					imgVw.setScaleY(scaleFactor);
					}
					
					lbl.setLayoutY(imgHght);
					lbl.setFont(Font.font(10));
					Rectangle offsRct = new Rectangle(0,0, lbl.getWidth(), 30);
					offsRct.setFill(Color.TRANSPARENT);
					offsRct.setLayoutY(lbl.getLayoutY()+lbl.getBoundsInParent().getHeight());
					
					Group imgGrp = new Group();
					imgGrp.getChildren().addAll(imgVw, offsRct, lbl);
					node = imgGrp;
				}else{
					Rectangle attRct = new Rectangle();
					attRct.setWidth(300);
					attRct.setHeight(100);
					
					attRct.setFill(Color.CYAN);
									
					final ImageView imgVw = new ImageView();
					
					final Group iconGrp = new Group();
					
					
					lbl.setLayoutY(attRct.getHeight()-30);
					lbl.setFont(Font.font(10));
					lbl.setWrapText(true);
					lbl.setMaxWidth(attRct.getWidth());
					
					iconGrp.getChildren().addAll(attRct, lbl);
					
					node = iconGrp;
					
					Runnable fetchIcon = () -> {
		             // Windows {
		                FileSystemView view = FileSystemView.getFileSystemView();
		                javax.swing.Icon icon = view.getSystemIcon(file);
		                // }
	
		                // OS X {
		                //final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		                //javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
		                // }
		                
		                if (icon != null){
			                BufferedImage bufferedImage = new BufferedImage(
			                    icon.getIconWidth(), 
			                    icon.getIconHeight(), 
			                    BufferedImage.TYPE_INT_ARGB
			                );
			                icon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		
			                Platform.runLater(() -> {
			                    Image fxImage = SwingFXUtils.toFXImage(
			                        bufferedImage, null
			                    );
			                    imgVw.setImage(fxImage);
			                    imgVw.setLayoutX( (attRct.getWidth() - imgVw.getImage().getWidth()) * 0.5 );
			                    imgVw.setLayoutY( (attRct.getHeight() - imgVw.getImage().getHeight()) * 0.5 );
			                    iconGrp.getChildren().add(imgVw);
			                });
		                }
			        };
	
			        javax.swing.SwingUtilities.invokeLater(fetchIcon);   
					
				}
				
				Tooltip toolT = new Tooltip(att);
				Tooltip.install(node, toolT);
				
				node.setOnMouseEntered(t->{
					if (cursorSetter != null){
						cursorSetter.setCursor(Cursor.HAND);
					}
				});
				node.setOnMouseExited(t->{
					if (cursorSetter != null){
						cursorSetter.setCursor(Cursor.DEFAULT);
					}
				});
				node.setOnMouseClicked(t->{
					if (file != null){
						File tarFile = file;
	
						File dataFold = new File("data");
						int maxDist = Integer.MAX_VALUE;
						for (File curFle: dataFold.listFiles()){
							if (!file.exists()){
								int curDist = LevenshteinDistance.computeLevenshteinDistance(file.getName(), curFle.getName());
								if (curDist < maxDist){
									maxDist = curDist;
									tarFile = curFle;
								}
							}
						}
						
						final File tarFleFnl = tarFile;
						
						if (tarFile.exists()){
							new Thread(new Task<Void>(){
								@Override
								protected Void call() throws Exception {
									Desktop.getDesktop().open( tarFleFnl );
									return null;
								}
							}).start();
						}
					}
				});
				
				this.getChildren().addAll(node);
				
			}else{
				Rectangle backgrRct = new Rectangle();
				
				Text txtArea = new Text();
				if (message.getClass() == TextMessage.class){
					backgrRct.setFill(new Color(0.7d, 1.0d, 0.7d, 1.0d));//Color.LIME);
					txtArea.setText( ((TextMessage)message).get_content() );
				}else if(message.getClass() == ServerMessage.class){
					backgrRct.setFill(new Color(1.0d, 0.9d, 0.9d, 1.0d));
					txtArea.setText( ((ServerMessage)message).get_content() );
				}
							
	//			txtArea.setEditable(false);
	//			txtArea.setMaxWidth(500);
	//			txtArea.setWrapText(true);
				txtArea.setWrappingWidth(500);
				
				double insets = 5d;
						
				backgrRct.setWidth(  txtArea.getBoundsInLocal().getWidth()  + 2d*insets);
	//			backgrRct.setHeight( txtArea.getBoundsInLocal().getHeight() + 2d*insets);
				backgrRct.setArcWidth(20);
				backgrRct.setArcHeight(20);
				
				
				String actor = message.get_actor();
		        String dateStr = getStringDate(message.get_timestamp());
		        Text lbl;
				if (actor != null)
					lbl = new Text(String.format("Am: %s | von: %s", dateStr,
																  actor));
				else{
					lbl = new Text(String.format("Am: %s", dateStr));
				}
				
				lbl.setFont(Font.font(11));
				
				double yOffs = insets;
				
				lbl.setLayoutY( yOffs + lbl.getBaselineOffset() );
				
				yOffs = lbl.getBoundsInLocal().getHeight() + 5;
				
				Line line = new Line(0, yOffs,
									 backgrRct.getWidth(), yOffs);
				
				lbl.setFill(new Color(0d,0d,0d, 0.5d));
				line.setStroke(Color.LIGHTGRAY);
				
				yOffs += 4;
				
				txtArea.setLayoutX(insets);
				txtArea.setLayoutY( yOffs + txtArea.getBaselineOffset() );
				
				yOffs += txtArea.getBoundsInLocal().getHeight() + insets;
				
				backgrRct.setHeight( yOffs );
				
				this.getChildren().addAll(backgrRct, lbl, line, txtArea);
			}
		}
	}
	
	public void close(){
		message = null;
		cursorSetter = null;
	}
	
	public static class LevenshteinDistance {                                               
	    private static int minimum(int a, int b, int c) {                            
	        return Math.min(Math.min(a, b), c);                                      
	    }                                                                            
	                                                                                 
	    public static int computeLevenshteinDistance(CharSequence lhs, CharSequence rhs) {      
	        int[][] distance = new int[lhs.length() + 1][rhs.length() + 1];        
	                                                                                 
	        for (int i = 0; i <= lhs.length(); i++)                                 
	            distance[i][0] = i;                                                  
	        for (int j = 1; j <= rhs.length(); j++)                                 
	            distance[0][j] = j;                                                  
	                                                                                 
	        for (int i = 1; i <= lhs.length(); i++)                                 
	            for (int j = 1; j <= rhs.length(); j++)                             
	                distance[i][j] = minimum(                                        
	                        distance[i - 1][j] + 1,                                  
	                        distance[i][j - 1] + 1,                                  
	                        distance[i - 1][j - 1] + ((lhs.charAt(i - 1) == rhs.charAt(j - 1)) ? 0 : 1));
	                                                                                 
	        return distance[lhs.length()][rhs.length()];                           
	    }                                                                            
	}

}
