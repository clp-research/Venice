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
package venice.hub.utils;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import venice.hub.VeniceHub;
import venice.lib.parser.SlotEvent;

/**
 * Holds a data item with value, sensor name, data type and timestamp and provides methods for
 * calculating and comparing delays.
 * <p>
 * The timestamp is used by {@link TTEQueue} for delaying.
 * 
 * @see java.util.concurrent.Delayed
 * @see venice.lib.parser.SlotEvent
 */
public class TTE extends SlotEvent implements Delayed {
	
	public TTE(SlotEvent slotEvent){
		this.label = slotEvent.getLabel();
		this.time = slotEvent.getTime();
		this.value = slotEvent.getValue();
		this.type = slotEvent.getType();
		this.namespace = slotEvent.getNamespace();
	}
	
	public TTE(Object value, String slot, Class<?> type, long time){
		super(value, "", slot, type, time);
    }
	
	public TTE(){
		super();
	}
	
    @Override
    public long getDelay(TimeUnit unit) {
    	
        long delay = time + VeniceHub.getReplayDelay() - System.currentTimeMillis();
        return unit.convert(delay, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        if (time < ((TTE) o).getTime()) {
            return -1;
        } else if (time > (((TTE) o).getTime())) {
            return 1;
        }
        return 0;
    }
    
    public SlotEvent toSlotEvent(){
    	return new SlotEvent(value, namespace, label, type, time);
    }
    
}
