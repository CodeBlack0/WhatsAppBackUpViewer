package whatsappbackupviewer;

import message.TextMessage;
import message.Message;
import message.AttachmentMessage;
import message.ServerMessage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class WhatsAppBackUpViewer extends Application{
	
    public static Scene scene;	
    private static List<Message> messages;

    public static void main(String[] args) {
        launch(args);
    }		

    @Override
    public void start(Stage primaryStage) throws Exception {
        messages = getMessages("G:\\files\\Downloads\\WhatsApp Chat - Informatik SS 2017 (15.05.17)\\_chat.txt");
        new gui_objects.MainFrame(primaryStage, messages);
    }

    public static Message process_line(String line) {
        try {
            // if the message fits the server message regex make servermessageOBJ
            if (Message.isServerevent(line)) {
            	System.out.printf("isServerevent: %s%n", line);
                return new ServerMessage(line);                
            }
            // if the message fits the attachment message regex make attachmentmessageOBJ
            else if (Message.isAttachment(line)) {
            	System.out.printf("isAttachment: %s%n", line);
                return new AttachmentMessage(line);
            } 
            // if the message fits the text message regex make textmessageOBJ
            else if (Message.isText(line)) {
            	System.out.printf("isText: %s%n", line);
                return new TextMessage(line);
            } else {
            	System.err.printf("not a message: %s%n", line);
//              throw new Exception("Message [" + line + "] doesn't conform to known patterns.");
            }           
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
	private static List<Message> getMessages(String filePath){//String f){

            // 1. read from file structure: 
            // filePath == filepath of _chat.txt-file
            try(BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))){
                return readChatTxtFromBfReader(reader);
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }

            // 2. read from Zip:
            // filePath == filepath to Zip-File
//		try (ZipFile zf = new ZipFile( zipFilePath )){
//			for ( Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ){
//			  ZipEntry entry = e.nextElement();
//		//	  System.out.println( entry.getName() );
//			  
//			  if (entry.getName().equals("_chat.txt")){
//				  InputStream is = zf.getInputStream( entry );
//				  
//				  try(BufferedReader reader = new BufferedReader(new InputStreamReader(is))){					  
//					  return readChatTxtFromBfReader(reader);
//				  }
//			  }
//			}
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			return null;
//		}

            return null;
	}
        
	private static List<Message> readChatTxtFromBfReader(BufferedReader reader) throws IOException{
		List<Message> messages = new ArrayList<>();
                Message msg = null;
                String line;
                int counter = 0;
                while((line = reader.readLine()) != null){
                    if (counter++ > 100) break;
                    line = processString(line);
                    if (lineIsAMessage(line)){
                        msg = process_line(line);
                        if (msg == null){
                            System.err.printf("msg-error! something went wrong at line: %s%n", counter);
                        }
                        messages.add(msg);
                    }else{
                        // falls die derzeitige zeile keine message ist, kann sie (nach derzeitigem verstaendnis)
                        // nur zusaetzlicher text des vorangeganegnen message-objekts sein -> damit muss das vorangegangene 
                        // message objekt vom typ TextMessage sein:
                        if (msg != null && msg.getClass() == TextMessage.class){
                            TextMessage txtMsg = (TextMessage)msg;
                            txtMsg.append_message(System.lineSeparator() + line);
                        }else{
                            System.err.printf("Derzeitige line ist keine message, aber zuvorige line was auch keine TextMessage -> was also ist diese zeile???"
                                            + "%n		%s%n", line);
                        }
                    }
                }
                return messages;
	}
	
	// checkt, ob eine zeile der anfang einer message ist (textmessage, servermessage etc.)
	private static boolean lineIsAMessage(String line){
            return Message.isServerevent(line) || Message.isAttachment(line) || Message.isText(line);
	}
	
	// in dem WhatsApp-backup-text-file ("_chat.txt") stecken einige schraege characters drin, dieser wird sich hier
	// entledigt:
	private static String processString(String str){
            return str.replaceAll(String.format("%s", (char)160), " ")
                      .replaceAll(String.format("%s", (char)8234), "")
                      .replaceAll(String.format("%s", (char)8236), "")
                      .replaceAll(String.format("%s", (char)8206), "");
	}

}
