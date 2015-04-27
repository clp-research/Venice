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

import java.io.File;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import venice.hub.utils.Configuration;
import venice.hub.utils.TTEQueue;
import venice.hub.utils.Configuration.Connection;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.parser.XIOParser;
import venice.lib.parser.XIORegExParser;

/**
 * Main class. Starts and stops all other threads.
 * <p>
 * The most important Threads are the reader and the writer.
 * The reader is always the Thread that receives or reads data from the source
 * and the writer is always the Thread that sends or writes data to the target. 
 * 
 * @author Jens Eickmeyer, Oliver Eickmeyer
 */
public class VeniceHub {
	private static Logger logger;
	static {
		// setup logger as early as possible
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(VeniceHub.class);
	}
	
	/**
	 * Defines a unique value for marking timestamps as invalid.
	 * Usually this value is got from venice.lib 
	 * (<code>venice.lib.parser.XIOParser.INVALID_TIMESTAMP</code>).
	 */
	public final static long INVALID_TIMESTAMP = venice.lib.parser.XIOParser.INVALID_TIMESTAMP;
	
	/**
	 * Threads (especially subclasses of VeniceReader and VeniceWriter) should
	 * check if they were commanded to stop while they were paused.
	 * This defines the interval for such checks (in ms).
	 */
	public final static long CHECK_IF_STOPPED_WHILE_PAUSED_INTERVAL = 1000;
	
	private static boolean active = true;
	private static long replayDelay; // difference between timestamps of logfile and current systemtime plus any delay of pausing
	private static long replayOffset; // gets added to every timestamp (for synchronization with ELAN)
	private static long timestampOf1stLine;
	private static long timestampOfLastLine;
	private static long lastPushedTimestamp;
	private static boolean messagesEnabled;
	private static boolean lagHistoryEnabled;
	private static int lagLogN = 1;
	private static XIOParser xioparser;
	private static long lastActivity = INVALID_TIMESTAMP;
    private static boolean paused; // replay paused?
    private static long pauseTime; // the system time when pause started
	
    // there can be more than one controller
	private static ArrayList<VeniceControl> controllerList;
	private static ArrayList<Thread> controllerThreadList;
	
	// there should be only ONE reader and ONE writer
    private static VeniceReader reader;
    private static VeniceWriter writer;

    /**
     * Main method. Creates VeniceHub, which organize all other threads.
     * 
     * @param args optional command line arguments, first argument is treated as a path to a configuration file
     */
    public static void main(String[] args) {
    	initialize(args);
    	while( active ){
    		try {
    			Thread.sleep(100);
    		} catch (InterruptedException e) {
    		}
    	}
    	clean();
    	message("veniceHub finished");
    	System.exit(0);
    }
    
    /**
     * Constructor of VeniceHub is private, so no instance can be created.
     * VeniceHub.java is used static only.
     */
    private VeniceHub(){
    	// nothing
    }

    /**
     * Commands running threads to stop and wait until they have finished.
     */
	private static void clean() {
		// command all controller threads to stop:
		for(VeniceControl vc : controllerList){
			logger.debug("commanding "+vc+" to stop");
			vc.stopThread();
		}
		
		// command reader and writer threads to stop:
        if(reader!=null) reader.stopThread();
        if(writer!=null) writer.stopThread();
        
        // wait until reader and writer threads have finished:
		String stillActive; // to show the still unfinished threads
		String oldActive=""; // to remember the unfinished threads from before 
		do{
			stillActive = "";
			if(reader != null && !reader.isFinished()) stillActive += " - reader\n"; // if a reader is unfinished
			if(writer != null && !writer.isFinished()) stillActive += " - writer\n"; // if a writer is unfinished
			for(VeniceControl vc : controllerList){
				if(vc != null && !vc.isFinished()) stillActive += " - controller "+vc+"\n"; // if a controller is unfinished
			}
			if(!stillActive.equals("")){
				// if there are unfinished threads, add some explaining text:
				stillActive = "waiting for shutdown of\n" + stillActive;
			}
			if(!stillActive.equals(oldActive)){
				 // if the unfinished threads are not the same like before, print them to console
				 // (so they don't get printed out multiple times)
				message(stillActive);
				oldActive = stillActive; // remember the unfinished tasks for the next loop iteration
			}
			try {
				Thread.sleep(100); // wait a bit to not waste CPU time
			} catch (InterruptedException e) {} // it's no problem if the waiting gets interrupted
		}while(!stillActive.equals(""));
		System.exit(0);
    }
	
