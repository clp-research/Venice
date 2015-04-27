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
package venice.hub;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEQueue;
import venice.lib.parser.SlotEvent;
import venice.lib.parser.XIOParser;

/**
 * Abstract class for writing data from TTEQueue to a target.
 * <p>
 * Gets data from {@link TTEQueue} and writes it to the target. The target is specified by the subclass.
 * A subclass should be started as a Thread, like this:<br>
 * <code>
 * ANewWriter = new ANewWriter();<br>
 * ThreadForANewWriter = new Thread(ANewWriter, "VH_ANewWriter");<br>
 * ThreadForANewWriter.start();<br>
 * </code>
 *
 * @see TTEQueue
 * @see DiskWriter
 * @see IIOWriter
 */
public abstract class VeniceWriter implements Runnable{
	protected static Logger logger;
	
	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(VeniceWriter.class);
	}
	
    protected boolean active;
    protected boolean finished;
    protected boolean completed;
    protected boolean paused;
    protected XIOParser parser;
    protected Configuration config;
    protected long lastTimestamp;
    protected TTEQueue queue;
    protected final static long NO_TIMESTAMP = venice.lib.parser.XIOParser.INVALID_TIMESTAMP;
    protected final long WAIT_ON_EMPTY_QUEUE_TIMEOUT = 100L;
    protected boolean lagHistoryEnabled;
    protected int lag;
    protected ArrayList<lagPoint> lagList;
	protected int lagCounter;
	protected boolean initialized = false;
    
    /**
     * Constructor. Should not be overridden by a subclass.
     * A subclass should override <code>initialize</code> instead.
     */
    public VeniceWriter(){
    	active = true;
    	finished = false;
    	paused = false;
    	config = Configuration.getInstance();
    	parser = VeniceHub.getPreferredXIOParser();
    	
    	queue = TTEQueue.getInstance();
    	lagHistoryEnabled = VeniceHub.isLagHistoryEnabled();
    	lag = 0;
    	if(lagHistoryEnabled) lagList = new ArrayList<lagPoint>();
    	lagCounter = 1;
    	initialize();
    	initialized = true;
    }
    
    /**
     * Prepare everything needed to write to target. Will be called by the constructor, so an
     * implementing class should not override the constructor.
     * <p>
     * For example: A disk writer has to open file. A net writer has to create slots, and so on.
     */
    protected void initialize(){
    	// needs to be implemented by subclass
    }
    
    /**
     * Checks, if the writer is ready to work.
     * @return <code>true</code> if writer is ready to work, else <code>false</code>
     */
    public boolean isInitialized(){
    	return initialized;
    }
    
    /**
     * Called when this thread has to be stopped.
     */
    public void stopThread() {
    	logger.debug("got stopThread command");
    	active = false;
    }
    
    /**
     * Returns <code>true</code> if this Thread can be safely killed.
     * <p>
     * After the call of <code>stopThread</code> this Thread will try to get into a safe closing state.
     * For example every open file will be closed. 
     * @return <code>true</code> if it is safe to kill this thread
     */
    public boolean isFinished(){
    	return finished;
    }
    
    /**
     * Pauses the thread.
     * <p>
     * To end pause call <code>proceed</code>.
     */
    public void pause(){
    	paused = true;
    }
    
    /**
     * End pause.
     */
    public void proceed(){
    	logger.debug("unpaused");
    	paused = false;
    	notify();
    }
    
    @Override
    /**
     * Starts the writer thread.
     * <p>
     * This is the main part. It polls TTE items from the TTEQueue and writes them to the target.
     */
    public void run(){
    	logger.debug("running");
    	SlotEvent slotEvent = null;
    	long writingTime = NO_TIMESTAMP;
    	while (active) {
			try {
				slotEvent = queue.poll(WAIT_ON_EMPTY_QUEUE_TIMEOUT, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// do nothing, if interrupted
			} 
			if(slotEvent != null){
				//logger.debug("writing "+slotEvent);
				writingTime = write(slotEvent);
				if(writingTime != NO_TIMESTAMP){
					completed = false;
					calculateLag(writingTime, slotEvent);
				}
			}
			else{
				if(config.getQuitIfIdle() > 0 &&
				   VeniceHub.getLastActivity() != NO_TIMESTAMP && 
				   System.currentTimeMillis() - VeniceHub.getLastActivity() > config.getQuitIfIdle()){
					// if quitIfIdle is set, check for idle time
					logger.info("quitting because of exceeding maximum idle time");
					VeniceHub.quit();
				}
				if(!completed && lastTimestamp != NO_TIMESTAMP){
					if(lastTimestamp == VeniceHub.getTimestampOfLastLine()){
						// played last line, so replay is completed
						VeniceHub.message("replay completed");
						completed = true;
					}
				}
			}
			synchronized(this){
				while(paused && active){
					try {
						// check eventually if this thread was commanded to stop
						wait(VeniceHub.CHECK_IF_STOPPED_WHILE_PAUSED_INTERVAL);
					} catch (InterruptedException e) {
						// do nothing, if interrupted while waiting
					}
				}
			}
        }
    	cleanUp();
    	VeniceHub.message("VeniceWriter finished");
        finished = true;
    }
    
    /**
     * Writes a TTE object to target.
     * 
     * @return long The time (in milliseconds), when the TTE was written to the target. 
     */
    protected long write(SlotEvent slotEvent){
    	// implemented by subclass
    	
    	// important:
    	//  - store timestamp of last TTE written in lastTimestamp
    	//  - send timestamp of last written TTE to VeniceHub
    	//    with VeniceHub.setLastPushedTimestamp(lastTimestamp)
    	//    so the TTEBufferedReader can shift the buffer properly
    	return NO_TIMESTAMP;
    }
    
    /**
     * Clean up, so the Thread can end safely.
     * <p>
     * For example: A disk writer has to close the file.
     */
    protected void cleanUp(){
    	// needs to be implemented by subclass
    }
    
    /**
     * Contains the lag at a specific time.
     * <p>
     * Used for building a history of lags while writing data to target.
     */
	protected class lagPoint{
		public int lag;
		public long timestamp;
		public lagPoint(int lag, long timestamp){
			this.lag = lag;
			this.timestamp = timestamp;
		}
		public String toString(){
			return String.valueOf(timestamp)+", "+String.valueOf(lag);
		}
	}
    
	/**
	 * Calculates the lag of writing TTE objects to target.
	 * <p>
	 * Compares the time, when TTE objects was written with the system time, modified by replay delay.
	 * 
	 * @param writingTime The time when the last TTE was written
	 * @param e The written TTE
	 */
    private void calculateLag(long writingTime, SlotEvent e){
    	//long time = tte.getTime();
    	long time = lastTimestamp;
    	if(lagCounter++ >= VeniceHub.getLagLogN()){
        	lag = (int) (writingTime - (time + VeniceHub.getReplayDelay()));
        	if(lagHistoryEnabled) lagList.add( new lagPoint( lag, time ));
        	lagCounter = 1;
    	}
    }
    
    /**
     * Saves lag history to file.
     * 
     * @param filename
     */
    public void saveLag(String filename){
	   	long ts0 = VeniceHub.getTimestampOf1stLine();
	   	try (Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "utf-8"))) {
	   		writer.write("# lag history; format: timestamp[ms] lag[ms]\n");
	   		for(lagPoint lp: lagList){
					writer.write(lp.timestamp-ts0+"\t"+lp.lag+"\n");
	   		}
	   		writer.close();
	   		System.out.println("...saved");
	   	} catch (IOException ex){
	   		VeniceHub.message("Error while writing into lagFile");
	   	} 
   }
    
}
