package whatsappbackupviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import message.AttachmentMessage;
import message.Message;
import message.ServerMessage;
import message.TextMessage;

public class MainWAH extends Application{
	
	public static Scene scene;
	
	private static List<Message> messages;

	public static void main(String[] args) {
		
		launch(args);
	}
		
	
	@Override
	public void start(Stage primaryStage) throws Exception {
		messages = getMessages("data/_chat.txt");

		new gui_objects.MainFrame(primaryStage, messages);
	}

    public static Message process_line(String line) {
        try {
            // if the message fits the server message regex make servermessageOBJ
            if (Message.isServerevent(line)) {
//            	System.out.printf("isServerevent: %s%n", line);
                return new ServerMessage(line);                
            }
            // if the message fits the attachment message regex make attachmentmessageOBJ
            else if (Message.isAttachment(line)) {
//            	System.out.printf("isAttachment: %s%n", line);
                return new AttachmentMessage(line);
            } 
            // if the message fits the text message regex make textmessageOBJ
            else if (Message.isText(line)) {
//            	System.out.printf("isText: %s%n", line);
                return new TextMessage(line);
            } else {
                throw new Exception("Message [" + line + "] doesn't conform to known patterns.");
            }           
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
	private static List<Message> getMessages(String filePath){//String f){

		if (Files.exists(Paths.get(filePath))){
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
		}
				
		return null;
	}

	private static List<Message> readChatTxtFromBfReader(BufferedReader reader) throws IOException{
		List<Message> messages = new ArrayList<>();
		  
		  Message msg = null;
		  
		  String line;
		  
		  int counter = 0;
		  
		  while((line = reader.readLine()) != null){
			  
			  //if (counter++ > 100)break;
			  
			    line = processString(line);
			    			  						  						
				if ( lineIsAMessage(line) ){
					
					msg = process_line(line);
					
					if (msg == null){
						System.err.printf("msg-error! something went wrong at line: %s%n", counter);
					}
					messages.add( msg );
				}else{
					// falls die derzeitige zeile keine message ist, kann sie (nach derzeitigem verstaendnis)
					// nur zusaetzlicher text des vorangeganegnen message-objekts sein -> damit muss das vorangegangene 
					// message objekt vom typ TextMessage sein:
					if (msg != null && msg.getClass() == TextMessage.class){
						TextMessage txtMsg = (TextMessage)msg;
						txtMsg.append_message( System.lineSeparator() + line );
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
	
	
	
	//-------------------------------------------------------------------------------------------------------
	
	
	
	
	public static void loadDataFromServer(){
		
		new Thread(new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				String oldVersTmpFle = "res/version.txt";
				String newVersTmpFle = "temp/version.txt";
				String url = "https://www.dropbox.com/s/pv5xtkh6fz7f7cf/version.txt?dl=0";
				
				try{
					downloadFromDropbox(url, newVersTmpFle);
					
					File versFle = new File(newVersTmpFle);
					if (versFle.exists()){
						try(BufferedReader readerNew = new BufferedReader(new FileReader(versFle))){
							try(BufferedReader readerOld = new BufferedReader(new FileReader(oldVersTmpFle))){
								String versionNew = readerNew.readLine();
								String versionOld = readerOld.readLine();
								
								System.out.printf("versionNew: %s%n", versionNew);
			
								if ( !versionNew.equals(versionOld) ){
									Path from = Paths.get(newVersTmpFle); 
									Path to   = Paths.get(oldVersTmpFle); 
									Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
									Files.delete(from);
								}
								
								url = "https://www.dropbox.com/s/oijpgcle4615iq5/WAChatBackup.zip?dl=0";
								String zipFilePath = "res/data.zip";
								
								downloadFromDropbox(url, zipFilePath);
								
								List<String> oldFlNms = new ArrayList<>();
								Path path = Paths.get("data");
								try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
								  for (Path entry : stream){
									  oldFlNms.add(entry.getFileName().toString());
								  }
								  stream.close();
								}
								
								try (ZipFile zf = new ZipFile( zipFilePath )){
									for ( Enumeration<? extends ZipEntry> e = zf.entries(); e.hasMoreElements(); ){
									  ZipEntry entry = e.nextElement();
									  
									  if ( !oldFlNms.contains(entry.getName()) ){
			//							  if (entry.getName().equals("_chat.txt")){
			//								  
			//							  }else{
											  String fileSep = File.separator;
											  String tarFileName = String.format("data%s%s", fileSep, entry.getName());
											  InputStream is = zf.getInputStream( entry );
											  OutputStream os = new FileOutputStream(tarFileName, false);
											  writeFile(is, os);
			//							  }
									  }
									}
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							}
							
						}catch(FileNotFoundException e){
							e.printStackTrace();
						}catch(IOException e){
							e.printStackTrace();
						}
					}
				}catch(MalformedURLException e){ 
					return null;
				}catch(IOException e){
					return null;
				}
				return null;
			}	
		}).start();
	}
	private static void writeFile(InputStream is, OutputStream os) throws IOException {
	    byte[] buf = new byte[512]; // optimize the size of buffer to your need
	    int num;
	    while ((num = is.read(buf)) != -1) {
	      os.write(buf, 0, num);
	    }
	}
	private static void downloadFromDropbox(String url, String tarFile) throws MalformedURLException, FileNotFoundException, IOException{
	    URL download = new URL(url);
	    ReadableByteChannel rbc = Channels.newChannel(download.openStream());
	    FileOutputStream fileOut = new FileOutputStream(tarFile);
	    fileOut.getChannel().transferFrom(rbc, 0, 1 << 24);
	    fileOut.flush();
	    fileOut.close();
	    rbc.close();
	}

}
