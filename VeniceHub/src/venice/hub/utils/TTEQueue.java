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

import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

import venice.hub.VeniceHub;
import venice.lib.parser.SlotEvent;

/**
 * A singleton queue that stores data read by {@link venice.hub.VeniceReader} objects
 * and provide it for {@link venice.hub.VeniceWriter} objects,
 * with delaying items by use of the timestamp.
 * <p>
 * The data have to be {@link TTE} items. They will be provided in the order of their timestamp.
 * 
 * @see java.util.concurrent.DelayQueue
 * @see TTE
 * 
 */
public class TTEQueue {

	/**
	 * singleton constructor
	 */
    private static TTEQueue instance = new TTEQueue();

    private BlockingQueue<TTE> queue = new DelayQueue<TTE>();

    /**
     * The constructor cannot be called externaly due to the fact that this is to be used as a singleton.
     */
    private TTEQueue() {
    }

    /**
     * Static method that returns the only instance of this class
     */
    public static TTEQueue getInstance() {
        return instance;
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary for
     * space to become available.
     * 
     * @param e TTE object to put on queue.     
     * @throws InterruptedException if interrupted while waiting 
     */
    public void put(SlotEvent e) throws InterruptedException {
        queue.put(new TTE(e));
    }

    public TTE peek(){
        return queue.peek();
    }
    
    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    public TTE take() throws InterruptedException {
        return queue.take();
    }

    public TTE poll() {
        return queue.poll();
    }
    
    public TTE poll(long timeout, TimeUnit unit) throws InterruptedException{
        return queue.poll(timeout, unit);
    }
    
    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        if (queue.size() == 0) {
            return true;
        }
        return false;
    }
    
    /**
     * For debugging: Shows the actual content of the DelayQueue with DiskTime.
     */
    public void showContent(){
    	//XIOParser parser = new XIOParser();
    	TTE tte;
    	Iterator<TTE> i = queue.iterator();
    	long maxTime = 0;
    	long minTime = 0;
    	while(i.hasNext()){
    		tte = i.next();
    		if(tte.getTime()>maxTime) maxTime = tte.getTime();
    		if(minTime==0) minTime = tte.getTime();
    		else if(tte.getTime() < minTime) minTime = tte.getTime();
    	}
    	int qsize = size();
    	long now = System.currentTimeMillis();
    	long dlay = VeniceHub.getReplayDelay();
    	VeniceHub.message("size of TTEBuffer="+qsize);
    	VeniceHub.message("Timestamp           raw   delayOffset   plus offset     minus now");
    	VeniceHub.message("---------+-------------+-------------+-------------+-------------+");
    	VeniceHub.message(String.format("largest   %13d %13d %13d %13d", maxTime, dlay, maxTime+dlay, maxTime+dlay-now));
    	VeniceHub.message(String.format("smallest  %13d %13d %13d %13d", minTime, dlay, minTime+dlay, minTime+dlay-now));
    }
    
    /**
     * Removes all elements of the DelayQueue with DiskTime less upperDiskTime.
     * Used with the seek command, to jump forward in the replay file.
     * @param upperTime All elements with DiskTime less than upperDiskTime will be removed.
     */
    public void removeUntil(long upperTime){
    	TTE tte;
    	Iterator<TTE> i = queue.iterator();
    	while(i.hasNext()){
    		tte = i.next();
    		if(tte.getTime()<upperTime){
    			queue.remove(tte);
    		}
    	}    	
    }
    
    public void reset(){
    	queue = new DelayQueue<TTE>();
    }
}