	/**
	 * Generates an InstantIO namespace with the information from Configuration.
	 * 
	 * See the overloaded method for more information about node prefix, namespace labels and slot slot names.
	 */
	private static void startNetworkNode(){
		startNetworkNode("{SlotLabel}");
	}

    /**
     * Generates an InstantIO namespace with the information from Configuration.
     * <p>
     * The network node will put the prefix in front of namespaces.
     * Add <code>"/{SlotLabel}"</code> to the prefix to have the namespace and slot labels after the prefix.
     * Usually prefixes will be of syntax <code>"prefix/{SlotLabel}"</code>.
     * For example, if the prefix is <code>"replay/{SlotLabel}"</code>
     * and a namespace with label <code>"VeniceHub"</code> is created
     * and a slot with label <code>"Sensor1/Gesture"</code> is added to this namespace, then
     * the resulting slot name will be <code>replay/VeniceHub/Sensor1/Gesture</code>.<br>
     * To omit a node prefix, just set it to <code>{SlotLabel}</code>.
     * 
     * @param prefix The prefix for the networknode
     */
    private static void startNetworkNode(String prefix) {
        
    	IIONamespaceBuilder.setMulticastAddress(Configuration.getInstance().getMulticastAdress());
    	IIONamespaceBuilder.setMulticastPort(Configuration.getInstance().getMulticastPort());
    	IIONamespaceBuilder.setPrefix(prefix);
    	
    }
    
