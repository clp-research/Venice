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

import java.util.Arrays;
import java.util.StringTokenizer;

import org.instantreality.InstantIO.Color;
import org.instantreality.InstantIO.ColorRGBA;
import org.instantreality.InstantIO.Matrix3f;
import org.instantreality.InstantIO.Matrix4f;
import org.instantreality.InstantIO.Rotation;
import org.instantreality.InstantIO.Vec2f;
import org.instantreality.InstantIO.Vec3f;

import venice.lib.networkRSB.ValueParser;

/**
 * Describes classes that provide XIO parsing.
 * Every subclass has to provide methods for parsing from string to event,
 * event to string and preparsing of the timestamp from a string.
 * The preparsing should be faster than the full
 * parsing, but it depends on the implementation of the subclass.
 * <p>
 * The strings have to be in the XIO line format.<br>
 * Example:<br>
 * <code>
 * &lt;sfstring value="hello" timestamp="123" sensorname="testslot"/&gt;
 * </code>
 * <p>
 * They should also be able to parse XIO line in old format.<br>
 * Example:<br>
 * <code>
 * &lt;irio:sfstring value="hello" timestamp="123" sensorname="testslot"&gt;&lt;/irio:sfstring&gt;
 * </code>
 * <p>
 * Subclasses can demand a specific order of the attributes of the XIO lines.
 * <p>
 * <code>XIOParser</code> provides a collection of useful static methods to
 * support parsing.
 * 
 * @author Oliver Eickmeyer
 */
public abstract class XIOParser {
	/**
	 * Defines an invalid timestamp.
	 * <p>
	 * For example, methods that parse timestamps into a <code>long</code>
	 * value should return this value, if parsing fails (if they do not
	 * throw an exception).
	 */
	public final static long INVALID_TIMESTAMP = -2147483648;
	
	/**
	 * Parses an event into a XIO line.
	 * @param e the event to be parsed
	 * @return the XIO line representing the given event, or <code>null</code> if parsing failed
	 */
    public String eventToString(SlotEvent e){
    	return null;
    }
    
    /**
     * Parses a XIO line into an event.
     * @param str the XIO line
     * @return the event reconstructed from the given XIO line, or <code>null</code> if parsing failed
     */
    public SlotEvent stringToEvent(String str){
    	return null;
    }

    /**
	 * Quickly parses only the timestamp of the given XIO line. 
	 * @param s the XIO line
	 * @return the timestamp or <code>INVALID_TIMESTAMP</code> if the timestamp
	 * could not be parsed
	 */
    public long preparseTS(String s){
    	return INVALID_TIMESTAMP;
    }
    
    /**
     * Filters or replaces special characters in a string.
     * 
     * @param s String to be filtered
     * @return filtered String
     */
    public static String filterSpecialCharacters(String s){
    	s = s.replaceAll("\n", "\\\\n"); // replace carriage return (13) with '\n'
    	return s;
    }
    
    /**
     * Finds and returns the label of the slot in a sensor name string.
     * Example: In the string <code>test/abc</code> the label of the slot
     * is <code>abc</code>.
     * <p>
     * To remember: The sensor name strings (for example in log files) are
     * composed of the namespace label and the slot label.
     * Example: <code>venice/test/abc</code> contains the namespace label
     * <code>venice/test</code> and the slot label <code>abc</code>.
     * <p>
     * Note: A well formed sensor name string should never start or end
     * with a slash. 
     *  
     * @param s The string to be searched for the slot label
     * @return <ul><li>The label of the slot, if found.</li>
     * <li>Empty string, if there is no label after the last slash or if s is
     * an empty string by itself.
     * (Example: <code>test/</code> leads to an empty string)</li>
     * <li>The input string itself, if there is no namespace label
     * (Example: <code>abc</code> leads to <code>abc</code>)</li>
     * <li><code>null</code> if s is <code>null</code>.
     * </ul>
     */
    public static String findSlotLabel(String s){
    	return s != null ? s.substring(s.lastIndexOf('/')+1) : null;
    }
    
