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

import venice.hub.utils.Configuration;
import venice.lib.networkRSB.RSBNamespaceBuilder;
import venice.lib.parser.SlotEvent;

/**
 * Writes data to RSB scope.
 * <p>
 * Writes TTE items from TTEQueue to RSB scope, using predefined scope or default scope. 
 * 
 */
public class RSBWriter extends VeniceWriter {
	
	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(RSBWriter.class);
	}
	
	/**
	 * Prepare scope for RSB.
	 * <p>
	 * If predefined scopes are available, they will be created.
	 * Otherwise the default scope will be used.
	 */
	protected void initialize(){
		logger.debug("initializing");
		// give path to protobuf folder (or null, if venice.lib should search for it by itself)
    	RSBNamespaceBuilder.setProtobufDir(Configuration.getInstance().getProtobufDir());
    	// give name of file for class-matching
    	RSBNamespaceBuilder.setMatchFile(config.getClassMatchFile());
    	// give name of file for xio codes
    	RSBNamespaceBuilder.setXioCodesFilename(config.getXioCodesFilename());
    	// set the prefix that will be added to all scopes
    	RSBNamespaceBuilder.setPrefix("VeniceHubReplay");
    	// initialize protobuf classes
    	RSBNamespaceBuilder.initializeProtobuf();
    	// load xio codes
    	config.readSlotFile();
    	// give predefined slots if existing, or activate dynamic slot creation
		if(config.isSensorPredefined())
			RSBNamespaceBuilder.initializeOutSlots(config.getPreScopes());
		else{
			RSBNamespaceBuilder.initializeOutSlots();
			RSBNamespaceBuilder.setPrefix(config.getRSBDefaultInformerScope());
		}
	}

	/**
	 * Write data to RSB scope.
	 * <p>
	 * Decodes the TTE item and write it to RSB.
	 */
    public long write(final SlotEvent e){
        
    	if (e == null || e.getType() == null) {
    		return NO_TIMESTAMP;
    	}
    	
    	long writingTime = NO_TIMESTAMP;

    	String scope = e.getScope();
    	
    	Object value;
    	if(Configuration.getInstance().isRSBToXIO())
    		value = parser.eventToString(e);
    	else
    		value = e.getValue();
    	
    	logger.debug("writing to RSB: "+e);
    	boolean written = RSBNamespaceBuilder.write(scope, value);
    	
    	if(written){
    		lastTimestamp = e.getTime();
    		VeniceHub.setLastPushedTimestamp(lastTimestamp);
    	}
    	
    	return writingTime;
    }
    
    /**
     * Remove all slots.
     */
    protected void cleanUp(){
    	RSBNamespaceBuilder.removeAll();
    }
}