    /**
     * Initializes all variables, does some basic setup and starts all
     * threads (the reader, the writer and all controllers).
     * This is the first method that gets called, when VeniceHub
     * is started.
     * @param args This are the command line arguments. There is only one
     * possible argument, the name of a configuration file (optional).
     */
    public static void initialize(String[] args){
    	
    	paused = false; // Warning: Changing this need also changes for readers and writers
    	messagesEnabled = true;
    	replayDelay = 0;
		replayOffset = 0;
		timestampOf1stLine = INVALID_TIMESTAMP;
		timestampOfLastLine = INVALID_TIMESTAMP;
		lastPushedTimestamp = INVALID_TIMESTAMP;
		xioparser = new XIORegExParser();
		lagHistoryEnabled = false;

    	boolean parsedSuccessfully = Configuration.getInstance().parseArguments(args);
    	if(! parsedSuccessfully) System.exit(1);
    	
    	Connection target = Configuration.getInstance().getTarget();
    	Connection source = Configuration.getInstance().getSource();
    	
    	/* check for implicit need of the default xio code file for RSB.
    	 * This is true for RSB<->Disk modes.
    	 */
        if(Configuration.getInstance().getXioCodesFilename() == null){
        	if(source == Connection.RSB  && target == Connection.DISK ||
        	   source == Connection.DISK && target == Connection.RSB){
        		File f = new File(Configuration.FILENAME_XIO_RSB);
        		if(f.exists() && f.isFile()){
	        		Configuration.getInstance().setXioCodesFilename(Configuration.FILENAME_XIO_RSB);
	        		logger.info("will use file "+Configuration.getInstance().getXioCodesFilename()+" for XIO-codes-file (use -x option to name another file)");
        		}
        	}
        }

        /*
         * check for implicit need of the default protobuf folder for RSB.
         * This is true for all RSB-modes.
         * It's possible to use RSB without protobuf classes, but then only
         * Strings can be send and received.
         */
        if(Configuration.getInstance().getProtobufDir() == null){
        	if(source == Connection.RSB || target == Connection.RSB){
        		File f = new File(Configuration.DIRNAME_PROTOBUF);
        		if(f.exists() && f.isDirectory()){
        			Configuration.getInstance().setProtobufDir(f.getAbsolutePath());
        			logger.info("will use directory "+Configuration.getInstance().getProtobufDir()+" for protobuf-classes (use --protobuf option to name another folder)");
        		}
        	}
        }
        
        /*
         * check for implicit need of the default class matching definitions
         * for RSB<->IIO modes.
         */
        if(Configuration.getInstance().getClassMatchFile() == null){
        	if(source == Connection.RSB && target == Connection.IIO ||
        	   source == Connection.IIO && target == Connection.RSB){
        		File f = new File(Configuration.FILENAME_CLASS_MATCH);
        		if(f.exists() && f.isFile()){
        			Configuration.getInstance().setClassMatchFile(Configuration.FILENAME_CLASS_MATCH);
        			logger.info("will use file "+Configuration.getInstance().getClassMatchFile()+" for class matching defs between RSB and IIO (use --classMatcher option to name another file).");
        		}
        	}
        }
        
        // start ConsoleControl for receiving commands via console
        VeniceControl consoleControl = new ConsoleControl();
        Thread consoleControlThread = new Thread(consoleControl, "Console controller");
        consoleControlThread.start();
        controllerList = new ArrayList<VeniceControl>();
        controllerThreadList = new ArrayList<Thread>();
        controllerList.add(consoleControl);
        controllerThreadList.add(consoleControlThread);
        
        if( source == Connection.DISK){
        	// if replay mode:
        	if(!new File(Configuration.getInstance().getLogFilePath()).isFile()){
        		// if the replay file does not exist:
        		System.err.println("The file "+Configuration.getInstance().getLogFilePath()+" does not exist or is not a file.");
        		System.exit(1);
        	}
        }
        
        if(source == target){
        	System.err.println("Source and target can not be the same.");
        	System.exit(1);
        }
        
        if(target == Connection.VP){
        	System.err.println("VenicePort can not be used as target");
        	System.exit(1);
        }

        switch (target) {
        case IIO:
        	startNetworkNode();
        	writer = new IIOWriter();
            createThread(writer, "VH_IIOWriter");
        	break;
        case DISK:
        	writer = new DiskWriter();
            createThread(writer, "VH_DiskWriter");
            break;
        case RSB:
        	writer = new RSBWriter();
        	createThread(writer, "VH_RSBWriter");
            break;
        default:
        	// nothing
        }
        
    	// start the RPC Server for remote control, when replaying from Disk
    	if (source == Connection.DISK && ! Configuration.getInstance().isNoRPC()){
    		VeniceControl rpcControl = new RPCControl();
    		Thread rpcThreadControl = new Thread(rpcControl, "RPC controller");
    		rpcThreadControl.start();
    		
    		// add this new thread to the lists
    		controllerList.add(rpcControl);
    		controllerThreadList.add(rpcThreadControl);
    	}
        
        // wait until writer is initialized
        logger.debug("waiting for initialization of writer");
        while( ! writer.isInitialized()){
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// nothing
			}
        }
        logger.debug("writer is initialized");