    /**
     * Finds and returns the label of the namespace in a sensor name string.
     * Example: In the string <code>venice/test/abc</code> the label of the
     * slot is <code>venice/test</code>.
     * <p>
     * For more information see <code>findSlotLabel</code>.
     * 
     * @param s The string to be searched for the namespace label
     * @return <ul><li>The label of the namespace, if found.</li>
     * <li>Empty string, if there is no label before the last slash,
     * or if s does not contain a slash.
     * (Example: <code>abc</code> leads to an empty string)</li>
     * <li><code>null</code> if s is <code>null</code>.
     * </ul>
     */
    public static String findNamespaceLabel(String s){
    	if(s == null) return null;
    	int p = s.lastIndexOf('/');
    	if(p >= 0) return s.substring(0, p);
    	else return "";
    }
    
    /**
     * Converts the value data of a protobuf object into a String.
     * @param value The protobuf object holding the data.
     * @return The String representation of the data
     */
    public static String protobuf2Str(Object value){
    	return venice.lib.networkRSB.ValueParser.protobufToString(value);
    }
    
    /**
     * Generates a fail slotEvent.
     * A fail slotEvent should be returned by the parser
     * when it fails to parse a string into an event.
     * The value will be the string that the parser failed to parse. The
     * slotlabel will just be "fail" and the timestamp will be set to
     * the value of the <code>INVALID_TIMESTAMP</code> constant.
     * @param s The string causing the parser to fail
     * @return a fail slotEvent, containing the parameter s as value, "fail" as
     * slot label, <code>String</code> as type and
     * <code>INVALID_TIMESTAMP</code> as timestamp
     */
    public static SlotEvent failSlotEvent(String s){
    	return new SlotEvent(s, "", "fail", String.class, INVALID_TIMESTAMP);
    }
    
    /**
     * Converts String to an integer. A
     * <code>NumberFormatException</code> will
     * not be thrown - instead a 0 is returned. Otherwise it is acting
     * like <code>Integer.parseInt</code>.
     *
     * @param str string to convert
     * @return the integer value represented by the argument in decimal,
     * or 0 if parsing fails because of a <code>NumberFormatException</code>
     */
    public static int str2int(String str) {
    	int value = 0;
    	try{
    		value = Integer.parseInt(str);
    	}
    	catch(NumberFormatException e){}
        return value;
    }
    
    /**
     * Converts String to a long integer. A
     * <code>NumberFormatException</code> will
     * not be thrown - instead a 0 is returned. Otherwise it is acting
     * like <code>Long.parseLong</code>.
     *
     * @param str string to convert
     * @return the long integer value represented by the argument in decimal,
     * or 0 if parsing fails because of a <code>NumberFormatException</code>
     */
    public static long str2long(String str) {
    	long value = 0;
    	try{
    		value = Long.parseLong(str);
    	}
    	catch(NumberFormatException e){}
        return value;
    }
    
    /**
     * Converts String to a float. A
     * <code>NumberFormatException</code> will
     * not be thrown - instead a 0 is returned. Otherwise it is acting
     * like <code>Float.parseFloat</code>.
     *
     * @param str string to convert
     * @return the float value represented by the argument in decimal,
     * or 0 if parsing fails because of a <code>NumberFormatException</code>
     */
    public static float str2float(String str) {
    	float value = 0;
    	try{
    		value = Float.parseFloat(str);
    	}
    	catch(NumberFormatException e){}
        return value;
    }
    
    /**
     * Converts String to a double precision float. A
     * <code>NumberFormatException</code> will
     * not be thrown - instead a 0 is returned. Otherwise it is acting
     * like <code>Double.parseDouble</code>.
     *
     * @param str string to convert
     * @return the double precision float value represented by the argument in decimal,
     * or 0 if parsing fails because of a <code>NumberFormatException</code>
     */
    public static double str2double(String str) {
    	double value = 0;
    	try{
    		value = Double.parseDouble(str);
    	}
    	catch(NumberFormatException e){}	
        return value;
    }
    
    /**
     * Converts a string to a boolean.
     *
     * @param str string to convert
     * @return boolean value represented by the argument
     */
    public static boolean str2bool(String str) {
    	boolean value = Boolean.valueOf(str);
        return value;
    }

