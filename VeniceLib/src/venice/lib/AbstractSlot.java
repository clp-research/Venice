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
package venice.lib;

/**
 * Describes a sensor slot by label, namespace and data type.
 * <p>
 * Can be used for InstantIO namespace and RSB scope and possibly for other protocols.<br>
 * Every class, that represents a sensor slot (or "field") should extend this class.<br>
 */
public class AbstractSlot {
	protected String label; // name of the slot
	protected String namespace; // scope (or namespace) of the slot
	protected Class<?> type; // type of the data the sensor will produce
	
	/**
	 * Constructs a nameless slot without type.
	 */
	public AbstractSlot(){
		this("", null, null);
	}
	
	/**
	 * Constructs an abstract slot without namespace.
	 * @param label Label of the slot
	 * @param type Type of the data the slot will handle
	 */
	public AbstractSlot(String label, Class<?> type){
		this("", label, type);
	}
	
	/**
	 * Constructs an abstract slot.
	 * 
	 * @param namespace Namespace of the slot
	 * @param label Label of the slot
	 * @param type Type of the data the slot will handle
	 */
	public AbstractSlot(String namespace, String label, Class<?> type){
		this.namespace = namespace;
		this.label = label;
		this.type = type;
	}
	
	/**
	 * Returns the name of the sensor. The name can include namespace- and scope-like information.
	 * Example: "OpenDS/Car/CarVelocity" can be a sensor name.
	 * 
	 * @return Returns name of the sensor.
	 */
	public String getLabel(){
		return label;
	}
	
	/**
	 * Sets the name of the sensor. The name can include namespace- and scope-like information.
	 * Example: "OpenDS/Car/CarVelocity" can be a sensor name.
	 * 
	 * @param name The new name of the sensor
	 */
	public void setLabel(String name){
		this.label = name;
	}
	
	/**
	 * Returns the Scope of this slot.
	 * <p>
	 * A Scope consists of a Namespace and a Label. The part before the last
	 * slash is the Namespace and the part after the last slash is the Label.
	 * <p>
	 * Example:<br>
	 * The scope <code>'Venice/test/slotA'</code> consists of the Namespace
	 * <code>'Venice/test'</code> and the Label <code>'slotA'</code>.<br>
	 * <br>
	 * If the namespace is <code>MyNamespace</code> and the
	 * label of the slot is <code>SlotA</code>, then the Scope will
	 * be <code>MyNamespace/SlotA</code>.
	 */
	public String getScope(){
		if(namespace != null && ! namespace.isEmpty())
			return namespace + "/" + label;
		else
			return label;
	}
	
	/**
	 * Returns the type of the sensor.
	 * 
	 * @return Returns the type of the sensor
	 */
	public Class<?> getType(){
		return type;
	}
	
	/**
	 * Sets the type of the sensor.
	 * @param type The new type of the sensor
	 */
	public void setType(Class<?> type){
		this.type = type;
	}
	
	/**
	 * Sets the namespace of the slot.
	 * 
	 * @param nmspc a string without leading or following slash
	 */
	public void setNamespace(String nmspc){
		namespace = nmspc;
	}
	
	/**
	 * Returns the namespace of the slot.
	 */
	public String getNamespace(){
		return namespace;
	}
	
	/**
	 * Returns a string representation of this sensor.
	 * 
	 * @return Returns a string representation of this sensor.
	 */
	public String toString(){
		return getScope() + " (" + type.getName() + ")";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((namespace == null) ? 0 : namespace.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractSlot other = (AbstractSlot) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (namespace == null) {
			if (other.namespace != null)
				return false;
		} else if (!namespace.equals(other.namespace))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}
}
