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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Holds methods for converting classes to XIO-tag-names and vice versa.<br>
 * For example java.lang.String becomes <code>"sfstring"</code>.
 * <p>
 * Can also load new definitions from file (adding or replacing).<br>
 * If using dynamically loaded classes, it is possible to name an additional
 * class loader. If XIOMaps can't find a class, it will try to load it
 * with the additional class loader.
 * <p>
 * Example for a XIO codes file:
 * <p>
 * <code>
 * &lt;?xml version="1.0"?&gt;<br>
 * &lt;codes clear="true"&gt;<br>
 * &nbsp; &lt;def class="java.lang.String" code="sfstring"/&gt;<br>
 * &nbsp; &lt;def class="protobuf.MFVec3fProtos$MFVec3f" code="mfvec3f"/&gt;<br>
 * &lt;/codes&gt;<br>
 * </code>
 * <p>
 * In the above example, the <code>clear</code> attribute tells venice.lib to
 * remove all XIO codes before reading the new ones from file.
 */
public class XIOMaps {
	
	private static Logger logger = Logger.getLogger(XIOMaps.class);

    private static Map<String, Class<?>> str2classMap;
    private static Map<Class<?>, String> class2strMap;
    
    private static ClassLoader additionalClassLoader = null;
    
    static{
    	setDefaultValues();
    }
    
    public static void setDefaultValues(){
    	str2classMap = new HashMap<String, Class<?>>();
        class2strMap = new HashMap<Class<?>, String>();
        
        // put in default codes (can be removed with clear())
        putPair("sfstring", String.class);
        putPair("mfstring", String[].class);
        putPair("sffloat", Float.class);
        putPair("mffloat", Float[].class);
        putPair("sfdouble", Double.class);
        putPair("mfdouble", Double[].class);
        putPair("sfbool", Boolean.class);
        putPair("mfbool", Boolean[].class);
        putPair("sfint32", Integer.class);
        putPair("mfint32", Integer[].class);
    }
    
    /**
     * Private constructor. This class can not be instantiated.
     */
    private XIOMaps() {
    	// nothing
    }

    /**
     * Gets the map where XIO codes are mapped to classes.
     * @return the map where XIO codes are mapped to classes
     */
    public static Map<String, Class<?>> getStr2classMap() {
        return str2classMap;
    }

    /**
     * Gets the map where classes are mapped to XIO codes.
     * @return the map where classes are mapped to XIO codes
     */
    public static Map<Class<?>, String> getClass2strMap() {
        return class2strMap;
    }
    
    /**
     * Maps a XIO-code to a class.
     * 
     * @param code XIO code that has to be mapped to the given class
     * @param clazz The class to which the XIO code has to be matched
     */
    public static void putPair(String code, Class<?> clazz){
    	str2classMap.put(code, clazz);
    	class2strMap.put(clazz, code);
    }
    
    /**
     * Loads additional class-string-definitions from a XML file.
     * @param fileName
     */
    public static void loadXIOCodes(String fileName){
    	
    	if(fileName == null) return;
    	
    	File file = null;
    	file = new File(fileName);
    	if(file == null || ! file.exists() || ! file.isFile()){
    		logger.error("Can't load "+fileName);
    		return;
    	}
    	
    	DocumentBuilder builder = null;
    	try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		if(builder == null) return; // DocumentBuilder doesn't want to work
		
		Document document=null;
		try {
			document = builder.parse(file);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		if(document==null) return; // XML Document can't be parsed
		
		Element primeElement = document.getDocumentElement();
		String clear = findAttr("clear", primeElement);
		if(clear != null && clear.equals("true")){
			logger.debug("clearing old xio codes");
			clear();
		}
		
		NodeList defList = primeElement.getElementsByTagName("def");
		for(int iMatch=0; iMatch<defList.getLength(); iMatch++){
			Node def = defList.item(iMatch);
			String className = findAttr("class", def);
			String codeName = findAttr("code", def);
			if(className != null && codeName != null ){
				Class<?> clazz = null;
				try {
					clazz = Class.forName(className);
				} catch (ClassNotFoundException e) {
					if(additionalClassLoader != null){
						try {
							clazz = additionalClassLoader.loadClass(className);
						} catch (ClassNotFoundException e1) {
							// nothing
						}
					}
				}
				if(clazz == null){
					logger.error("Error: Class "+className+" doesn't exist.");
				}
				else{
					putPair(codeName, clazz);
				}
			}
		}
    }
    
    /**
     * Finds an attribute in the given XML element.
     * @param attr The attribute to look for
     * @param element The element where to look
     * @return The value of the attribute or <code>null</code>
     */
    private static String findAttr(String attr, Node element){
    	String name = null;
    	if(element.getAttributes() != null)
    		if(element.getAttributes().getNamedItem(attr) != null)
    			name = element.getAttributes().getNamedItem(attr).getNodeValue();
    	return name;
    }
    
    /**
     * Removes all code-class-pairs, so the maps are empty afterwards.
     */
    public static void clear(){
    	str2classMap.clear();
    	class2strMap.clear();
    }
    
    /**
     * Sets an additional class loader. Will be used if loading XIO codes
     * from file and the classname is not found via standard classloader.
     * This is used for RSB protobuf classes.
     * @param classLoader
     */
    public static void setAdditionalClassLoader(ClassLoader classLoader){
    	additionalClassLoader = classLoader;
    }

}