    /**
     * Converts a string to a <code>org.instantreality.Vec2f</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert, must contain 2 float values separated by
     * spaces
     * @return <code>org.instantreality.Vec2f</code> representing the string
     */
    public static Vec2f str2vec2f(String str) {
        Float[] floats = new Float[2];

        StringTokenizer sT = new StringTokenizer(str, " ");
        int i = 0;
        while (sT.hasMoreTokens()) {
        	try{
        		floats[i] = Float.parseFloat(sT.nextToken());
        	}
        	catch(NumberFormatException e){
        		floats[i] = (float) 0;
        	}
            i++;
        }

        Vec2f value = new Vec2f(floats[0], floats[1]);
        return value;
    }
    
    /**
     * Converts a string to a <code>org.instantreality.Vec3f</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert, must contain 3 float values separated by
     * spaces
     * @return <code>org.instantreality.Vec3f</code> representing the string
     */
    public static Vec3f str2vec3f(String str) {
        Float[] floats = new Float[3];

        StringTokenizer sT = new StringTokenizer(str, " ");
        int i = 0;
        while (sT.hasMoreTokens()) {
            floats[i] = Float.parseFloat(sT.nextToken());
            i++;

        }
        Vec3f value = new Vec3f(floats[0], floats[1], floats[2]);
        return value;
    }
    
    /**
     * Converts a string to a <code>org.instantreality.Rotation</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert, must contain 4 float values separated by
     * spaces
     * @return <code>org.instantreality.Rotation</code> representing the string
     */
    public static Rotation str2rot(String str) {
        Float[] floats = new Float[4];

        StringTokenizer sT = new StringTokenizer(str, " ");
        int i = 0;
        while (sT.hasMoreTokens()) {
        	try{
        		floats[i] = Float.parseFloat(sT.nextToken());
        	}
        	catch(NumberFormatException e){
        		floats[i] = (float) 0;
        	}
            i++;
        }
        Rotation value = new Rotation(floats[0], floats[1], floats[2], floats[3]);
        return value;
    }
    
    /**
     * Converts a string to an array of <code>org.instantreality.Vec2f</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert
     * @return array of <code>org.instantreality.Vec2f</code>
     */
    public static Vec2f[] str2mfvec2f(String str){
    	String[] tokens = str.substring(1, str.length()-1).split(", ");
    	Vec2f[] value = new Vec2f[tokens.length];
    	for(int n=0; n<tokens.length; n++){
    		try{
    			value[n] = new Vec2f( Float.parseFloat(tokens[n].split(" ")[0]), Float.parseFloat(tokens[n].split(" ")[1]) );
    		}
    		catch(NumberFormatException e){
    			value[n] = new Vec2f(0, 0);
    		}
    	}
    	return value;
    }
    
    /**
     * Converts a string to an array of <code>org.instantreality.Vec3f</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert
     * @return array of <code>org.instantreality.Vec3f</code>
     */
    public static Vec3f[] str2mfvec3f(String str){
    	String[] tokens = str.substring(1, str.length()-1).split(", ");
    	Vec3f[] value = new Vec3f[tokens.length];
    	for(int n=0; n<tokens.length; n++){
    		try{
    			value[n] = new Vec3f( Float.parseFloat(tokens[n].split(" ")[0]),
    							   	  Float.parseFloat(tokens[n].split(" ")[1]),
    				                  Float.parseFloat(tokens[n].split(" ")[2]));
    		}
    		catch(NumberFormatException e){
    			value[n] = new Vec3f(0, 0, 0);
    		}
    	}
    	return value;
    }
    
    /**
     * Converts a string to an array of <code>org.instantreality.Rotation</code>.
     * If a <code>NumberFormatException</code> occurs while parsing a field,
     * that field will be set to 0.
     *
     * @param str string to convert
     * @return array of <code>org.instantreality.Rotation</code>
     */
    public static Rotation[] str2mfrot(String str){
    	String[] tokens = str.substring(1, str.length()-1).split(", ");
    	Rotation[] value = new Rotation[tokens.length];
    	for(int n=0; n<tokens.length; n++){
    		try{
    			value[n] = new Rotation( Float.parseFloat(tokens[n].split(" ")[0]),
    				                     Float.parseFloat(tokens[n].split(" ")[1]),
    				                     Float.parseFloat(tokens[n].split(" ")[2]),
    				                     Float.parseFloat(tokens[n].split(" ")[3]));
    		}
    		catch(NumberFormatException e){
    			value[n] = new Rotation(0, 0, 0, 0);
    		}
    	}
    	return value;
    }
    
