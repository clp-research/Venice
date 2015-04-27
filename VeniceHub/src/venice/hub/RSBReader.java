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
package venice.hub;

import org.apache.log4j.Logger;

import venice.hub.utils.TTE;
import venice.hub.utils.TTEQueue;
import venice.lib.AbstractSlotListener;
import venice.lib.networkRSB.RSBNamespaceBuilder;
import venice.lib.parser.SlotEvent;

/**
 * Reads data from RSB.
 * <p>
 * Receives data over RSB and put it into the TTEQueue. 
 * Currently assumes that input is XIO-formatted strings.
 */
public class RSBReader extends VeniceReader implements AbstractSlotListener{
	private static Logger logger;
	
	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(RSBReader.class);
	}
	
	/**
	 * Gets called by the RSB handler of venice.lib. 
	 * venice.lib will pass new data through this method to venice.hub.
	 * 
	 * @param data The object that represents the data
	 * @param type The type of the data
	 * @param label The label of the slot where the data was received
	 */
    public void newData(Object data, String namespace, String label, Class<?> type){
    	logger.debug("new data: "+data.toString()+" ("+type.getName()+") from "+label);
    	if(TTEQueue.getInstance().size() >= QUEUE_CAPACITY) return; // if TTEQueue is full, ignore data
		SlotEvent slotEvent;
		
		if(config.isRSBStringXIOLine()){
			// parse received String as a XIO line
			slotEvent = parser.stringToEvent((String) data);
		}
		else{
			// convert data to TTE object
			slotEvent = new TTE();
			slotEvent.setNamespace(namespace);
			slotEvent.setLabel(label);
			slotEvent.setType(type);
			slotEvent.setValue(data);
		}
		slotEvent.setTime(System.currentTimeMillis()); // overwriting the timestamp of the XIO line
        try {
			TTEQueue.getInstance().put(slotEvent);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Creates a Handler for receiving RSB data.
     */
    protected void initialize(){
    	// give path to protobuf folder (or null, if venice.lib should search for it by itself)
    	RSBNamespaceBuilder.setProtobufDir(config.getProtobufDir());
    	// give name of file for class-matching
    	RSBNamespaceBuilder.setMatchFile(config.getClassMatchFile());
    	// give name of file for xio codes
    	RSBNamespaceBuilder.setXioCodesFilename(config.getXioCodesFilename());
    	// setup listening scope
    	RSBNamespaceBuilder.setPrefix(config.getRSBDefaultInformerScope());
    	// initialize protobuf classes
    	RSBNamespaceBuilder.initializeProtobuf();
    	// load xio codes
    	config.readSlotFile();
    	// initialize default in-slot listener
    	RSBNamespaceBuilder.initializeInSlots(config.getPreScopes());
    	// register RSBReader as the master listener for new data
    	RSBNamespaceBuilder.setMasterInSlotListener(this);
    }
    
    /**
     * Removes all slots.
     */
    protected void cleanUp(){
    	RSBNamespaceBuilder.removeAll();
    }
}
