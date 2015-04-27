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

import java.util.logging.Level;
import java.util.logging.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEQueue;
import venice.lib.AbstractSlotListener;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.networkIIO.SlotFlags;
import venice.lib.parser.SlotEvent;

/**
 * Listens to InstantIO namespace and receives data from slots.
 * <p>
 * Received data will be put into the {@link venice.hub.utils.TTEQueue}.
 */
public class IIOReader extends VeniceReader implements AbstractSlotListener{
    private TTEQueue queue = TTEQueue.getInstance();
    private final int QUEUE_CAPACITY = Configuration.getInstance().getQueueCapacity();
	
	protected void initialize(){
		IIONamespaceBuilder.setMulticastTTL(config.getMulticastTTL());
		IIONamespaceBuilder.setUnicastAddress(config.getUnicastAddress());
    	IIONamespaceBuilder.setUnicastPort(config.getUnicastPort());
    	IIONamespaceBuilder.setUnicastServers(config.getUnicastServers());
		IIONamespaceBuilder.setXioCodesFilename(config.getXioCodesFilename());
		IIONamespaceBuilder.setPrefix("VeniceHubLog");
		
		SlotFlags slotFlags = config.getSlotFlags();
		if(slotFlags.isExporting() == null) slotFlags.setExporting(false);
		if(slotFlags.isImporting() == null) slotFlags.setImporting(true);
		IIONamespaceBuilder.setSlotFlags(slotFlags);
		
		IIONamespaceBuilder.prepareNamespace(config.getNamespaceLabel());
		config.readSlotFile();
		IIONamespaceBuilder.initializeInSlots(Configuration.getInstance().getPreScopes());
		IIONamespaceBuilder.setMasterInSlotListener(this);
	}

	@Override
	public void newData(Object data, String namespace, String label, Class<?> type) {

		if(TTEQueue.getInstance().size() >= QUEUE_CAPACITY) return; // if TTEQueue is full, ignore data
    	
        if(data == null){
        	// actually never happened so far, but to be sure
        	return;
        }

        SlotEvent e = new SlotEvent();
        e.setType(type);
        e.setValue(data);
        e.setNamespace(namespace);
        e.setLabel(label);
        e.setTime(System.currentTimeMillis());
        pushEvent(e);
    }
    private void pushEvent(SlotEvent e) {
        try {
            queue.put(e);
        } catch (InterruptedException ex) {
            Logger.getLogger(IIOReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
