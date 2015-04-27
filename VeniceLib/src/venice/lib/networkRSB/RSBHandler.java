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

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.protobuf.Message;
import com.google.protobuf.Descriptors.FieldDescriptor;

import rsb.AbstractEventHandler;
import rsb.Event;
import venice.lib.Configuration;
import venice.lib.parser.XIOParser;

/**
 * Waits for new RSB-Events and transfers the received RSB-data
 * to the master-listener (of the application that uses this library).
 * <p>
 * This handler is normally added to every RSB in-slot by the {@link RSBNamespaceBuilder}.
 * <p>
 * It will also take care of class-matching, if present.
 */
public class RSBHandler extends AbstractEventHandler {
	private static Logger logger;
	private HashMap<Class<?>, ClassMatcher> matchProtobufToOtherMap;
	
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(RSBHandler.class);
	}
	
	/**
	 * Creates a new RSB handler and checks is class-matching is active.
	 */
	public RSBHandler(){
		super();
		matchProtobufToOtherMap = RSBNamespaceBuilder.getMatchProtobufToOtherMap();
	}

	@Override
	/**
	 * Transfers the received RSB-data to the master-listener (of the
	 * application that uses this library).
	 */
	public void handleEvent(Event event){
		String scope = event.getScope().toString();
		if(scope.equals("/")){
			// if event was received on root
			scope = "";
		}
		else{
			/* If event used scope.
			 * A scope always begins and ends with a slash,
			 * so it contains at least 3 characters.
			 * For venice, leading and trailing slashes
			 * have to be removed.
			 */
			scope = scope.substring(1, scope.length()-1);
		}
		String namespace = XIOParser.findNamespaceLabel(scope);
		String label = XIOParser.findSlotLabel(scope);
		Object data = event.getData();
		Class<?> type = event.getType();
//		logger.debug("receiving a "+type.getName()+" from "+scope);
		ClassMatcher cm = matchProtobufToOtherMap.get(type);
//		logger.debug("class matcher = "+cm);
		if(cm == null){
			// no matching: transfer data directly to the master
			RSBNamespaceBuilder.getMasterInSlotListener().newData(data, namespace, label, type );	
		}
		else{
			// if matching type exists, convert data before transferring to master
			if(cm.isRepeated){
				
				// handle data as a
				// MULTIFIELD
				
				Message m = (Message) event.getData();
//				logger.debug("  Message "+m);
				List<FieldDescriptor> fdList = m.getDescriptorForType().getFields();
				int nField = m.getRepeatedFieldCount(fdList.get(0));
				Object[] convertedData = (Object[]) Array.newInstance(cm.target.getComponentType(), nField);
				for(int iA=0; iA<nField; iA++){
					Object[] parameters = new Object[cm.type.length];
					for(int iP = 0; iP<cm.type.length; iP++){
						try {
							parameters[iP] = cm.getter[iP].invoke(data, iA);
						} catch (IllegalAccessException | IllegalArgumentException
								| InvocationTargetException e) {
							logger.error("Failed to get value "+iP+" from "+type.getName()+"["+iA+"]");
							return;
						}
					}
					try {
						convertedData[iA] = cm.constructor.newInstance(parameters);					
					} catch (InstantiationException | IllegalAccessException
							| IllegalArgumentException | InvocationTargetException e) {
						logger.error("Failed to construct "+cm.target.getName());
						return;
					}
				}
				RSBNamespaceBuilder.getMasterInSlotListener().newData(convertedData, namespace, label, cm.target);
			}
			else{
				
				// handle data as a
				// SINGLEFIELD
				
				Object[] parameters = new Object[cm.type.length];
				for(int i = 0; i<cm.type.length; i++){
					try {
						parameters[i] = cm.getter[i].invoke(data);
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						logger.error("Failed to get value "+i+" from "+type.getName());
						return;
					}
				}
				Object convertedData = null;
				try {
					convertedData = cm.constructor.newInstance(parameters);					
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException e) {
					logger.error("Failed to construct "+cm.target.getName());
					return;
				}
				RSBNamespaceBuilder.getMasterInSlotListener().newData(convertedData, namespace, label, cm.target);
			}
		}
	}
}
