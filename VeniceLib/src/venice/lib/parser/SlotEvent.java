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
package venice.lib.parser;

import java.util.Arrays;

import venice.lib.AbstractSlot;

/**
 * Holds a data item with value, sensor name, data type and timestamp.
 * This class is mainly used to communicate data between a large number
 * of venice components and with the application.
 * It is unrelated to any specific network protocol, that is the reason
 * why the slot of the event is handled as a <code>String</code> instead
 * as an instance of an actual slot class (such as <code>InSlot</code>
 * from InstantIO, or <code>Informer</code> from RSB).
 */
public class SlotEvent extends AbstractSlot{
    
    protected Object value;
    protected long time;
    
    /**
     * Constructs an empty event.
     */
    public SlotEvent() {
    }
    
    /**
     * Constructs an event with all needed fields.
     * 
     * @param value the data of this event
     * @param namespace namespace for the slot
     * @param label the label of the slot used by the event
     * @param type the data type (=class) of the value
     * @param time the timestamp of the event
     */
    public SlotEvent(Object value, String namespace, String label, Class<?> type, long time){
    	this.value = value;
    	this.label = label;
    	this.namespace = namespace;
    	this.type = type;
    	this.time = time;
    }

    /**
     * Returns the data of this event.
     * 
     * @return the value of this event
     */
    public Object getValue() {
        return value;
    }

    /**
     * Sets the value for this event. For some protocols
     * it is necessary to set the type of the event according to the class
     * of the value object.
     * @param value the value of this event
     */
    public void setValue(Object value) {
        
        this.value = value;
    }

    /**
     * Gets the timestamp of this event.
     * 
     * @return the timestamp of this event
     */
    public long getTime() {
        return time;
    }

    /**
     * Sets the timestamp for this event.
     * 
     * @param time the timestamp to set
     */
    public void setTime(long time) {
        this.time = time;
    }
    
    /**
     * Returns a <code>String</code> representation of this event.
     * The format is<br>
     * <code>'{label}' of {type}, timed for {time}, value: {value}</code>
     * <p>
     * If there is no label, <code>{label}</code> will be just an empty string.
     * <p>
     * The <code>{type}</code> will be the java class name or
     * <code>"unknown type"</code>, if type is <code>null</code>.
     * <p>
     * <code>{time}</code> is the <code>String</code> representation of the
     * timestamp.
     * <p>
     * <code>{value}</code> is the <code>String</code> representation of the
     * value. If the value is an array, then each element of the array will
     * be turned into a <code>String</code> representation, with the format
     * defined by <code>java.util.Arrays</code>.
     * <p>
     * This method is used mainly as part of error messages or for debugging.
     * It is in no way meant to parse events. To parse events into
     * <code>String</code>s use {@link venice.lib.parser.XIOParser}.
     * 
     */
    @Override
    public String toString(){
    	String strName, strType, strValue;
    	strName = label != null ? label : "";
    	strType = type != null ? type.getName() : "unknown type";
    	if(value != null){
    		if(value.getClass().isArray())
    			strValue = Arrays.toString((Object[]) value);
    		else
    			strValue = value.toString();
    	}
    	else strValue = "null";
    	return "'"+strName+"' of "+strType+", timed for "+String.valueOf(time)+", value: "+strValue;
    }
    
    /**
     * Returns the hash code for this <code>SlotEvent</code>
     * 
     * @return a hash code for this <code>SlotEvent</code>
     */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (int) (time ^ (time >>> 32));
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	/**
	 * Checks whether two slotEvent objects have equal values.
	 * 
	 * @param obj the reference object with which to compare
	 * @return <code>true</code> if this object is the same as the
	 * obj argument; <code>false</code> otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SlotEvent other = (SlotEvent) obj;
		if (time != other.time)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
    
    
}