    /**
     * Converts a string into an array of strings.
     * The string has to be in the format
     * <code>
     * [string1, string2, string3, ...]
     * </code>
     * <p>
     * The substrings have to be separated by one comma and one space.
     * @param str string to convert
     * @return array of strings
     */
    public static String[] str2mfstr(String str){
    	return str.substring(1, str.length()-1).split(", ");
    }
    
    public static Float[] str2mffloat(String str){
    	String[] tokens = str.substring(1, str.length()-1).split(", ");
    	Float[] value = new Float[tokens.length];
    	for(int n=0; n<tokens.length; n++){
    		try{
    			value[n] = new Float(tokens[n]);
    		}
    		catch(NumberFormatException e){
    			value[n] = new Float(0.0f);
    		}
    	}
    	return value;
    }
    
    /**
     * Converts a string into an object of the demanded type and set the result
     * as the new value for the given slotEvent.
     * 
     * @param e The slotEvent, that gets the converted value
     * @param valueString The value as a string
     * @param className The type, that the value should have
     */
    public static void setEventValue(SlotEvent e, String valueString, String className){
    	switch(className){
    	case "java.lang.Integer": e.setValue(str2int(valueString)); break;
    	case "java.lang.Long": e.setValue(str2long(valueString)); break;
    	case "java.lang.Float": e.setValue(str2float(valueString)); break;
    	case "java.lang.Double": e.setValue(str2double(valueString)); break;
    	case "java.lang.String": e.setValue(valueString); break;
    	case "java.lang.Boolean": e.setValue(str2bool(valueString)); break;
    	case "org.instantreality.InstantIO.Vec2f": e.setValue(str2vec2f(valueString)); break;
    	case "org.instantreality.InstantIO.Vec3f": e.setValue(str2vec3f(valueString)); break;
    	case "org.instantreality.InstantIO.Rotation": e.setValue(str2rot(valueString)); break;
    	case "org.instantreality.InstantIO.Color": e.setValue(Color.valueOf(valueString)); break;
    	case "org.instantreality.InstantIO.ColorRGBA": e.setValue(ColorRGBA.valueOf(valueString)); break;
    	case "org.instantreality.InstantIO.Matrix3f": e.setValue(Matrix3f.valueOf(valueString)); break;
    	case "org.instantreality.InstantIO.Matrix4f": e.setValue(Matrix4f.valueOf(valueString)); break;
    	case "[Lorg.instantreality.InstantIO.Vec2f;": e.setValue(str2mfvec2f(valueString)); break;
    	case "[Lorg.instantreality.InstantIO.Vec3f;": e.setValue(str2mfvec3f(valueString)); break;
    	case "[Lorg.instantreality.InstantIO.Rotation;": e.setValue(str2mfrot(valueString)); break;
    	case "[Ljava.lang.String;": e.setValue(str2mfstr(valueString)); break;
    	case "[Ljava.lang.Float;": e.setValue(str2mffloat(valueString)); break;
    	default:
    		if(className.startsWith("protobuf")){
    			e.setValue(ValueParser.stringToProtobuf(valueString, e.getType()));
    		}
    		else
    			e.setValue(valueString);
    	}
    }
    
    /**
     * Converts the value into a String.
     * Will catch arrays and convert them element by element in java style.
     * Can also handle protobuf classes.
     * 
     * @param value the value to be converted
     * @return string representation of the value
     */
    public static String value2str(Object value){
    	if( value.getClass().getName().startsWith("protobuf"))
    		return protobuf2Str(value);
    	if( value.getClass().isArray() )
    		return Arrays.toString((Object[]) value);
    	else
    		return value.toString();
    }
}
