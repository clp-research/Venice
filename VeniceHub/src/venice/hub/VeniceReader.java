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

import org.apache.log4j.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEQueue;
import venice.lib.parser.SlotEvent;
import venice.lib.parser.XIOParser;

/**
 * Reads data from source.
 * <p>
 * Reads data from source, converts it to a {@link SlotEvent} and putting them into the {@link TTEQueue}.
 * 
 * @see TTEQueue
 */
public abstract class VeniceReader implements Runnable{

	protected static Logger logger;

	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(VeniceReader.class);
	}
	
    protected boolean active;
    protected boolean finished;
    protected boolean paused;
	protected final int QUEUE_CAPACITY = Configuration.getInstance().getQueueCapacity();
	protected XIOParser parser;
	protected Configuration config;

	/**
	 * Constructor. Sets up basic fields and calls <code>preparations</code>.
	 */
    public VeniceReader(){
    	active = true;
    	finished = false;
    	paused = false;
    	config = Configuration.getInstance();
    	parser = VeniceHub.getPreferredXIOParser();
    	initialize();
    }
    
    /**
     * Prepare everything needed to read from source.
     * <p>
     * For example: A disk reader has to open file. A net reader has to create slots, and so on.
     */
    protected void initialize(){
    	// needs to be implemented by subclass
    }

    /**
     * Keeps the thread alive, until deactivated.
     */
    @Override
    public void run(){
        while(this.active){
            try {
            	synchronized(this){
            		wait(100L);
            	}
            } catch (InterruptedException ex) {
                // nothing
            }
            synchronized(this){
            	while(paused && active){
            		try {
            			// check eventually if this thread was commanded to stop
						wait(VeniceHub.CHECK_IF_STOPPED_WHILE_PAUSED_INTERVAL);
					} catch (InterruptedException e) {}
            	}
            }
        }
        VeniceHub.message("Reader finished");
        finished = true;
    }
	
	/**
	 * Reads a data item from the source.
	 * 
	 * @return a SlotEvent read from source.
	 */
	protected SlotEvent read(){
		// implemented by subclass
		return null;
	}
    
    /**
     * Resets the reader.
     * <p>
     * For example: A disk reader will restart at the beginning of the file.
     */
    public void reset(){
    	// implemented by subclass
    }
    
    /**
     * Called when this thread has to be stopped.
     */
    public void stopThread() {
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
    	paused = false;
    	notify();
    }
    
    /**
     * Clean up, so the Thread can end safely.
     * <p>
     * For example: A disk reader has to close the file.
     */
    protected void cleanUp(){
    	// needs to be implemented by subclass
    }

}
