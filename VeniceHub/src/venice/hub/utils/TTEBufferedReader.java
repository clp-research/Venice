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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

import venice.hub.VeniceHub;
import venice.lib.parser.SlotEvent;
import venice.lib.parser.XIOParser;

import static venice.lib.parser.XIOParser.INVALID_TIMESTAMP;

/**
 * Doing the real work for the {@link venice.hub.DiskReader}, reads data from file,
 * parses it into {@link TTE} items and caches them, until {@link venice.hub.DiskReader} will take them.
 * Data item will be provided over a {@link SynchronousQueue}.
 *
 * @see venice.hub.DiskReader
 */
public class TTEBufferedReader implements Runnable{
	private static Logger logger;

	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(TTEBufferedReader.class);
	}
	
	private final int CAPACITY  = Configuration.getInstance().getBufferCapacity(); // number of TTE objects
	private final int THRESHOLD =  Configuration.getInstance().getBufferThreshold(); // Threshold to shift the buffer before reaching it's end
	private final long MINIMUM_SKIP_AMOUNT = Configuration.getInstance().getBufferMinimumSkipAmount(); // if the estimated amount of bytes to skip is less, then stop skipping
	
	private InputStream inStream;
	private TTEBuffer buffer;
	private boolean active;
	private int headerLines;
	private String filePath;
	private boolean endOfFile;
	private FileInputStream fileInputStream;
	private XIOParser parse;
	private long avgBytesPerS; // for approximation of bytes per second
	private double toSmallProgress = Configuration.getInstance().getToSmallProgress();
	private double bigEnoughProgress = Configuration.getInstance().getBigEnoughProgress();
	private enum tasks {FILL, SEEK};
	private tasks task;
	private long seekedTimestamp;
	private boolean processingSeekRequest;
	private long lastSeekRequest;
	private boolean fastSeekingEnabled;
	private SynchronousQueue<SlotEvent> syncQ;
	private boolean finished;
	
	/**
	 * The constructor initializes variables, the buffer and the input file.
	 * @param fp filepath of the log file
	 */
	public TTEBufferedReader(String fp){
		finished = false;
		syncQ = new SynchronousQueue<SlotEvent>();
		lastSeekRequest = INVALID_TIMESTAMP;
		processingSeekRequest = false;
		seekedTimestamp = INVALID_TIMESTAMP;
		task = tasks.FILL;
		parse = VeniceHub.getPreferredXIOParser();
		filePath = fp;
		buffer = new TTEBuffer();
		buffer.ensureCapacity(CAPACITY);
		headerLines = Configuration.getInstance().getHeaderLines();
		avgBytesPerS = 0;
		fastSeekingEnabled = true;
		
        initStream(filePath);
        estimateAvgBytesPerS();
        fileReset();
	}
	
	/**
	 * Reads a line from file stream.
	 * <p>
	 * Note on the use of {@link StringBuilder} instead of {@link StringBuffer}:
	 * StringBuilder provides an API compatible with StringBuffer, but with no guarantee of synchronization.
	 * StringBuilder is designed for use as a drop-in replacement for StringBuffer in places where the
	 * string buffer was being used by a single thread (as is the case with TTEBufferedReader).
	 * Where possible, it is recommended that StringBuilder be used in preference to StringBuffer as it will
	 * be faster under most implementations. 
	 * @param inBuffer in this buffer the line will be stored
	 */
	private void readLineFromStream(StringBuilder inBuffer){
		int inByte = 0;
		boolean endOfLine = false;
		boolean error = false;
		
		while(!endOfLine && !endOfFile)
		{
			try {
				inByte = inStream.read();
			} catch (IOException | NullPointerException e) {
				if(e instanceof NullPointerException) VeniceHub.message("can not read from stream, NullPointerException");
				else if(e instanceof IOException); //VeniceHub.message("can not read from stream, IOException");
				else VeniceHub.message("can not read from stream, don't know why");
				error = true;
			}
			if(!error){
				if(inByte == '\n' || inByte == -1){
					endOfLine = true;
				}
				if(inByte == -1){
					endOfFile = true;
				}
				if(!endOfLine && !endOfFile){
					inBuffer.append((char)inByte);
				}
			}
		}
	}
	
	/**
	 * Runs a loop for reading data from file.
	 * Will also do the slow skipping.
	 * Makes a lot of condition checks (pausing, slow seeking, end of file, stopping thread, ...).
	 */
	public void run(){
		active = true; // thread is active
		int lineCounter = 1; // counts the read lines from the file
		SlotEvent slotEvent; // used to store a TTE object parsed from a line from file
		StringBuilder inBuffer; // used for reading a line from file
		String s = ""; // used for reading a line from file
		long preparsedTimestamp; // store a preparsed Timestamp from a line from file
		
		while(active){
			lineCounter = 0;
			endOfFile = false;
			while(!endOfFile && active){
				switch(task){
				case FILL:
					if(buffer.peek() != null){
						// if new data is prepared on readingposition
						try {
							// try to give data from readingposition to DiskReader
							// will wait until DiskReader has received the new Data
							boolean suc = syncQ.offer(buffer.peek(), 500L, TimeUnit.MILLISECONDS);
							if(suc){
								buffer.getNext();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if(buffer.size() < CAPACITY){ // if buffer is not full
						inBuffer = new StringBuilder();
						readLineFromStream(inBuffer);
						s = inBuffer.toString();
						if(s.length() > 0){
							lineCounter++;
				    		if (lineCounter > headerLines) {
				    			slotEvent = parse.stringToEvent(s); // try to parse the line into a TTE
				    			if(slotEvent.getTime() > -1){
		                    		buffer.add(slotEvent); // put the parsed TTE in the TTEBuffer
				    			}
				    		}
						}
					}
					else{ // if buffer is full 
						checkForReachingBufferThreshold();
					}
					break;
				case SEEK:
					inBuffer = new StringBuilder();
					readLineFromStream(inBuffer);
					s = inBuffer.toString();
					if(s.length() > 0){
						lineCounter++;
						if(lineCounter > headerLines){
							preparsedTimestamp = preparseTimestamp(s);
							if(preparsedTimestamp != INVALID_TIMESTAMP && preparsedTimestamp >= seekedTimestamp){
								slotEvent = parse.stringToEvent(s); // try to parse line into event
				    			if(slotEvent.getTime() != INVALID_TIMESTAMP){
		                    		buffer.add(slotEvent); // put the parsed TTE in the TTEBuffer
				    			}
				    			task = tasks.FILL; // switch from SEEK to FILL mode, because seeked timestamp is found
							}
						}
					}
					break;
				}
				if(lastSeekRequest != INVALID_TIMESTAMP){
					processSeek();
				}
			} // while not eof
			if(lastSeekRequest != INVALID_TIMESTAMP){
				processSeek();
			}
			if(endOfFile){
				//VeniceHub.message("EOF, waiting for the need of rereading the file");
				VeniceHub.setTimestampOfLastLine(buffer.getLastTimestamp());
			}
			while(lastSeekRequest == INVALID_TIMESTAMP && active){
				// wait for a seek command
				if(buffer.peek() != null){
					// if new data is prepared on readingposition
					try {
						// try to give data from readingposition to DiskReader
						// the put method will wait until DiskReader has received the new Data
						syncQ.offer(buffer.getNext(), 500L, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				else nap(1);
			}
			if(active) processSeek(); // process the seek command before restarting the loop
		} // while active
		
        try {
			inStream.close();
			VeniceHub.message("TTEBufferedReader closed replay file.");
		} catch (IOException e) {
			VeniceHub.message("Warning: TTEBufferedReader failed to close replay file.");
			e.printStackTrace();
		}
        VeniceHub.message("TTEBufferedReader finished");
        finished = true;
	}

	/**
	 * Will be called by VeniceHub to end this Thread.
	 */
	public void stopThread(){
		active = false;
	}
	
	/**
	 * With this method {@link venice.hub.VeniceHub} can check if this thread
	 * has finished, after a call of <code>stopThread</code>.
	 * 
	 * @return <code>true</code> if this thread is ready to be terminated;
	 * <code>false</code> if not.
	 */
	public boolean isFinished(){
		return finished;
	}
	
	/**
	 * Initializes the inputstream, including open the replay file.
	 * @param filePath The name and path of the log file.
	 */
	private void initStream(String filePath){
		// initializes the inputstream for the file to be read
		// can be compressed (GZIP) or non-compressed
		
		byte[] zipMagic  = new byte[] {31, -117}; // 2 Bytes to recognize compression
		byte[] fileMagic = new byte[2]; // 2 Bytes to be read for comparison
		
		try {
			fileInputStream = new FileInputStream(filePath);
			fileInputStream.read(fileMagic); // read 2 bytes
			//fs.reset(); // don't work, not supported
			fileInputStream.close(); // for reseting, otherwise, two bytes are lost
			
			fileInputStream = new FileInputStream(filePath); // reopen, to reset reading position
			
			// should we read compressed input?
			if (Arrays.equals(fileMagic, zipMagic)) {
				// compressed
				inStream = new GZIPInputStream(fileInputStream);
			} else {
				// non-compressed
				inStream = new BufferedInputStream(fileInputStream);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Induces the seek for a specific timestamp in the file.
	 * 
	 * @param timestampToSeek the timestamp to seek
	 */
	public void seek(long timestampToSeek){
		// just note the timestamp for further processing
		// the seeking will be done by processSeek
		lastSeekRequest = timestampToSeek;
		logger.debug("seek request for "+lastSeekRequest);
	}
	
	/**
	 * Seeks for a specific timestamp in the file.
	 * It will try to use fast seeking if possible.
	 */
	private void processSeek(){
		// will process any seek command

		if(processingSeekRequest){
			// if already seeking:
			// ignore the seek command for the moment
			VeniceHub.message("ignoring seek request for the moment, because already seeking");
			return;
		}
		
		processingSeekRequest = true; // so that a seek is not interrupted by another seek command
		
		long timestampToSeek = lastSeekRequest;
		
		logger.debug("start seeking for "+timestampToSeek);
		
		long timestampOfFilePosition = INVALID_TIMESTAMP; // the timestamp of the actual reading position in the file
		
		boolean insideBuffer = false;
		
		if(buffer.size() > 0){
			if(timestampToSeek >= buffer.getFirstTimestamp() && timestampToSeek <= buffer.getLastTimestamp()){
				insideBuffer = true;
			}
		}
		if(insideBuffer){
			// seeked timestamp is inside the buffer (or fast seeking is disabled)
			//VeniceHub.message("inside buffer");
			int n=0;
			long timestamp = INVALID_TIMESTAMP;
			do{
				timestamp = buffer.get(n++).getTime();
			}while(timestamp < timestampToSeek && n < buffer.size());
			buffer.setReadingPosition(--n);
		}
		else{
			// seeked timestamp is outside the buffer
			//VeniceHub.message("outside buffer");
			if(fastSeekingEnabled){
				buffer.setReadingPosition(0); // because the buffer will be cleared in both of the following if conditions
				if(timestampToSeek <= VeniceHub.getTimestampOf1stLine()){
					// the easiest case: just start at the beginning of the file, no skipping necessary
					fileReset();
					buffer = new TTEBuffer();
				}
				else{
					timestampOfFilePosition = buffer.getLastTimestamp(); // the last timestamp in buffer is also the timestamp of the actual file position
					buffer = new TTEBuffer();
					if(timestampToSeek < timestampOfFilePosition || timestampOfFilePosition == INVALID_TIMESTAMP){
						// skipping backwards is not possible, so a start from beginning is necessary
						fileReset();
						timestampOfFilePosition = VeniceHub.getTimestampOf1stLine();
					}
					int skipIterationCounter = 0;
					long skipAmount = 0; // bytes
					boolean keepOnSkipping = true;
					StringBuilder inBuffer;
					String s; // for the line read from file
					double globalProgress, lokalProgress;
					long initialTimestampForSkipping = timestampOfFilePosition; // for global progress calculation
					long formerTimestampForSkipping; // for lokal progress calculation
					long bps = avgBytesPerS;
					while(keepOnSkipping){
						skipAmount = bps * (timestampToSeek-timestampOfFilePosition) / 1000;
						if(skipAmount >= MINIMUM_SKIP_AMOUNT || skipIterationCounter==0){
							formerTimestampForSkipping = timestampOfFilePosition;
							VeniceHub.message("skip no. "+skipIterationCounter+": "+skipAmount+" bytes of data (avgBytePerS="+bps+")");
							try {
								inStream.skip(skipAmount);
								endOfFile = false;
								timestampOfFilePosition = INVALID_TIMESTAMP;
								while(!endOfFile && timestampOfFilePosition == INVALID_TIMESTAMP){
									// its necessary to read more than one line, when the timestamp could not be found
									// this is possible, because a skip mostly leads into a middle position of a line
									// and not at the beginning
									inBuffer = new StringBuilder();
									
									readLineFromStream(inBuffer);
									s = inBuffer.toString();
									timestampOfFilePosition = preparseTimestamp(s);
								}
								if(endOfFile || timestampOfFilePosition == INVALID_TIMESTAMP) keepOnSkipping = false;
								if(timestampOfFilePosition != INVALID_TIMESTAMP){
									if(timestampOfFilePosition >= timestampToSeek){
										// success
										keepOnSkipping = false;
									}
									else{
										// still not reached target timestamp
										globalProgress = (double)(timestampOfFilePosition-initialTimestampForSkipping)/(timestampToSeek-initialTimestampForSkipping);
										lokalProgress = (double)(timestampOfFilePosition-formerTimestampForSkipping)/(timestampToSeek-formerTimestampForSkipping);
										VeniceHub.message(String.format("Progress of last skip: %.2f overall (lokal %.2f)", globalProgress, lokalProgress));
										if(lokalProgress < toSmallProgress) lokalProgress = toSmallProgress;
										if(lokalProgress < bigEnoughProgress) bps = (long)((double)bps / lokalProgress);
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
								keepOnSkipping = false;
							}
						}
						else{
							// skipAmount is to small, so seek with the (slower) SEEK-task in run-loop
							task = tasks.SEEK;
							seekedTimestamp = timestampToSeek;
							keepOnSkipping = false;
						}
						skipIterationCounter++;
					}
				}
			} // endif fastSeekingEnabled
			else{
				// if fast seeking is NOT enabled
				// can't use file skipping, have to use the slower SEEK task
				task = tasks.SEEK;
				seekedTimestamp = timestampToSeek;
				if(buffer.size() > 0){
					timestampOfFilePosition = buffer.getLastTimestamp(); // the last timestamp in buffer is also the timestamp of the actual file position
					if(timestampToSeek < timestampOfFilePosition){
						fileReset();
						buffer = new TTEBuffer();
						buffer.setReadingPosition(0);
					}
				}
				else{
					// if the buffer is empty
					fileReset(); // start seeking from the beginning
				}
			}
		}

		lastSeekRequest = INVALID_TIMESTAMP;
		processingSeekRequest = false;
		logger.debug("finished seeking");
	}
	
	/**
	 * Closes the replay file and opens it again, including
	 * re-establishing the inputstreams.
	 */
	private void fileReset(){
		// close file
        try {
			inStream.close();
		} catch (IOException e) {
			VeniceHub.message("Warning: Failed to close InputStream.");
			e.printStackTrace();
		}
        
        try {
			fileInputStream.close();
		} catch (IOException e) {
			VeniceHub.message("Warning: Failed to close FileInputStream.");
			e.printStackTrace();
		}
        
        // open file again
        initStream(filePath);
	}
	
	/**
	 * If the last pushed timestamp is greater than the threshold-timestamp the buffer will get shifted.
	 */
	private void checkForReachingBufferThreshold(){
		if(buffer.size() >= CAPACITY){
			long timestamp = VeniceHub.getLastPushedTimestamp();
			if(timestamp != INVALID_TIMESTAMP && timestamp >= getThresholdTimestamp()){
        		VeniceHub.message("shifting buffer to "+timestamp);
            	buffer.addReadingPosition(-buffer.removeUntil( timestamp ));
            	if(buffer.getReadingPosition() < 0) buffer.setReadingPosition(0);
			}
		}
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
		long result = buffer.getLastTimestamp();
		if(THRESHOLD < buffer.size()) result = buffer.get(buffer.size()-THRESHOLD).getTime();
		return result;
	}
	
	/**
	 * Parse the timestamp from a XIO line style string. Used to accelerate seeking.
	 * 
	 * @param s String to be parsed
	 */
	private long preparseTimestamp(String s){
		return parse.preparseTS(s);
	}
	
	/**
	 * Estimates the average Bytes per Second 
	 * by counting bytes for each timestamp and divide them by the difference of the timestamps.
	 * Will read a number of lines defined in {@link Configuration}.
	 */
	public void estimateAvgBytesPerS(){
		long firstTS = VeniceHub.getTimestampOf1stLine();
		long byteCounter = 0; // counted bytes
		StringBuilder inBuffer = new StringBuilder(); // for storing a line from file 
		String s; // for storing a line from file
		endOfFile = false; // for EOF-Detection
		long timestamp = INVALID_TIMESTAMP; // the parsed timestamp from the line read from file
		int lineCount = 0; // count the lines read
		int lineCountMax = Configuration.getInstance().getNumOfLinesToEstBPSFromReplay(); // how many lines to estimate BPS?
		int headerLines = Configuration.getInstance().getHeaderLines();
		
		while(!endOfFile && lineCount < lineCountMax){
			inBuffer = new StringBuilder(); // prepare StringBuffer for reading a line
			readLineFromStream(inBuffer); // read a line from file and check if EOF
			s = inBuffer.toString(); // convert StringBuffer in a String
			
			if(s.length() > 0 && lineCount >= headerLines){
				timestamp = preparseTimestamp(s); // use the fast preparser to get the timestamp from the line
				if(timestamp != INVALID_TIMESTAMP){
					// if the line contains a valid timestamp 
					
					// if it is the 1st line, store it
					if(VeniceHub.getTimestampOf1stLine() == INVALID_TIMESTAMP){
						VeniceHub.setTimestampOf1stLine( timestamp ); // for global purposes
						firstTS = timestamp; // for this local estimation
					}
	                if(VeniceHub.getReplayDelay() == 0){
	                	VeniceHub.setReplayDelay(System.currentTimeMillis() - timestamp);
	                }
	
					byteCounter += s.length(); // count the bytes of this 1st line (start value)
				}
			}
			lineCount++; // count lines for checking reaching maximum number of lines
		}
		
		if(endOfFile) System.err.println("Warning: While estimating bytes per s, end of file reached.");
		
		long timeDiff = (timestamp - firstTS); // the timedifference
		if(timeDiff > 0){
			avgBytesPerS = byteCounter * 1000 / timeDiff;
		}
		else{
			// if timedifference is zero, then we have a problem...
			System.err.println("Could not estimate bytes per s! Fast seeking disabled.");
			avgBytesPerS = 1;
			fastSeekingEnabled = false;
		}
		VeniceHub.message("avgBytesPerS="+avgBytesPerS);
	}
	
	/**
	 * Causes the thread to sleep.
	 * 
	 * @param millis The number of milliseconds to sleep
	 */
	private void nap(long millis){
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// wake up earlier is not so bad
		}
	}
	
	/**
	 * Give access to the {@link SynchronousQueue} to receive data from the {@link TTEBufferedReader}.
	 * This is used by {@link venice.hub.DiskReader}.
	 * 
	 * @return Reference to the SynchronousQueue used by TTEBufferedReader to send data.
	 */
	public SynchronousQueue<SlotEvent> getSyncQ(){
		return syncQ;
	}
	
	/**
	 * method for debugging
	 */
	public TTEBuffer getTTEBuffer(){
		return buffer;
	}
	
	/**
     * Gets the maximum capacity of the buffer.
     * @return capacity of the buffer
     */
	public int getCAPACITY(){
		return CAPACITY;
	}
	
	/**
     * Gets the threshold of the buffer.
     * @return threshold of the buffer
     */
	public int getTHRESHOLD(){
		return THRESHOLD;
	}
}