        switch (source) {
        case IIO:
        	startNetworkNode();
        	reader = new IIOReader();
            createThread(reader, "VH_IIOReader");
        	break;
        case DISK:
        	reader = new DiskReader();
            createThread(reader, "VH_Diskreader");
            break;
        case RSB:
        	reader = new RSBReader();
            createThread(reader, "VH_RSBReader");
            break;
        case VP:
        	reader = new PortReader();
        	createThread(reader, "VH_PortReader");
        	break;
        default:
        	// nothing
        }
    }
    
    /**
     * Creates a thread for the Runnable, including a name.
     * Will also set priority.
     * @param runnable the Runnable that will be started as a separate Thread
     * @param name a name for the Thread
     * @return created Thread the created Thread
     */
    private static Thread createThread(Runnable runnable, String name){
    	Thread thread = new Thread(runnable, name);
    	if(Configuration.getInstance().isMaxPrio())
    		thread.setPriority(Thread.MAX_PRIORITY);
    	thread.start();
    	return thread;
    }
    
    /**
     * Switches between pause and play.
     */
    public static void switchPause(){
    	setPause(!paused);
    }
    
    /**
     * Sets pause or play mode.
     * 
     * @param p <code>true</code> will pause, <code>false</code> will un-pause
     */
    public static void setPause(boolean p){
    	if(!paused == p){
			paused = p;
			if(paused){
				pauseTime = System.currentTimeMillis();
				long delay = getReplayDelay();
				long first = getTimestampOf1stLine();
				long offset = getReplayOffset();
				if(first != INVALID_TIMESTAMP){
					long pausingtime = System.currentTimeMillis() - delay - first + offset;
					String niceString = giveNiceTimeString(pausingtime);
					logger.info("Replay paused at "+niceString+". Enter 'p' to continue.");
				}
				else{
					logger.info("Replay paused. Enter 'p' to continue.");
				}
					
				synchronized(writer){
					writer.pause();
				}
				synchronized(reader){
					reader.pause();
				}
			}else{
				logger.info("continuing replay");
				addReplayDelay(System.currentTimeMillis() - pauseTime);
				synchronized(writer){
					writer.proceed();
				}
				synchronized(reader){
					reader.proceed();
				}
			}
    	}
    }
    
    /**
     * Seek for a timestamp relative to the timestamp of the first line in
     * the file being replayed. This can also be modified by an optional
     * offset.
     * <p> 
     * The relative timestamp will be changed into an absolute timestamp
     * and than the <code>seekForTimestamp</code> method is called.
     * 
     * @param relativeTimestamp
     */
    public static void seekForRelativePosition(long relativeTimestamp){
    	long seekTime = getShiftedTimestampOf1stLine() + relativeTimestamp;
    	seekForTimestamp(seekTime);
    }
    
    /**
     * Seek for a absolute timestamp (a timestamp in the log file, without any modifications).
     * 
     * @param seekTime Timestamp to seek.
     */
    public static void seekForTimestamp(long seekTime){
    	long absSeekTime = seekTime;
    	setReplayDelay(System.currentTimeMillis() - absSeekTime);
		if(absSeekTime >= getTimestampOf1stLine()){
			TTEQueue.getInstance().reset();
			((DiskReader) reader).seek(absSeekTime);
		}
		if(paused) pauseTime = System.currentTimeMillis();
    }
    
    /**
     * Commands all threads to stop, waits until all threads are finished
     * and will then stop the virtual machine.
     */
    public static void quit(){
    	logger.info("got quit command");
    	active = false;
    }
    
    /**
     * Resets the reader (will start from the beginning again), so reading
     * from file will restart from beginning.
     * This includes clearing the queue and unpausing (if paused).
     */
    public static void reset(){
    	reader.reset();
    	TTEQueue.getInstance().reset();
		setPause(false);
    }
    
    /**
     * Shows some information about the disk-reader buffer.
     */
    public static void showBuffer(){
    	if(Configuration.getInstance().getSource() == Connection.DISK){
	    	DiskReader diskreader = (DiskReader) reader;
			message("TTEBuffer:");
			message("  First timestamp:     "+diskreader.getFirstTimestamp());
			message("  Threshold timestamp: "+diskreader.getThresholdTimestamp());
			message("  Last timestamp :     "+diskreader.getLastTimestamp());
			message("  actual size: "+diskreader.getBufferSize() + 
					"/"+diskreader.getCAPACITY()+" items (" +
					"threshold "+diskreader.getTHRESHOLD()+" items)");
    	}
    	else{
    		message("The command BUFFER only works for disk-readers.");
    	}
    }
    
    /**
     * Shows the timestamp of the last written event
     */
    public static void showLastWrittenTS(){
    	message("timestamp of last written event: "+getLastPushedTimestamp());
    }
    
    /**
     * Shows the timestamp of the first line of the file.
     * Only useful if reading from file.
     */
    public static void showTSOfFirstLine(){
    	message("Timestamp of first line: "+getTimestampOf1stLine());
    }
    
    /**
     * Saves lag history to file, if enabled.
     */
    public static void saveLagHistory(){
    	if(isLagHistoryEnabled()){
			if(!paused) setPause(true);
			String filename = Configuration.getInstance().getLagLogFile();
			message("saving lag data to "+filename);
			writer.saveLag(filename);
		}
		else{
			message("Lag logging history is not enabled.");
		}
    }
    
    /**
     * Switches between message mode on/off.
     */
    public static void switchMsg(){
    	setMessageEnabled(!getMessageEnabled());
		if(getMessageEnabled()) message("Messages enabled");
    }
    
	/**
	 * Sends a message to the default output.
	 * 
	 * @param msg the message to be send
	 */
	public static void message(String msg){
		message(msg, true);
	}
	/**
	 * Sends a message to the default output.
	 * 
	 * @param msg the message to be send
	 * @param linefeed if a linefeed should be added
	 */
	public static void message(String msg, boolean linefeed){
		if(messagesEnabled){
			if(linefeed) System.out.println(msg);
			else System.out.print(msg);
		}
	}
	
	/**
	 * Converts a Timestamp into a more readable String.
	 *
	 * @param t Timestamp (in milliseconds)
	 * @return String representing the Timestamp t
	 */
    public static String giveNiceTimeString(long t){
    	String s = "";
    	if(t > 3600000){
    		s = String.valueOf(t / 3600000L)+"h ";
    		t -= 3600000L * (t / 3600000L);
    	}
    	if(t > 60000){
    		s += String.valueOf(t / 60000L)+"m ";
    		t -= 60000 * (t / 60000L);
    	}
		s += String.valueOf((float)t / 1000.0)+"s";
    	
    	return s;
    }
    
    /**
     * Adds a value (ms) to the replay delay by which timestamps of events are
     * modified before they are replayed. See <code>setReplayDelay</code> for
     * more information.
     * 
     * @param delay milliseconds to add to the delay
     */
	public static void addReplayDelay(long delay){
    	replayDelay += delay;
    }
	
	/**
     * Sets the replay delay by which timestamps of events are modified before
     * they are replayed. This relates only to events from XIO lines from
     * a replay file. This delay is defined by the starting time of the
     * replay and by any pauses made during replay.
     * <p>
     * When starting the replay, the delay will be the difference between
     * the timestamp of the first line and the actual system time, so the
     * replay starts immediately. The duration of every pause will be added
     * to that delay.
     * 
     * @param delay delay (ms) by which events are delayed when replaying
     */
    public static void setReplayDelay(long delay){
    	replayDelay = delay;
    }
    
    /**
     * Gets the replay delay by which timestamps of events are modified before
     * they are replayed. See <code>setReplayDelay</code> for more
     * information.
     * 
     * @return delay (ms) by which events are delayed when replaying
     */
    public static long getReplayDelay(){
    	return replayDelay;
    }
    
    /**
     * Gets the offset by which timestamps of events are modified before
     * they are replayed. This relates only of events from XIO lines from
     * a replay file.
     * <p>
     * The offset can be used by the user or an external application
     * to synchronize replays.
     * 
     * @return offset (ms) to modify timestamp before replaying the event
     */
    public static long getReplayOffset(){
    	return replayOffset;
    }
    
    /**
     * Sets the timestamp-offset for replaying files.
     * The timestamp of the events parsed from XIO lines of the replay file
     * are getting modified by the offset before they get into the queue.
     * * <p>
     * The offset can be used by the user or an external application
     * to synchronize replays.
     *  
     * @param offset offset (ms) to modify timestamp before replaying the event
     */
    public static void setReplayOffset(long offset){
    	replayOffset = offset;
    	logger.info("replay offset set to "+replayOffset);
    }
    
    /**
     * 
     * @return Returns the timestamp of the first line plus replay start offset
     */
    public static long getShiftedTimestampOf1stLine(){
    	return timestampOf1stLine - replayOffset;
    }
    
    /**
     * Gets the timestamp of the first XIO line in the replay file
     * (for disk source mode).
     * 
     * @return timestamp of first XIO line in replay file
     */
    public static long getTimestampOf1stLine(){
    	return timestampOf1stLine;
    }
    
    /**
     * Sets the timestamp of the first XIO line in the replay file
     * (for disk source mode).
     * 
     * @param timestamp timestamp of first XIO line in replay file
     */
    public static void setTimestampOf1stLine(long timestamp){
    	timestampOf1stLine = timestamp;
    }
    
    /**
     * Returns the timestamp of the last pushed event 
     * (for network writer modes).
     * 
     * @return timestamp of the last pushed event
     */
    public static long getLastPushedTimestamp(){
    	return lastPushedTimestamp;
    }
    
    /**
     * Sets the timestamp of the last pushed event
     * (for network writer modes).
     * 
     * @param timestamp timestamp of the last pushed event
     */
    public static void setLastPushedTimestamp(long timestamp){
    	lastActivity = System.currentTimeMillis();
    	lastPushedTimestamp = timestamp;
    	//logger.debug("last pushed ts: "+lastPushedTimestamp);
    }
    
    /**
     * Should messages for the console be enabled?
     * @param state True or False
     */
    public static void setMessageEnabled(boolean state){
    	messagesEnabled = state;
    }
    
    /**
     * Returns <code>true</code> if messages are enabled, or
     * <code>false</code> if not.
     * @return <code>true</code> if messages are enabled;
     * <code>false</code> otherwise
     */
    public static boolean getMessageEnabled(){
    	return messagesEnabled;
    }
    
    /**
     * Returns the timestamp of the last line in the replay file.
     * This is only for disk source.
     * The timestamp will be INVALID_TIMESTAMP, if the timestamp of the
     * last line is unknown.
     * 
     * @return timestamp of last line in replay file
     */
    public static long getTimestampOfLastLine() {
		return timestampOfLastLine;
	}
    
    /**
     * Sets the timestamp of the last line in the file.
     * This is only for disk source.
     * 
     * @param newTimestampOfLastLine timestamp of last line in replay file
     */
	public static void setTimestampOfLastLine(long newTimestampOfLastLine) {
		timestampOfLastLine = newTimestampOfLastLine;
	}
	
	/**
	 * Returns the xioParser used for parsing XIO lines.
	 * @return xioParser used for parsing XIO lines
	 */
	public static XIOParser getPreferredXIOParser(){
		return xioparser;
	}
	
	/**
	 * Sets the xioParser to be used for parsing XIO lines.
	 * @param p the xioParser to be used for parsing
	 */
	public static void setPreferredXIOParser(XIOParser p){
		xioparser = p;
	}

	/**
	 * Enables or disables the lag history.
	 * 
	 * @param state <code>true</code> if lag history should be enabled;
	 * <code>false</code> otherwise
	 */
    public static void setLagHistoryEnabled(boolean state){
    	lagHistoryEnabled = state;
    }
    
    /**
     * Returns <code>true</code> if the lag history is enabled, or
     * <code>false</code> if not.
     * @return <code>true</code> if the lag history is enabled;
     * <code>false</code> otherwise
     */
    public static boolean isLagHistoryEnabled(){
    	return lagHistoryEnabled;
    }

    /**
     * Sets the rhythm of logging lag data.  A value of 1 means that
     * for every item lag data will be collected.  For large files it could
     * be better to increase the value (for example for a file with a
     * density of 2 megabytes per second a value of 200).
     *  
     * @param n Every n'th data item will be measured
     */
    public static void setLagLogN(int n){
    	lagLogN = n;
    }
    
    /**
     * See <code>setLagLogN</code>.
     * 
     * @return The setting of how dense the lag measuring will be
     */
    public static int getLagLogN(){
    	return lagLogN;
    }
    
    /**
     * Gives the system time of last activity in milliseconds.
     * @return system time of last activity (ms)
     */
    public static long getLastActivity(){
    	return lastActivity;
    }
    
    /**
     * Update last activity timer.<br>
     * This sets the last activity timer to the current system time (ms).
     */
    public static void updateLastActivity(){
    	lastActivity = System.currentTimeMillis();
    }
}

