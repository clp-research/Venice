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
package venice.lib.networkRSB;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.log4j.Logger;

import venice.lib.Configuration;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;

/**
 * Provides methods to convert between strings and protobuf messages.<br>
 * String format for singlefields: <code>"x y z"</code>.<br>
 * String format for multifields: <code>"[x1 y1 z1, x2 y2 z2, ...]"</code>.
 * <p> 
 * Examples:
 * <p>
 * A singlefield Float with value 0.123 is represented by <code>"0.123"</code>.<br>
 * A multifield Float with values 0.1, 0.2 and 0.3 is represented by
 * <code>"[0.1, 0.2, 0.3]"</code>.<br>
 * A singlefield Vec3f with values x=11.0, y=12.0 and z=13.0 is represented
 * by <code>"11.0 12.0 13.0"</code>.<br>
 * A multifield Vec3f with values x1=1.1, y1=1.2, z1=1.3, x2=2.1, y2=2.2 and
 * z2=2.3 is represented by <code>"[1.1 1.2 1.3, 2.1 2.2 2.3]"</code>.<br>
 */
public class ValueParser {
	private static Logger logger;
	
	static{
		Configuration.setupLogger();
		logger = Logger.getLogger(ValueParser.class);
	}
	
	/**
	 * Private constructor. This class can not be instantiated.
	 */
	private ValueParser(){
		// nothing
	}
	
	/**
	 * Converts a protobuf message into a string.<br>
	 * If singlefield, the string will be in format "x y z".<br>
	 * If multifield, the string will be in format "[x1 y1 z1, x2 y2 z2, ...]".<br>
	 * @param protobufMessage
	 * @return string containing values of protobuf message
	 */
    public static String protobufToString(Object protobufMessage){
		Message m = (Message) protobufMessage;
		
		String result = "";
		
		List<FieldDescriptor> fdList = m.getDescriptorForType().getFields();
		FieldDescriptor fd0 = fdList.get(0);
		if(fd0.isRepeated()){
			result="[";
			for(int iF=0; iF<m.getRepeatedFieldCount(fd0); iF++){
				if(iF>0) result += ", ";
				for(int iD=0; iD<fdList.size(); iD++){
					FieldDescriptor fd = fdList.get(iD);
					if(iD>0) result += " ";
					result += m.getRepeatedField(fd, iF).toString();
				}
			}
			result += "]";
		}
		else{
			for(int iD=0; iD<fdList.size(); iD++){
				FieldDescriptor fd = fdList.get(iD);
				if(iD>0) result += " ";
				result += m.getField(fd).toString();
			}
		}
		
		return result;
	}
	
	/**
	 * Converts a string of values into a protobuf message of the given type.<br>
	 * If singlefield, the string has to be in format "x y z".<br>
	 * If multifield, the string has to be in format "[x1 y1 z1, x2 y2 z2, ...]".<br>
	 * The class is expected to be a protobuf message class.
	 * @param valueString a string with values
	 * @param clazz a protobuf message type
	 * @return protobuf message of given type
	 */
	public static Message stringToProtobuf(String valueString, Class<?> clazz){
		Method builderMethod = null;
		try {
			builderMethod = clazz.getMethod("newBuilder");
		} catch (NoSuchMethodException | SecurityException e) {
			//e.printStackTrace();
		}
		if( builderMethod == null){
			logger.error("Could not access method newBuilder() for class "+clazz.getName());
			return null;
		}
		
		Message.Builder builder = null;
		try {
			builder = (Builder) builderMethod.invoke(clazz);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			//e.printStackTrace();
		}
		if(builder == null){
			logger.error("Could not invoke method newBuilder() on class "+clazz.getName());
			return null;
		}
		
		List<FieldDescriptor> fdl = builder.getDescriptorForType().getFields();
		
		logger.debug(fdl.size()+" fields for "+clazz.getName());
		
		if(fdl.get(0).isRepeated()){
			
			// MULTIFIELD
			
			logger.debug("repeated field");
			String[] fieldTokens = valueString.substring(1, valueString.length()-1).split(", ");
			logger.debug("fields: "+fieldTokens.length);
			for(int iF=0; iF<fieldTokens.length; iF++){
				logger.debug("field token "+iF+": \""+fieldTokens[iF]+"\"");
				if(fieldTokens[iF].length() == 0){
					logger.debug("empty field token");
				}
				else{
					String[] valueTokens;
					if(fdl.get(0).getJavaType() != JavaType.STRING){
						// if not String, separate values of one field by spaces
						// for example for vec2f split x and y value by space
						valueTokens = fieldTokens[iF].split(" ");
					}
					else{
						// if String, don't separate by spaces, because each field of
						// a multifield string can contain only one value (the string)
						// so a field of a mfstring can only have one token
						valueTokens = new String[1];
						valueTokens[0] = fieldTokens[iF];
					}
					for(int iT=0; iT<valueTokens.length; iT++){
						logger.debug("  value token "+iT+": "+valueTokens[iT]+" JavaType="+fdl.get(iT).getJavaType().toString());
						switch(fdl.get(iT).getJavaType()){
						case FLOAT:
							builder.addRepeatedField(fdl.get(iT), Float.parseFloat(valueTokens[iT]));
							break;
						case BOOLEAN:
							builder.addRepeatedField(fdl.get(iT), Boolean.parseBoolean(valueTokens[iT]));
							break;
						case DOUBLE:
							builder.addRepeatedField(fdl.get(iT), Double.parseDouble(valueTokens[iT]));
							break;
						case INT:
							builder.addRepeatedField(fdl.get(iT), Integer.parseInt(valueTokens[iT]));
							break;
						case LONG:
							builder.addRepeatedField(fdl.get(iT), Long.parseLong(valueTokens[iT]));
							break;
						case STRING:
							builder.addRepeatedField(fdl.get(iT), valueTokens[iT]);
							break;
						default:
							logger.warn("For the repeated field "+iT+" of "+clazz.getName()+" found unknown type: "+fdl.get(iT).getJavaType());	
						}
					}
				}
			}
		}
		else{
			
			// SINGLEFIELD
			
			String[] tokens = valueString.split(" ");
			for(int i=0; i<fdl.size(); i++){
				switch(fdl.get(i).getJavaType()){
				case FLOAT:
					builder.setField(fdl.get(i), Float.parseFloat(tokens[i]));
					break;
				case BOOLEAN:
					builder.setField(fdl.get(i), Boolean.parseBoolean(tokens[i]));
					break;
				case DOUBLE:
					builder.setField(fdl.get(i), Double.parseDouble(tokens[i]));
					break;
				case INT:
					builder.setField(fdl.get(i), Integer.parseInt(tokens[i]));
					break;
				case LONG:
					builder.setField(fdl.get(i), Long.parseLong(tokens[i]));
					break;
				case STRING:
					builder.setField(fdl.get(i), tokens[i]);
					break;
				default:
					logger.warn("For the field "+i+" of "+clazz.getName()+" found unknown type: "+fdl.get(i).getJavaType());
				}
			}
		}
		
		return builder.build();
	}
}
