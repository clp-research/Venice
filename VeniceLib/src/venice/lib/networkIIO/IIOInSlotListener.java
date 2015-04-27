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
import org.instantreality.InstantIO.InSlot;

import venice.lib.Configuration;

/**
 * This instantIO InSlotListener gets registered for each new InSlot.
 * It will receive new data and transfer it to the master-listener
 * (from the application that uses this library).
 */
public class IIOInSlotListener implements InSlot.Listener{
	private static Logger logger;
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(IIOInSlotListener.class);
	}
	
	String label;
	String namespace;
	
	/**
	 * Constructor for creating a new listener for the slot with the given label.
	 * @param label The label of the slot to be listened
	 */
	public IIOInSlotListener(String namespace, String label){
		this.label = label;
		this.namespace = namespace;
	}
	
	@Override
	/**
	 * Receives new data from the slot and transfers it to the master-listener.
	 * 
	 * @param inSlot The InSlot where the new data is ready
	 */
	public void newData(InSlot inSlot) {
		
		Object value = null;
		try {
			value = inSlot.popData().getValue();
		} catch (InterruptedException e) {
			e.printStackTrace();
			logger.warn("received invalid data");
			return;
		}
		
		Class<?> type = inSlot.getType();
		
		// send new data to master-listener
		IIONamespaceBuilder.getMasterInSlotListener().newData(value, namespace, label, type);
	}

	@Override
	public void startInSlot(InSlot arg0) {
	}

	@Override
	public void stopInSlot(InSlot arg0) {
	}
}
