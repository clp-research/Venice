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
package venice.lib.networkIIO;

/**
 * Defines flags for the handling of slots for the network node.
 * <p>
 * If a flag is <code>null</code>, the relevant object has to decide on its
 * own what setting is to be used.
 * <p>
 * The IIONamespaceBuilder uses SlotFlags to determine activation or
 * deactivation of importing/exporting outslots/inslots.
 * 
 * @author Oliver Eickmeyer
 */
public class SlotFlags {
	Boolean importing;
	Boolean exporting;
	
	/**
	 * Constructs a new setting of flags with default values.
	 * <p>
	 * Default values for importing and exporting slots are
	 * <code>null</code>
	 */
	public SlotFlags(){
		setImporting(null);
		setExporting(null);
	}
	
	/**
	 * Constructs a new setting of flags with the given values.
	 * 
	 * @param importFlag sets the flag for importing slots
	 * @param exportFlag sets the flag for exporting slots
	 */
	public SlotFlags(Boolean importFlag, Boolean exportFlag){
		setImporting(importFlag);
		setExporting(exportFlag);
	}

	/**
	 * Returns state of importing.
	 * @return <code>true</code>=active, <code>false</code>=not active
	 */
	public Boolean isImporting() {
		return importing;
	}
	
	/**
	 * Sets state of importing.
	 * @param importing <code>true</code>=active, <code>false</code>=not active
	 */
	public void setImporting(Boolean importing) {
		this.importing = importing;
	}
	
	/**
	 * Returns state of exporting.
	 * @return <code>true</code>=active, <code>false</code>=not active
	 */
	public Boolean isExporting() {
		return exporting;
	}
	
	/**
	 * Sets state of exporting.
	 * @param exporting <code>true</code>=active, <code>false</code>=not active
	 */
	public void setExporting(Boolean exporting) {
		this.exporting = exporting;
	}
}
