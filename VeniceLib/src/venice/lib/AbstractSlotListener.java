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
package venice.lib;

/**
 * Interface for an InSlot Listener that is independent from the network protocol.<br>
 * AbstractSlotListeners are used by the namespace-builders to send incoming
 * data to the application.  So if an application wants to read data from the
 * network, it must have an implementation of this interface and set is as
 * the master-listener for the relevant namespace-builder (see method
 * <code>setMasterInSlotListener</code> for the relevant namespace-builder).
 * 
 * @param data The data that has to be received
 * @param type The type of the data (and of the slot)
 * @param label The complete namespace/scope-label of the slot
 */
public interface AbstractSlotListener {
	
	/**
	 * This method will be called by the namespace-builders to send new
	 * incoming data to the application.
	 * 
	 * @param data the new data received via network
	 * @param namespace the namespace of the slot where the data was received
	 * @param label the label of the slot where the data was received
	 * @param type the type of the data
	 */
	public void newData(Object data, String namespace, String label, Class<?> type);
}
