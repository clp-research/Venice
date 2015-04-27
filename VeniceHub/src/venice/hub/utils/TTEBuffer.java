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

import java.util.ArrayList;

import venice.lib.parser.SlotEvent;

/**
 * A buffer for {@link TTE} objects with random access, to enable fast seeking in log files.
 * 
 * The {@link TTEBufferedReader} fills it with data from disk, writing this data on top,
 * the {@link venice.hub.DiskReader} reads data from a position that could be anywhere (normally
 * the DiskReader is reading slower than the TTEBufferedReader is filling)
 */
public class TTEBuffer extends ArrayList<SlotEvent> {
	
	private static final long serialVersionUID = 1L;
	private int readingPosition; // reading position
	
	/**
	 * The overridden constructor just initializes the reading position after calling the superior constructor. 
	 */
	public TTEBuffer(){
		super();
		readingPosition = 0;
	}
	
	/**
     * Gets the timestamp of the first event in the buffer of the disk reader.
     * If the buffer is empty, 0 is returned.
     * @return timestamp of first event in buffer, or 0 if the buffer is empty
     */
	public long getFirstTimestamp(){
		if(size()>0) return get(0).getTime(); 
		else return 0;
	}
	
	/**
     * Gets the timestamp of the last event in the buffer of the disk reader.
     * If the buffer is empty, 0 is returned.
     * @return timestamp of last event in buffer, or 0 if the buffer is empty
     */
	public long getLastTimestamp(){
		// returns the timestamp of the last element in the buffer
		if(size()>0) return get(size()-1).getTime();
		else return 0;
	}
	
	/**
	 * Removes all items with timestamp equal or less than the given
	 * timestamp.
	 * @param timestamp
	 * @return the number of removed items
	 */
	public int removeUntil(long timestamp){
		int count = 0;
		while(count < size() && get(count).getTime() <= timestamp) count++;
		if(count > 0) removeRange(0, count);
		return count;
	}

	/**
	 * Returns the reading position inside of the buffer.
	 * @return the reading position inside of the buffer
	 */
	public int getReadingPosition(){
		return readingPosition;
	}
	
	/**
	 * Sets the reading position inside of the buffer.
	 * @param rp the new reading position
	 */
	public void setReadingPosition(int rp){
		readingPosition = rp;
	}
	
	/**
	 * Add a value to the reading position.
	 * @param delta the value to be added to the reading position
	 */
	public void addReadingPosition(int delta){
		readingPosition += delta;
	}
	
	/**
	 * A reader uses this method to demand a new TTE item.
	 * It will get <code>null</code>, when there is no TTE item available.
	 * @return TTE object or <code>null</code> if there is no TTE available.
	 */
	public SlotEvent getNext(){
		SlotEvent slotEvent = null;
		
		if(readingPosition < size()) slotEvent = get(readingPosition++);
		
		return slotEvent;
	}
	
	/**
	 * Peeking if there is an slotEvent available at the reading position, without removing it.
	 * If there is no slotEvent available, <code>null</code> will be returned instead.
	 * 
	 * @return slotEvent available at the reading position; <code>null</code>
	 * if there is no slotEvent
	 */
	public SlotEvent peek(){
		SlotEvent slotEvent = null;
		if(readingPosition < size()) slotEvent = get(readingPosition);
		return slotEvent;
	}
}
