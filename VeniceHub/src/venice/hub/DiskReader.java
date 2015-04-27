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
package venice.hub;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEBufferedReader;
import venice.hub.utils.TTEQueue;
import venice.lib.parser.SlotEvent;

/**
 * Reads data from disk.
 * <p>
 * Reads XIO lines of log file, convert them into {@link SlotEvent} objects and
 * puts them into the {@link TTEQueue}.
 * The actual reading from file and the parsing will be done by the
 * {@link TTEBufferedReader}.
 *
 * @see TTEBufferedReader
 * @see TTEQueue
 * 
 * @author Oliver Eickmeyer
 */
public class DiskReader extends VeniceReader {

	private static Logger logger;

	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(DiskReader.class);
	}

    private String filePath;
    private TTEBufferedReader tbfr;
	private SynchronousQueue<SlotEvent> syncQ;

	/**
	 * Gets the file name and starts the TTEBufferedReader.
	 */
    protected void initialize() {
    	filePath = Configuration.getInstance().getLogFilePath();
    	prepareTTEBufferedReader();
    }
    
    /**
     * Starts the TTEBufferedReader and connects to the SynchronousQueue, which will transmit the data.
     */
    private void prepareTTEBufferedReader(){
    	tbfr = new TTEBufferedReader(filePath);
    	Thread tbfrThread = new Thread(tbfr, "VH_TBR");
    	tbfrThread.start();
    	syncQ = tbfr.getSyncQ();
    }
    
	@Override
	/**
	 * The main loop. Reads data until deactivated.
	 */
	public void run() {
		SlotEvent slotEvent;
    	while(active){
    		if(TTEQueue.getInstance().size() < QUEUE_CAPACITY){
    			try {
    				slotEvent = read(); // get new data (or wait until there is something)
    				//logger.debug("read "+slotEvent);
					if(slotEvent != null) TTEQueue.getInstance().put(slotEvent); // put new data into queue
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    		else{
    			// Queue is full
    			try { Thread.sleep(10); } catch (InterruptedException e) {}
    		}

    		synchronized(this){
    	    	while(paused && active){
    	    		try{
    	    			// check eventually if this thread was commanded to stop
    	    			wait(VeniceHub.CHECK_IF_STOPPED_WHILE_PAUSED_INTERVAL);
    	    		}catch(Exception e){}
    	    	}
        	}
    	}

    	cleanUp();
    	VeniceHub.message("DiskReader finished");
        finished = true;
	}
    
    /**
     * Stops the TTEBufferedReader (and waits until it is finished).
     */
    protected void cleanUp(){
    	tbfr.stopThread();
    	// wait until TTEBufferedReader finishes
    	while(!tbfr.isFinished()) try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
    
    /**
     * Restart the reading from the beginning of the file.
     */
    public void reset(){
    	long ts1 = VeniceHub.getTimestampOf1stLine();
    	if(ts1 != venice.lib.parser.XIOParser.INVALID_TIMESTAMP){
    		VeniceHub.setReplayDelay(System.currentTimeMillis() - ts1);
    		tbfr.seek(ts1); // set TTEBufferedReader to the first line
    	}
		else tbfr.seek(0);
    }
        
    /**
     * Reads a SlotEvent from TTEBufferedReader over SynchronousQueue.
     * <p>
     * Will wait, if SynchronousQueue is empty, but not forever, so that events
     * like pausing, seeking or reseting will not be missed.
     */
    protected SlotEvent read(){
    	SlotEvent slotEvent = null;
    	try {
    		slotEvent = syncQ.poll(500L, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {}
    	logger.debug("read from disk: "+slotEvent);
    	return slotEvent;
    }
    
    /**
     * Gets the timestamp of the first event in the buffer of the disk reader.
     * If the buffer is empty, 0 is returned.
     * @return timestamp of first event in buffer, or 0 if the buffer is empty
     */
    public long getFirstTimestamp(){
    	return tbfr.getTTEBuffer().getFirstTimestamp();
    }
    
    /**
     * Gets the timestamp of the item on the threshold in the buffer of the
     * disk reader. If the buffer has less items then the threshold, the
     * timestamp of the last event is returned instead.
     * <p>
     * Example:<br>
     * If the capacity of the buffer is 1000 and the threshold
	 * is 100, than the timetamp of the 900th item is returned.
	 * But if the buffer holds only 700 items, then the timestamp of the
	 * 700th event is returned.
     * @return timestamp of the event on the threshold, or, if the buffer size
     * is smaller than the threshold, the timestamp of the last event
     */
    public long getThresholdTimestamp(){
    	return tbfr.getThresholdTimestamp();
    }
    
    /**
     * Gets the timestamp of the last event in the buffer of the disk reader.
     * If the buffer is empty, 0 is returned.
     * @return timestamp of last event in buffer, or 0 if the buffer is empty
     */
    public long getLastTimestamp(){
    	return tbfr.getTTEBuffer().getLastTimestamp();
    }
    
    /**
     * Gets the size of the buffer used by the disk reader.
     * @return size of buffer
     */
    public int getBufferSize(){
    	return tbfr.getTTEBuffer().size();
    }
    
    /**
     * Gets the maximum capacity of the buffer.
     * @return capacity of the buffer
     */
    public int getCAPACITY(){
    	return tbfr.getCAPACITY();
    }
    
    /**
     * Gets the threshold of the buffer.
     * @return threshold of the buffer
     */
    public int getTHRESHOLD(){
    	return tbfr.getTHRESHOLD();
    }
    
    /**
     * Seeks to a specific timestamp in the file.
     * 
     * @param timestamp timestamp to be seeked
     */
    public void seek(long timestamp){
    	tbfr.seek(timestamp);
    }
}
