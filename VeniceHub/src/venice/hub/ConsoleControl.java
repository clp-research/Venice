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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.apache.log4j.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEQueue;
import venice.hub.utils.Configuration.Connection;
import venice.lib.parser.XIOMaps;

/**
 * Reads user input from console, parses commands and calls appropriate
 * methods from VeniceHub.
 *
 */
public class ConsoleControl extends VeniceControl{
	private static Logger logger;
	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(ConsoleControl.class);
	}
	
	/**
	 * Constructs the console control.
	 */
	public ConsoleControl(){
		super();
	}

	/**
	 * Constantly checks the console for new input. Will call the parser
	 * if the input is a complete line.
	 */
	@Override
	public void run() {
		String input="";
    	
    	// for non-blocking input
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    	StringBuilder sb = new StringBuilder();
    	
    	VeniceHub.message("please enter 'q' to exit ('h' for help)");
    	while(active){
    		// for non-blocking input
    		
    		try {
				if(br.ready()){
					char c = (char) br.read();
					VeniceHub.updateLastActivity();
					if(c == 10 || c == 13){
						input = sb.toString();
						processInput(input);
						sb = new StringBuilder();
					}
					else{
						sb.append(c);
					}
				}
				else{
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// nothing
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} // end while
    	
    	try {
			br.close();
		} catch (IOException e) {
			logger.error("can't close BufferedReader");
		}
    	logger.info("finished");
    	finished = true;
		
	}
	
    /**
     * Called by VeniceHub when this thread has to be stopped.
     */
	@Override
    public void stopThread() {
		logger.debug("got stop command");
    	active = false;
    }
	
	/**
	 * Processes the console input and calls the appropriate
	 * method from VeniceHub, if it's a valid command.
	 * @param input The console input string
	 */
	private void processInput(String input){
		logger.debug("parsing "+input);
		input = input.trim();

		if(input.equals("q")){
			VeniceHub.quit();
			return;
		}
		
		if(input.startsWith("h")){
			System.out.println("Use following commands (and press 'Enter'):");
			System.out.println(" Commands for all modes:");
			System.out.println("  h : show help");
			System.out.println("  q : quit the programm");
			System.out.println(" msg: turn on/off messages");
			System.out.println(" Commands for replaying from Disk:");
			System.out.println("  p                   : (un)pause");
			System.out.println("  seek <timestamp>    : jump to timestamp");
			System.out.println("  skip <time> <unit>  : jump an amount of time");
			System.out.println("  time                : shows time of actual timestamp in h, m, s");
			System.out.println("  reset               : restart the replay");
			System.out.println("  offset [<ms>]       : show [or set] offset value for replay");
		}
		
		if(input.equals("config")){
			Configuration.getInstance().printConfig();
		}
		
		if(input.equals("queue")){
			TTEQueue.getInstance().showContent();
		}
		
		if(input.equals("p")){
			// pause (only for replaying from disk)
			if(Configuration.getInstance().getSource() == Connection.DISK ){
				VeniceHub.switchPause();
			}
			else{
				logger.warn("pausing is only allowed if reading from disk");
			}
		}
		if(input.equals("xio")){
			VeniceHub.message("defined xio codes:");
			for(Map.Entry<Class<?>, String> me : XIOMaps.getClass2strMap().entrySet()){
				VeniceHub.message(me.getKey()+" - "+me.getValue());
			}
			for(Map.Entry<String, Class<?>> me : XIOMaps.getStr2classMap().entrySet()){
				VeniceHub.message(me.getKey()+" - "+me.getValue());
			}
		}
		if( Configuration.getInstance().getSource() == Connection.DISK ){
    		if(input.startsWith("offset")){
    			if(input.split(" ").length == 1){
    				VeniceHub.message("Offset: "+VeniceHub.getReplayOffset());
    			}
    			else if(input.split(" ").length == 2){
    				try{
    					long newStartOffset = Long.parseLong( input.split(" ")[1] );
    					VeniceHub.message("Offset set to "+newStartOffset);
    					VeniceHub.setReplayOffset(newStartOffset);
    				}
    				catch(NumberFormatException e){
    					VeniceHub.message("Wrong number format for this command.");
    				}
    			}
    		}
    		if(input.startsWith("seek")){
    			if(input.split(" ").length > 1){
        			try{
        				long seekTime = Long.parseLong( input.split(" ")[1] );
        				VeniceHub.seekForRelativePosition(seekTime);
    				}
        			catch(NumberFormatException e){
        				VeniceHub.message("Wrong number format for seek command.");
        			}
    			}
    			else VeniceHub.message("A timestamp is needed for the seek command. Usage: seek <timestamp>");
    		}
    		if(input.startsWith("skip")){
    			if(input.split(" ").length > 1){
    				try{
    					long jumpTime = Long.parseLong( input.split(" ")[1] );
    					if(input.split(" ").length > 2){
    						if(input.split(" ")[2].equals("s")) jumpTime *= 1000;
    						else if(input.split(" ")[2].equals("m")) jumpTime *= 60000;
    						else if(input.split(" ")[2].equals("h")) jumpTime *= 3600000;
    						else if(!input.split(" ")[2].equals("ms")){
    							VeniceHub.message("Don't know this time unit. Use ms, s, m or h.\n Default is ms, if you omit the time unit.");
    							jumpTime = 0;
    						}
    					}
    					if(jumpTime != 0){
        					//long seekTime = namespcWriter.getLastTTEwritten().getTime()
        					//			  + jumpTime;
        					long seekTime = VeniceHub.getLastPushedTimestamp() + jumpTime;
        					VeniceHub.seekForTimestamp(seekTime);
    					}
    				}
    				catch(NumberFormatException e){
    					VeniceHub.message("Wrong number format for skip command.");
    				}
    			}
    			else VeniceHub.message("A milliseconds value is needed for the skip command.\n Usage: skip <milliseconds> (<unit>)");
    		}
    		if(input.equals("time")){
    			long value = VeniceHub.getLastPushedTimestamp();
    			if(value != VeniceHub.INVALID_TIMESTAMP){
    				value += -VeniceHub.getTimestampOf1stLine() + VeniceHub.getReplayOffset();
    				VeniceHub.message(VeniceHub.giveNiceTimeString(value));
    			}
    			else VeniceHub.message("no data written");
    		}
    		if(input.equals("reset")){
    			VeniceHub.message("reseting");
    			VeniceHub.reset();
    		}
    		if(input.equals("buffer")){
    			VeniceHub.showBuffer();
    		}
    		if(input.equals("0")){
    			VeniceHub.showLastWrittenTS();
    		}
    		if(input.equals("1")){
    			VeniceHub.showTSOfFirstLine();
    		}
    		if(input.equals("last")){
    			VeniceHub.message("Timestamp of last line: "+VeniceHub.getTimestampOfLastLine());
    		}
    		if(input.startsWith("savelag")){
				VeniceHub.saveLagHistory();
    		}
    		if(input.equals("msg")){
    			VeniceHub.switchMsg();
    		}
		} // end if
	}
	
	@Override
	public String toString(){
		return "ConsoleControl";
	}
}
