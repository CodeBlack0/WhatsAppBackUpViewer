package whatsappbackupviewer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;
import message.AttachmentMessage;
import message.Message;
import message.ServerMessage;
import message.TextMessage;

public class WhatsAppBackUpViewer extends Application {	
    public static Scene scene;
    private static List<Message> messages;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        loadDataFromServer();
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
    
    private static List<Message> getMessages(String filePath){
        if (Files.exists(Paths.get(filePath))){
            try(BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))){
                return readChatTxtFromBfReader(reader);
            }catch(FileNotFoundException e){
                e.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        return null;
    }

    private static List<Message> readChatTxtFromBfReader(BufferedReader reader) throws IOException{
        List<Message> messages = new ArrayList<>();

        Message msg = null;
        String line;
        int counter = 0;

        while((line = reader.readLine()) != null){
            counter++;
            line = processString(line);
            if ( lineIsAMessage(line) ){
                msg = process_line(line);
                if (msg == null) System.err.printf("msg-error! something went wrong at line: %s%n", counter);
                messages.add( msg );
            } else {
//              falls die derzeitige zeile keine message ist, kann sie (nach derzeitigem verstaendnis) 
//              nur zusaetzlicher text des vorangeganegnen message-objekts sein -> damit muss das vorangegangene 
//              message objekt vom typ TextMessage sein:
                if (msg != null && msg.getClass() == TextMessage.class){
                    TextMessage txtMsg = (TextMessage)msg;
                    txtMsg.append_message( System.lineSeparator() + line );
                } else System.err.printf("Derzeitige line ist keine message, aber zuvorige line was auch keine TextMessage -> was also ist diese zeile???%n		%s%n", line);
            }
        }
        return messages;
    }

    // checkt, ob eine zeile der anfang einer message ist (textmessage, servermessage etc.)
    private static boolean lineIsAMessage(String line) { return Message.isServerevent(line) || Message.isAttachment(line) || Message.isText(line); }

    // in dem WhatsApp-backup-text-file ("_chat.txt") stecken einige schraege characters drin, dieser wird sich hier
    // entledigt:
    private static String processString(String str) {
        return str.replaceAll(String.format("%s", (char)160), " ")
                  .replaceAll(String.format("%s", (char)8234), "")
                  .replaceAll(String.format("%s", (char)8236), "")
                  .replaceAll(String.format("%s", (char)8206), "");
    }
	
//-------------------------------------------------------------------------------------------------------

    public static void loadDataFromServer(){
        String oldVersTmpFle = "res/version.txt";
        String newVersTmpFle = "temp/version.txt";
        String zipFilePath = "res/data.zip";
        String version_url = "https://www.dropbox.com/s/pv5xtkh6fz7f7cf/version.txt?dl=1";
        String zip_url = "https://www.dropbox.com/s/oijpgcle4615iq5/WAChatBackup.zip?dl=1";
        String data_dir = "data";
        
        new Thread(new Task<Void>(){
            @Override
            protected Void call() throws Exception {
                try{
                    System.out.println("Getting new version file from: " + version_url);
                    downloadFromDropbox(version_url, newVersTmpFle);

                    // get version file
                    File versFle = new File(newVersTmpFle);
                    // if it exists...
                    if (versFle.exists()){
                        System.out.println("Got version, proceeding to checking it's content.");
                        // ...check if it's the newest version
                        String versionNew, versionOld;
                        try(BufferedReader readerNew = new BufferedReader(new FileReader(versFle))){
                            try(BufferedReader readerOld = new BufferedReader(new FileReader(oldVersTmpFle))){
                                versionNew = readerNew.readLine();
                                versionOld = readerOld.readLine();
                                System.out.printf("versionOld: %s%n", versionOld);
                                System.out.printf("versionNew: %s%n", versionNew);
                            } catch(FileNotFoundException e) { e.printStackTrace(); return null; } 
                              catch(IOException e) { e.printStackTrace(); return null; }
                        } catch(MalformedURLException e) { return null; }
                          catch(IOException e) { return null; }

                        if ( !versionNew.equals(versionOld) ){
                            System.out.println("Not newest version.");
                        } else {
                            System.out.println("Already have the newest version.");
                            return null;
                        }

                        // if it was not the newest version download and process the zip
                        System.out.println("Getting the newer zip from: " + zip_url);
                        downloadFromDropbox(zip_url, zipFilePath);
                        List<String> oldFlNms = new ArrayList<>();
                        Path path = Paths.get("data");
                        try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
                            for (Path entry : stream){
                                oldFlNms.add(entry.getFileName().toString());
                            }
                            stream.close();
                        }

                        System.out.println("Proceeding to unpacking process.");
                        File destDir = new File(data_dir);
                        if (!destDir.exists()) {
                            destDir.mkdir();
                        }
                        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
                        ZipEntry entry;
                        // iterates over entries in the zip file
                        while ((entry = zipIn.getNextEntry()) != null) {
                            String filePath = data_dir + File.separator + entry.getName();
                            if (!entry.isDirectory()) {
                                // if the entry is a file, extracts it
                                extractFile(zipIn, filePath);
                            } else {
                                // if the entry is a directory, make the directory
                                File dir = new File(filePath);
                                dir.mkdir();
                            }
                            zipIn.closeEntry();
                            entry = zipIn.getNextEntry();
                        }
                        zipIn.close();
                        System.out.println("Finnished unpacking.");

                        System.out.println("Saving newest version file.");
                        Path from = Paths.get(newVersTmpFle); 
                        Path to   = Paths.get(oldVersTmpFle); 
                        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
                        Files.delete(from);
                        System.out.println("Proceeding to launching GUI.");
                    }
                } catch(Exception e) { e.printStackTrace(); }
                return null;
            }
        }).start();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }

    private static void downloadFromDropbox(String url, String tarFile) throws MalformedURLException, FileNotFoundException, IOException{
        URL download = new URL(url);
        ReadableByteChannel rbc = Channels.newChannel(download.openStream());
        FileOutputStream fileOut = new FileOutputStream(tarFile);
        fileOut.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fileOut.flush();
        fileOut.close();
        rbc.close();
    }
}
