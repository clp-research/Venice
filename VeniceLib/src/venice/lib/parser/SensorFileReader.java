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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import venice.lib.AbstractSlot;

/**
 * Provides the possibility to parse a file with information about sensors,
 * namespaces and slots.
 * <p>
 * <b>Example 1</b>:
 * <p>
 * <code>
 * &lt;?xml version="1.0"?&gt;<br>
 * &lt;Sources&gt;<br>
 * &lt;Sensor name="Simulator"&gt;<br>
 * &nbsp; &lt;Namespace name="car"&gt;<br>
 * &nbsp; &nbsp; &lt;slot name="message" type="sfstring"/&gt;<br>
 * &nbsp; &nbsp; &lt;slot name="lane" type="sfint32"/&gt;<br>
 * &nbsp; &nbsp; &lt;slot name="speed" type="sffloat"/&gt;<br>
 * &nbsp; &lt;/Namespace&gt;<br>
 * &lt;/Sensor&gt;<br>
 * &lt;/Sources&gt;<br>
 * </code>
 * <p>
 * The names should never contain a leading or trailing slash
 * (even for RSB).
 * For RSB needed slashes will be added automatically, when creating
 * informers or handlers.
 * <p>
 * <b>Example 2 (brief style)</b>:
 * <p>
 * <code>
 * &lt;?xml version="1.0"?&gt;<br>
 * &lt;Sources&gt;<br>
 * &lt;Simulator&gt;<br>
 * &nbsp; &lt;car&gt;<br>
 * &nbsp; &nbsp; &lt;message type="sfstring"/&gt;<br>
 * &nbsp; &nbsp; &lt;lane type="sfint32"/&gt;<br>
 * &nbsp; &nbsp; &lt;speed type="sffloat"/&gt;<br>
 * &nbsp; &lt;/car&gt;<br>
 * &lt;/Simulator&gt;<br>
 * &lt;/Sources&gt;<br>
 * </code>
 * <p>
 * A XML-element with a 'type' attribute will be parsed as a slot.
 * All parent XML-elements of a slot will be parsed as namespace
 * (except for the topmost).<br>
 * If an element has a 'name' attribute, this be used for the slot
 * or the namespace. If it doesn't, the tag-name itself will be used.
 */
public class SensorFileReader {
	
	/**
	 * Constructor is not accessible.
	 */
	private SensorFileReader(){
	}
	
	/**
	 * Reads the content of the given file and parses it into an array of
	 * abstract slot definitions.<br>
	 * The file content needs to be in a XML-format see
	 * the class description for a short example.
	 * 
	 * @param file contains the XML-structure with slot-definitions
	 * @return an ArrayList with abstract slots
	 */
	public static ArrayList<AbstractSlot> parse(File file){
		ArrayList<AbstractSlot> slotList = new ArrayList<AbstractSlot>();
		
		DocumentBuilder builder = null;
    	try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		if(builder == null) return null; // DocumentBuilder doesn't want to work
		
		Document document=null;
		try {
			document = builder.parse(file);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		if(document==null) return null; // XML Document can't be parsed
		
		Element primeElement = document.getDocumentElement();

		// read all elements of the XML tree
		NodeList nodelist = primeElement.getElementsByTagName("*");
		
		// go through all elements
		for(int i=0; i<nodelist.getLength(); i++){
			Node node = nodelist.item(i); // element to check
			if(node.getNodeName() != null){
				// if it has a name, check if it has a type
				String typeString = findElementsAttr(node, "type"); 
				if(typeString != null){
					// if it has a type, it is assumed to be a slot
					
					// use name attribute or tag name as slot label
					String label = findElementsAttr(node, "name");
					if(label == null) label = node.getNodeName();
					
					String namespace = ""; // prepare namespace string
					Node parent = node.getParentNode(); // look for a parent element
					while(parent != null && !parent.isSameNode(primeElement)){
						// if there is a parent element, use it as a part of the namespace
						
						// use the name attribute or the tag name
						String parentName = findElementsAttr(parent, "name");
						if(parentName == null) parentName = parent.getNodeName();
						
						// if namespace is still empty, use parent name as namespace
						if(namespace.isEmpty()) namespace = parentName;
						// if namespace is not empty, put parent name in front of namespace
						else namespace = parentName + "/" + namespace;
						
						// check, if parent has also a parent
						parent = parent.getParentNode();
					}
					slotList.add(createAbstractSlot(namespace, label, typeString));
				}
			}
		}
		
		return slotList;
	}
	
	/**
	 * Creates a new AbstractSlot with given name for namespace, the label
	 * and the name of the type. The type will be parsed into the
	 * corresponding class. If the type can't be parsed into a class,
	 * <code>String</code> will be used as type and a warning will be printed
	 * out to System.err.
	 * @param namespace Name of the namespace
	 * @param label The label of the slot
	 * @param typeString The name of the type of the slot
	 * @return A new AbstractSlot with the given namespace, label and type
	 */
	private static AbstractSlot createAbstractSlot(String namespace, String label, String typeString){
		AbstractSlot as = new AbstractSlot();
		
		// set label of the slot
		as.setLabel(label);
		
		// set namespace of the slot
		as.setNamespace(namespace);
		
		// set type of the slot
		Class<?> type = null;
    	if(typeString != null) type = XIOMaps.getStr2classMap().get(typeString.toLowerCase());
    	if(type == null){
    		System.err.println("Can't find "+typeString+" - using String instead");
    		type = String.class;
    	}
    	as.setType(type);
		
		return as;
	}
	
    /**
     * Finds an Attribute and return its value, or null if not found.
     * @param element
     * @return Value of attr or <code>null</code> if there is no such Attribute.
     */
    private static String findElementsAttr(Node element, String attr){
    	String value = null;
    	if(element.getAttributes() != null)
    		if(element.getAttributes().getNamedItem(attr) != null)
    			value = element.getAttributes().getNamedItem(attr).getNodeValue();
    	return value;
    }
}
