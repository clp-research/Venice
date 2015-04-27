/*
 * Copyright (c) 2015 Dialog Systems Group, University of Bielefeld
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

import java.io.IOException;
import java.io.StringReader;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import venice.lib.Configuration;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

/**
 * A DOM-XML-Parser for XIO lines. It provides methods for parsing XIO lines
 * into events, parsing events into XIO lines, quick parsing of the timestamp
 * of an XIO line
 * 
 * @author Oliver Eickmeyer
 *
 */
public class XIODomParser extends XIOParser {
	
	static Logger logger;
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(XIODomParser.class);
	}
	
    private DOMParser domparser;
    
    public XIODomParser(){
    	domparser = new DOMParser();
    }
    
    /**
     * Let the DOM-parser parse the given XIO line and returns
     * the representing XML element.
     * @param xioLine The XIO line to be parsed.
     * @return A XML element representing the XIO line.
     */
    private Node getNode(String xioLine){
        String str = old2new(xioLine);
        
        InputSource is = new InputSource(new StringReader(str));
        
		try {
			domparser.parse(is);
		} catch (SAXException | IOException e) {
			//e.printStackTrace();
			return null;
		}
		
		Document document = domparser.getDocument();
		NodeList nl = document.getChildNodes();
        Node node = nl.item(0);
        
        return node;
    }

	@Override
	public long preparseTS(String xioLine) {
        long ts = INVALID_TIMESTAMP;

        Node node = getNode(xioLine);
        
        if(node == null) return ts;
        
        NamedNodeMap AttrMap = node.getAttributes();
        
        // process TIMESTAMP
        if(AttrMap.getNamedItem("timestamp") != null)
        	ts = Long.parseLong(AttrMap.getNamedItem("timestamp").getNodeValue());
                
    	return ts;
	}

	@Override
    public String eventToString(SlotEvent e) {
        String value = value2str(e.getValue());
        Long timestamp = e.getTime();
        String sensor = e.getScope();
        String type = XIOMaps.getClass2strMap().get(e.getType());
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
     * Converts a xio line from old into new format.
     * If the xio line is already in new format, nothing will be changed.
     * 
     * @param xioLine The xio line to be checked and possibly converted.
     * @return A xio line in new format.
     */
    public String old2new(String xioLine){
    	String str;
    	
        if(xioLine.startsWith("<irio:")){
        	// convert from old to new format
        	int p = xioLine.lastIndexOf('<');
        	str = '<' + xioLine.substring(6, p-1) + "/>";
        }
        else str = xioLine;
    	
        return str;
    }
    
	@Override
    public SlotEvent stringToEvent(String xioLine){
		String typeString;
    	Class<?> type = null;
    	String value;
        long ts;
        String sensor=null;
        SlotEvent se = new SlotEvent();
        
        Node node = getNode(xioLine);
        if(node == null) return failSlotEvent(xioLine);
        NamedNodeMap AttrMap = node.getAttributes();
        
        // process TYPE
        typeString = node.getNodeName(); // get data type of the value
        type = XIOMaps.getStr2classMap().get(typeString); // get Class
        if(type==null) type = String.class; // use String for unknown types
        //logger.debug("typestring "+typeString+" turned to "+type);
        se.setType(type);
        
        // process VALUE
        if(AttrMap.getNamedItem("value") != null){
        	value = AttrMap.getNamedItem("value").getNodeValue(); // read value as a string
        	setEventValue(se, value, type.getName()); // store value as given class type
        }
        else return failSlotEvent(xioLine);
        
        // process SENSORNAME
        if(AttrMap.getNamedItem("sensorname") != null){
        	sensor = AttrMap.getNamedItem("sensorname").getNodeValue();
        	se.setLabel(sensor);
        }
        else{
        	if(AttrMap.getNamedItem("sensorName") != null){
            	sensor = AttrMap.getNamedItem("sensorName").getNodeValue();
            	se.setLabel(findSlotLabel(sensor));
            	se.setNamespace(findNamespaceLabel(sensor));
            }
            else return failSlotEvent(xioLine);
        }
        
        // process TIMESTAMP
        if(AttrMap.getNamedItem("timestamp") != null){
        	ts = Long.parseLong(AttrMap.getNamedItem("timestamp").getNodeValue());
        	se.setTime(ts);
        }
        else return failSlotEvent(xioLine);
        
    	return se;
    }
    
    public String toString(){
    	return "DOM";
    }
}
