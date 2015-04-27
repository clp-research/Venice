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
package venice.lib.networkIIO;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.BufferedInSlot;
import org.instantreality.InstantIO.Namespace;
import org.instantreality.InstantIO.InSlot;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;

import venice.lib.Configuration;
import venice.lib.parser.XIOParser;

/**
 * A listener to the InstantIO namespace, to get informed when new slots
 * are created.
 */
public class IIONamespaceListener implements Namespace.Listener{
	private static Logger logger;
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(IIONamespaceBuilder.class);
	}

	/**
	 * Creates a namespace listener to listen to the root namespace.
	 */
	public IIONamespaceListener(){
		Root.the().addListener(this);
	}
	
	@Override
	public void inSlotAdded(Namespace arg0, String arg1, InSlot arg2) {
	}

	@Override
	public void inSlotRemoved(Namespace arg0, String arg1, InSlot arg2) {
	}

	
	/**
	 * Gets called by InstantReality, when an outslot is added.
	 * Checks if the new outslot is already known. If not, a new inslot is created for it
	 * and a new listener is added to that in-slot ({@link IIOInSlotListener}).
	 * 
	 * @param namespace namespace of the detected outSlot
	 * @param label label of the detected outSlot
	 * @param outSlot detected outSlot
	 */
	@Override
	public void outSlotAdded(Namespace namespace, String label, OutSlot outSlot) {
		
		String scope = label;
		String slotLabel = XIOParser.findSlotLabel(scope);
		String namespaceLabel = XIOParser.findNamespaceLabel(scope);
		
		logger.debug("outslot '"+slotLabel+"' detected in namespace '"+namespaceLabel+"'");
		
        if (IIONamespaceBuilder.getInSlotMap().containsKey(scope)) return;
        
        InSlot inSlot = new BufferedInSlot(outSlot.getType(), outSlot.getDescription(), 80);
        
        Namespace ownNamespace = IIONamespaceBuilder.findNamespace(namespaceLabel, true);
        ownNamespace.addInSlot(slotLabel, inSlot);
        ownNamespace.addExternalRoute(slotLabel, "{NamespaceLabel}/{SlotLabel}");
        
        // register a new Listener for the new InSlot
        inSlot.addListener(new IIOInSlotListener(namespaceLabel, slotLabel));

        IIONamespaceBuilder.getInSlotMap().put(scope, inSlot);
	}

    /**
     * Gets called by InstantReality, when an outslot is removed from the
     * network. Will remove the inslot that is linked to the removed
     * outslot.
     * 
     * @param namespace from where the outslot was removed
     * @param label name of the removed outslot
     * @param outSlot the removed <code>OutSlot</code> object
     */
    @Override
    public void outSlotRemoved(Namespace namespace, String label, OutSlot outSlot) {
    	logger.debug("outslot removed: "+label);
    	
        // get associated inSlot
        InSlot inSlot = IIONamespaceBuilder.getInSlotMap().get(label);

        // don't do anything if this slot does not exist
        if (inSlot == null) {
            return;
        }

        // remove slot from namespace and forget it
        namespace.removeInSlot(label, inSlot);

        IIONamespaceBuilder.getInSlotMap().remove(label);
    }

	@Override
	public void routeAdded(Namespace arg0, String arg1, String arg2) {
	}

	@Override
	public void routeRemoved(Namespace arg0, String arg1, String arg2) {
	}

}
