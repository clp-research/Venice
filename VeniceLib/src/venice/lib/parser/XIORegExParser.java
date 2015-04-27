/*
 * Copyright (c) 2015 Dialogue Systems Group, University of Bielefeld
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package venice.lib.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import venice.lib.Configuration;

/**
 * Parses strings to slotEvents and vice versa. Uses regular expression matching.
 * Strings need to be in the format of a XIO line, for example:<br>
 * <code>&lt;sfstring value="hello world" timestamp="1394123456789" sensorname="messagebox"/&gt;</code>
 * <p>
 * The order of the attributes in the XIO line may not be changed.
 * 
 * @author Oliver Eickmeyer
 */
public class XIORegExParser extends XIOParser{
	
	static Logger logger;
	
	private Pattern patType, patValue, patTS, patSensor; // regular expression pattern for parsing xio lines
	private Pattern patPreparseTS; // reg. ex. for preparsing Timestamp
    private Matcher matcher;
    
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(XIORegExParser.class);
	}

    public XIORegExParser() {
		
        // the following patterns are predefined in this constructor,
        // because that speeds up parsing
        patType = Pattern.compile("\\s*[<\\s*|:](\\w+?)\\s"); // regular expression to parse type (including old format)
        patValue =  Pattern.compile("value\\s*=\\s*\"(.*?)\""); // regular expression to parse value
        patTS =  Pattern.compile("timestamp=\"(.*?)\""); // regular expression to parse timestamp
        patSensor =  Pattern.compile("sensor[N|n]ame\\s*=\\s*\"(.*?)\""); // regular expression to parse sensorName
        
        // special pattern for preparsing timestamp (for faster seeking)
        //patPreparseTS = Pattern.compile("[<\\s*|:](\\w+?)\\s+value=\"(.*)\"\\s+timestamp=\"(\\d*)\"\\s+sensorname=\"(.*)\"\\s*>");
        patPreparseTS = Pattern.compile("[<|<irio:](\\w+?)\\s+value=\"(.*)\"\\s+timestamp=\"(\\d*?)\"+\\s+sensor[N|n]ame=\"(.*?)\".*>");
    }
    
    public long preparseTS(String s){
    	long ts = INVALID_TIMESTAMP;
    	matcher = patPreparseTS.matcher(s);
    	if(matcher.find()){
    		ts = Long.parseLong(matcher.group(3));
    	}
    	return ts;
    }
    
    /**
     * Converts a SlotEvent into a string.
     * 
     * @param slotEvent SlotEvent object
     * @return String representation of the SlotEvent
     */
    public String eventToString(SlotEvent slotEvent) {
        String value = value2str(slotEvent.getValue());
        Long timestamp = slotEvent.getTime();
        String sensor = slotEvent.getScope();
        String type = XIOMaps.getClass2strMap().get(slotEvent.getType());
        if(type == null){
        	// if the type is not recognized, use string as default
        	// and look for special characters that have to be filtered
        	type = "sfstring";
        	value = filterSpecialCharacters(value);
        }
        String xioOutString = "<" + type + " value=\"" + value + "\" timestamp=\"" + timestamp.toString() + "\" sensorName=\"" + sensor + "\"/>";
        return xioOutString;
    }
    
    /**
     * Parse a single string into a type timed event (TTE), string has to be in
     * xml format:<br>
     * <code>&lt;type value="..." timestamp="..." sensorName="..."/&gt;</code><br>
     * or the old format : <code>&lt;irio:type ...&gt;&lt;/irio:type&gt;</code><br>
     * Examples:<br>
     * <code>&lt;sfint32 value="123" timestamp="1381234567890" sensorName="test"/&gt;</code><br>
     * <code>&lt;irio:sfint32 value="123" timestamp="1381234567890" sensorName="test"&gt;&lt;/irio:sfint32&gt;</code><br>
     * @param str The string that is to parsed.
     * @return Returns always a TTE object. In case of a failed parsing attempt, a fail TTE is created.
     * */
    public SlotEvent stringToEvent(String str){
    	String type=null;
    	Class<?> typeClass = null;
        long ts;
        String sensor=null;
        SlotEvent e = new SlotEvent();
        
        // parse type
    	matcher = patType.matcher(str);
    	if(matcher.find()){
    		type = matcher.group(1);
    		typeClass = XIOMaps.getStr2classMap().get(type); // Class for this datatype
            if(typeClass == null){
            	typeClass = String.class; // use String for every unknown datatype
            }
            //logger.debug("set type class for "+str+" to "+typeClass.getName());
            e.setType(typeClass);
    	}
    	else return failSlotEvent(str);
    	
    	// parse value
    	matcher = patValue.matcher(str);
        if(matcher.find()){
        	setEventValue(e, matcher.group(1), typeClass.getName());
        	//logger.debug("e.getValue()="+e.getValue());
        }
        else return failSlotEvent(str);
        
        // parse timestamp
        matcher = patTS.matcher(str);
        if(matcher.find()){
        	ts = Long.parseLong(matcher.group(1));
        	e.setTime(ts);
        }
        else return failSlotEvent(str);
        
        // parse sensorName
        matcher = patSensor.matcher(str);
        if(matcher.find()){
        	sensor = matcher.group(1);
        	e.setLabel(findSlotLabel(sensor));
        	e.setNamespace(findNamespaceLabel(sensor));
        }
        else return failSlotEvent(str);
        
    	return e;
    }
    
    public String toString(){
    	return "REGEX";
    }

}
