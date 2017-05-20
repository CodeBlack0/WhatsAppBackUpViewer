/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextMessage extends Message {
    public static final Pattern PATTERN = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{2}, \\d{1,2}:\\d{2}:\\d{2} (?:AM|PM))(:) ([^:]+)(:) ((?:.|\n)+)");
    
    public TextMessage(String line) throws Exception {
        Matcher matcher = PATTERN.matcher(line);   
        if (matcher.find()) {
            this.timestamp = parse_date(matcher.group(1));
            this.actor = matcher.group(3);
            this.content = matcher.group(5);
        } else {
            throw new Exception("Failed to parse textmessage");
        }
    }
    
    public void append_message(String line) {
        this.content += line;
    }
}
