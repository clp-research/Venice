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
package venice.hub;

import venice.hub.utils.Configuration;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.networkIIO.SlotFlags;
import venice.lib.parser.SlotEvent;

/**
 * Writes data to InstantIO namespace.
 * <p>
 * It creates necessary outslots and sends TTE items to them by their sensor name.
 * The creation of outslots can happen dynamic or predefined (see {@link Configuration}).
 */
public class IIOWriter extends VeniceWriter{

    @Override
    /**
     * Create outslots if they are predefined.
     */
    protected void initialize(){
    	// initialize outslots
    	IIONamespaceBuilder.setMulticastTTL(config.getMulticastTTL());
    	IIONamespaceBuilder.setUnicastAddress(config.getUnicastAddress());
    	IIONamespaceBuilder.setUnicastPort(config.getUnicastPort());
    	IIONamespaceBuilder.setUnicastServers(config.getUnicastServers());
    	IIONamespaceBuilder.setXioCodesFilename(config.getXioCodesFilename());
    	IIONamespaceBuilder.setPrefix("VeniceHubReplay");
    	
    	SlotFlags slotFlags = config.getSlotFlags();
		if(slotFlags.isExporting() == null) slotFlags.setExporting(true);
		if(slotFlags.isImporting() == null) slotFlags.setImporting(false);
		IIONamespaceBuilder.setSlotFlags(slotFlags);
    	
		IIONamespaceBuilder.prepareNamespace(config.getNamespaceLabel());
		config.readSlotFile();
		IIONamespaceBuilder.setSendInitValue(Configuration.getInstance().getSendInitValue());
    	IIONamespaceBuilder.initializeOutSlots(Configuration.getInstance().getPreScopes());
    }

    @Override
    public long write(final SlotEvent slotEvent){
    	
    	if(slotEvent == null){
    		return NO_TIMESTAMP;
    	}
    	
    	String slotLabel = slotEvent.getLabel();
    	String namespaceLabel = slotEvent.getNamespace();
    	Object value = slotEvent.getValue();
    	long writingTime = System.currentTimeMillis();
    	Object data = value;

    	boolean success = IIONamespaceBuilder.write(slotLabel, data, namespaceLabel);
    	
    	if(!success) return NO_TIMESTAMP;
    	
        lastTimestamp = slotEvent.getTime(); // store lastTimestamp locally (e.g. for lag calculation)
        VeniceHub.setLastPushedTimestamp(lastTimestamp); // store it globally to (some classes need it)
    	
		return writingTime;
    }
}
