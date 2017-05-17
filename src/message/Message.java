/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package message;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Baseclass for Messages
 * @author timfi
 */
public abstract class Message {
    private Pattern date_pattern = Pattern.compile("^(\\d{2})\\.(\\d{2})\\.(\\d{2})\\, (\\d{1,2})\\:(\\d{2})\\:(\\d{2}) (AM|PM)");
    private int timezone_offset = -1;
    protected String actor;
    protected long timestamp;
    protected String content;
    
    public String get_actor() { return this.actor; }
    public long get_timestamp() { return this.timestamp; }
    public String get_content() { return this.content; }
    
    /**
     * Convert a timestamp from string form to milliseconds since Epoch
     * 
     * @param S given timestamp in string form
     * @return the milliseconds since Epoch
     * @throws Exception if the given string doesn't match the known pattern
     */
    protected long parse_date(String S) throws Exception {
        Matcher matcher = date_pattern.matcher(S);
        // temporary variables to store the results
        String Y, M, D ,h, m, s;
        // if the matcher found the results extract the data
        if (matcher.find()) {
            Y = matcher.group(3);
            M = matcher.group(2);
            D = matcher.group(1);
            // converts from 12 to 24 hour format and adjusts for the timezone differnce to UTC
            h = ("PM".equals(matcher.group(7)) ? 
                    Integer.toString(Integer.parseInt(matcher.group(4)) + 12 + timezone_offset) :
                    Integer.toString(Integer.parseInt(matcher.group(4)) +  - timezone_offset ));
            m = matcher.group(5);
            s = matcher.group(6);
        // else trow an exception so everyone knows some one fucked with the timestamps
        } else {
            throw new Exception("Illegal timestamp format: " + S);
        }
        // formats the values to something Instant.parse can read (YYYY-MM-DDThh:mm:ssZ)
        String temp_timestamp = "20" + Y + "-" + M + "-" + D + "T" + (h.length() < 2 ? "0" + h : h) + ":" + m + ":" + s + "Z";
        
        return Instant.parse(temp_timestamp).toEpochMilli();
    }
    
    /**
     * Checks if the given line matches the standard pattern for textmessages 
     * 
     * @param line line to check
     * @return 
     */
    public static boolean isText(String line) {
        Matcher matcher = TextMessage.PATTERN.matcher(line);        
        return matcher.matches() & !isAttachment(line);
    }
    
    /**
     * Checks if the given line matches the standard pattern for servermessages 
     * 
     * @param line line to check
     * @return 
     */    
    public static boolean isServerevent(String line) {
        Matcher matcher = ServerMessage.PATTERN.matcher(line);
        return matcher.matches();
    }
    
    /**
     * Checks if the given line matches the standard pattern for attachmentmessages 
     * 
     * @param line line to check
     * @return 
     */
    public static boolean isAttachment(String line) {
        Matcher matcher = AttachmentMessage.PATTERN.matcher(line);
        return matcher.matches();
    }
}
