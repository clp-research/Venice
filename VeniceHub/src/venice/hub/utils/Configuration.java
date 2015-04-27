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
package venice.hub.utils;

import java.io.File;
import java.util.ArrayList;
import org.apache.log4j.Logger;

import com.beust.jcommander.JCommander;

import venice.hub.VeniceHub;
import venice.lib.AbstractSlot;
import venice.lib.networkIIO.SlotFlags;
import venice.lib.parser.SensorFileReader;
import venice.lib.parser.XIODomParser;
import venice.lib.parser.XIORegExParser;

/**
 * Parses command line arguments and provides the configuration data for the
 * other components of VeniceHub.
 * <p>
 * This class is a singleton structure. To access the non-static methods, use
 * the <code>Configuration</code> object returned by <code>getInstance</code>.
 * <p>
 * Most settings have default values, either by <code>Configuration</code>
 * itself, or by <code>ArgumentParser</code>, in many cases by both.
 * Default values of <code>ArgumentParser</code> will overwrite those from
 * <code>Configuration</code>.
 * 
 * @author Jens Eickmeyer, Oliver Eickmeyer
 */
public class Configuration {
	private static Logger logger;

	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(Configuration.class);
	}
	
	/**
	 * The default name for the xml file containing definitions of how to map
	 * XIO codes and data types for RSB.
	 * This file will be loaded when XIO-RSB-mapping is needed, but the user
	 * have not provided the relevant command line argument (-x).
	 * For example, the RSB-to-Disk-mode requires the XIO-RSB-mapping.
	 */
	public static final String FILENAME_XIO_RSB = "xiocodes_RSB.xml";
	
	/**
	 * The default name of the folder containing the protobuf classes for RSB.
	 * This folder will be used, when RSB-modes are used, but the user
	 * have not provided the relevant command line argument (--protobuf).
	 */
	public static final String DIRNAME_PROTOBUF = "protobuf";
	
	/**
	 * The default name for the xml file containing definitions of how to
	 * match classes between RSB and IIO.
	 * This file will be loaded when class matching between IIO- and RSB-
	 * datatypes is necessary, but the user have not provided the relevant
	 * option (--classMatcher).
	 * For example, the RSB-to-IIO-mode requires class matching.
	 */
	public static final String FILENAME_CLASS_MATCH = "match.xml";

	/**
	 * The possible connections for data sources and targets.
	 * They are given by the command line argument <code>--input</code> and
	 * <code>--output</code>, or
	 * <code>-i</code> and <code>-o</code>. They are defining the
	 * 'mode of operation' of VeniceHub.
	 * <ul>
	 * <li>IIO - InstantIO</li>
	 * <li>RSB - Robotics Service Bus</li>
	 * <li>DISK - a file in the local file system</li>
	 * <li>VP - a TCP socket (only as a source)</li>
	 * </ul>
	 *
	 */
	public enum Connection {IIO, RSB, DISK, VP}

    // Default settings (may be overwritten by a given configuration file)
    private Connection source = Connection.IIO;
    private Connection target = Connection.DISK;
    private String multicastAddress = "224.21.12.68";
    private int multicastPort = 4711;
    private int multicastTTL = 1;
    private String unicastAddress = null;
    private int unicastPort = 0;
    private String unicastServers = null;
    private String namespaceLabel = "VeniceHub";
    private int headerLines = 2; // the first n lines of a replay file will be ignored
    private String logFilePath = "log.xio.gz";
    private boolean writeRaw = false;
    private String headerLine="<?xml version=\"1.0\"?>";
    private boolean rsbToXIO = false; // convert data into a xio line before pushing it into RSB ?
    private boolean rsbStringsAreXIO = false; // parse RSB Srings as XIO lines?
    private String slotFile = null;
    private boolean slotPredefined = false; // will be set to true by readSlotFile()
    private ArrayList<AbstractSlot> preScopes = null; // predefined slots for RSB scope and IIO Namespace
    private String rsbDefaultInformerScope = "/";
    private boolean noRPC = false;
    private String rpcServerAdress = "localhost";
    private int rpcServerPort = 4243;
    private int queueCapacity = 10000;
    private int bufferCapacity = 1000000;
    private int bufferThreshold = 100000;
    private long bufferMinimumSkipAmount = 2000; // bytes
    private int numOfLinesToEstBPSFromReplay = 10000; // wil read so many lines to estimate bytes per second for skipping
    private double toSmallProgress = 0.01;
    private double bigEnoughProgress = 0.99;
    private String lagLogFile = "lag.dat";
    private String protobufDir = null;
    private String classMatchFile = null;
    private String xiocodesFilename = null;
    private long quitIfIdle = 0;
    private boolean sendInitValue = false;
    private SlotFlags slotFlags = new SlotFlags();
    private String vpAddress = "localhost"; // for VenicePort connection
    private int vpPort = 0; // for VenicePort connection
    private String vpFile = null;
    private boolean maxprio = false;
    
    // singleton constructor
    private static Configuration configuration = new Configuration();

    /**
     *  Constructor is private, because this is a singleton class
     */
    private Configuration() {
    	// nothing
    }
    
    /**
     * Parses command line arguments.
     * Use <code>--help</code> or <code>-h</code> to see all available
     * command line argument options.
     * @param args command line arguments
     * @return returns <code>true</code> if no error has occurred while parsing
     * arguments
     */
    public boolean parseArguments(String[] args){
    	ArgumentParser ap = new ArgumentParser();
    	JCommander jc = new JCommander(ap, args);
    	
    	if(ap.needsHelp()){
    		jc.usage();
    		return false;
    	}
        
        source = ap.getInput();
        target = ap.getOutput();
        logFilePath = ap.getFilename();
        VeniceHub.setReplayOffset(ap.getOffset());
        multicastAddress = ap.getMulticastAddress();
        multicastPort = ap.getMulticastPort();
        multicastTTL = ap.getMulticastTTL();
        unicastAddress = ap.getUnicastAddress();
        unicastPort = ap.getUnicastPort();
        unicastServers = ap.getUnicastServers();
        writeRaw = ap.getWriteRaw();
        slotFile = ap.getSlotFilename();
        namespaceLabel = ap.getNamespaceLabel();
        headerLines = ap.getHeaderlines();
        headerLine = ap.getHeader();
        rsbToXIO = ap.getRSBtoXIO();
        rsbStringsAreXIO = ap.getRSBStringsAreXIO();
        rsbDefaultInformerScope = ap.getRSBDefaultInformerScope();
        queueCapacity = ap.getQueueCapacity();
        bufferCapacity = ap.getBufferCapacity();
        bufferThreshold = ap.getBufferThreshold();
        bufferMinimumSkipAmount = ap.getBufferMinimumSkipAmount();
        numOfLinesToEstBPSFromReplay = ap.getNumOfLinesToEstBPSFromReplay();
        toSmallProgress = ap.getToSmallProgress();
        noRPC = ap.isNoRPC();
        rpcServerAdress = ap.getRPCServerAdress();
        rpcServerPort = ap.getRPCServerPort();
        VeniceHub.setLagHistoryEnabled(ap.isLagHistoryEnabled());
        VeniceHub.setLagLogN(ap.getLagLogN());
        lagLogFile = ap.getLagLogFile();
        VeniceHub.setMessageEnabled(! ap.isSilent());
        VeniceHub.setReplayOffset(ap.getOffset());
        switch(ap.getParser()){
        case "DOM":
        	VeniceHub.setPreferredXIOParser(new XIODomParser());
        	break;
        case "REGEX":
        	VeniceHub.setPreferredXIOParser(new XIORegExParser());
        	break;
        default:
        	logger.error("Don't know XIOParser "+ap.getParser());
        	return false;
        }
        protobufDir = ap.getProtobuf();
        classMatchFile = ap.getClassMatcher();
        xiocodesFilename = ap.getXIOCodeFile();
        quitIfIdle = ap.getQuitIfIdle();
        sendInitValue = ap.getSendInitValue();
        slotFlags.setImporting(ap.getImportFlag());
        slotFlags.setExporting(ap.getExportFlag());
        vpPort = ap.getVPPort();
        vpFile = ap.getVPFile();
        maxprio = ap.getMaxPrio();
        
        return true;
    }
    
    /**
     * Prints configuration on screen.
     */
    public void printConfig(){
    	System.out.println("--input");
    	System.out.println(
    			source == Connection.IIO     ? "IIO":
	            source == Connection.DISK    ? "Disk":
	            source == Connection.RSB     ? "RSB":
                "?");
    	System.out.println();
    	
    	System.out.println("--output");
    	System.out.println(
    			target == Connection.IIO     ? "IIO":
	            target == Connection.DISK    ? "Disk":
	            target == Connection.RSB     ? "RSB":
                "?");
    	System.out.println();
    	
    	System.out.println("--file");
    	System.out.println(logFilePath);
    	System.out.println();
    	
    	if(writeRaw){
    		System.out.println("--writeRaw");
    		System.out.println();
    	}
    	
    	if(slotFile != null){
    		System.out.println("--slotfile");
    		System.out.println(slotFile);
    		System.out.println();
    	}
    	
    	if(xiocodesFilename != null && ! xiocodesFilename.isEmpty()){
    		System.out.println("--xiocodes");
    		System.out.println(xiocodesFilename);
    		System.out.println();
    	}
    	
    	if(VeniceHub.getReplayOffset() != 0){
    		System.out.println("--offset");
    		System.out.println(VeniceHub.getReplayOffset());
    	}
    	
    	System.out.println("--mcadress");
    	System.out.println(multicastAddress);
    	System.out.println();
    	
    	System.out.println("--mcport");
    	System.out.println(multicastPort);
    	System.out.println();
    	
    	System.out.println("--mcttl");
    	System.out.println(multicastTTL);
    	System.out.println();
    	
    	if(namespaceLabel != null && ! namespaceLabel.isEmpty()){
    		System.out.println("--namespaceLabel");
    		System.out.println(namespaceLabel);
    		System.out.println();
    	}
    	
    	System.out.println("--importflag");
    	System.out.println(slotFlags.isImporting());
    	System.out.println();
    	
    	System.out.println("--exportflag");
    	System.out.println(slotFlags.isExporting());
    	System.out.println();
    	
    	System.out.println("--headerlines");
    	System.out.println(headerLines);
    	System.out.println();

    	System.out.println("--header");
    	System.out.println(headerLine);
    	System.out.println();
    	
    	if(sendInitValue){
    		System.out.println("--sendInitValue");
    		System.out.println();
    	}

    	if(rsbToXIO){
    		System.out.println("--rsbtoXIO");
    		System.out.println();
    	}

    	if(rsbStringsAreXIO){
    		System.out.println("--rsbStringsAreXIO");
    		System.out.println();
    	}

    	System.out.println("--rsbDefInfScope");
    	System.out.println(rsbDefaultInformerScope); 
    	System.out.println();
    	
    	if(protobufDir != null && ! protobufDir.isEmpty()){
    		System.out.println("--protobuf");
    		System.out.println(protobufDir);
    		System.out.println();
    	}
    	
    	if(classMatchFile != null && ! classMatchFile.isEmpty()){
    		System.out.println("--classMatcher");
    		System.out.println(classMatchFile);
    		System.out.println();
    	}
    	
    	if(noRPC){
    		System.out.println("--noRPC");
    		System.out.println();
    	}
    	
    	System.out.println("--rpcServerAdress");
    	System.out.println(rpcServerAdress);
    	System.out.println();
    	
    	System.out.println("--rpcServerPort");
    	System.out.println(rpcServerPort);
    	System.out.println();    	
    	
    	if( ! VeniceHub.getMessageEnabled()){
    		System.out.println("--silent");
    		System.out.println();
    	}
    	
    	System.out.println("--queueCapacity");
    	System.out.println(queueCapacity);
    	System.out.println();
    	
    	System.out.println("--parser");
    	System.out.println(VeniceHub.getPreferredXIOParser());
    	System.out.println();
    	
    	System.out.println("--bufferCapacity");
    	System.out.println(bufferCapacity);
    	System.out.println();
    	
    	System.out.println("--bufferThreshold");
    	System.out.println(bufferThreshold);
    	System.out.println();
    	
    	System.out.println("--bufferMinimumSkip");
    	System.out.println(bufferMinimumSkipAmount);
    	System.out.println();
    	
    	System.out.println("--numLinesToEstBPS");
    	System.out.println(numOfLinesToEstBPSFromReplay);
    	System.out.println();
    	
    	System.out.println("--toSmallProgress");
    	System.out.println(toSmallProgress);
    	System.out.println();
    	
    	System.out.println("--bigEnoughProgress");
    	System.out.println(bigEnoughProgress);
    	System.out.println();
    	
    	if(VeniceHub.isLagHistoryEnabled()){
    		System.out.println("--lagHistory");
    		System.out.println();
    		System.out.println("--lagLogN");
    		System.out.println(VeniceHub.getLagLogN());
    		System.out.println();
    		System.out.println("--lagLogFile");
    		System.out.println(lagLogFile);
    	}

    }

    /**
     * Returns the singleton instance of this class. To get access to the
     * non-static functions of Configuration, use this function.
     * @return
     */
    public static Configuration getInstance() {
        return configuration;
    }

    /**
	 * Returns the multicast address that is used by InstantIO
	 * network operations. It is returned as a <code>String</code>
	 * representation (i.e. <code>"224.21.12.68"</code>).
	 * @return multicast address for InstantIO
	 */
    public String getMulticastAdress() {
        return multicastAddress;
    }

    /**
	 * Returns the actual used multicast port that is used by InstantIO
	 * network operations.
	 * @return port for multicast
	 */
    public int getMulticastPort() {
        return multicastPort;
    }

    /**
     * Returns the name of the log file that is used for DISK connections.
     * If the target is DISK, then the log file is the output file where
     * VeniceHub write the XIO line representations of incoming data.
     * If the source is DISK, then the log file is read and parsed by
     * VeniceHub to replay the data.
     * @return name of the file for DISK-modes (may include path)
     */
    public String getLogFilePath() {
        return logFilePath;
    }

    /**
     * Returns the number of header lines in a log file. Those lines will
     * be ignored when reading from a log file.
     * 
     * @return number of headerLines to be ignored
     */
    public int getHeaderLines() {
        return headerLines;
    }

    /**
     * Returns <code>true</code> if sensor slots are predefined.
     * This happens only, when a slot file was read and parsed into
     * a list of predefined sensor slots.  VeniceHub will then create
     * only those slots, instead of creating slots dynamically.
     * Use <code>getPreScopes()</code> to get access to the list
     * of predefined sensor slots.
     * @return <code>true</code> if sensor slots are predefined;
     * <code>false</code> otherwise.
     */
    public boolean isSensorPredefined(){
    	return slotPredefined;
    }
    
    /**
     * Returns a list of predefined sensor slots. 
     * @return list of sensor slots, or <code>null</code> if no slots
     * has been predefined.
     */
    public ArrayList<AbstractSlot> getPreScopes(){
    	return preScopes;
    }
    
    /**
     * Returns <code>true</code> if the RSB-to-XIO option is used.
     * With this option, all data will first be converted into a String
     * containing an XIO line representation of that data. Then this
     * String object is send to RSB, instead of the data itself.
     * @return <code>true</code> if RSB-to-XIO option is used,
     * <code>false</code> otherwise
     */
    public boolean isRSBToXIO(){
    	return rsbToXIO;
    }
    
    /**
     * Returns <code>true</code> if a String received over RSB has to be
     * parsed as an XIO line.
     * @return <code>true</code> if RSB-Strings has to be parsed as XIO-lines,
     * <code>false</code> otherwise.
     */
    public boolean isRSBStringXIOLine(){
    	return rsbStringsAreXIO;
    }
    
    /**
     * Returns the default scope that is used by RSB informers, when
     * sensor slots (or scopes) are not predefined, but created
     * dynamically.
     * @return String with default scope for RSB informers
     */
    public String getRSBDefaultInformerScope(){
    	return rsbDefaultInformerScope;
    }
    
    /**
     * Returns the capacity of the delayed queue that holds the
     * data read from source, before it is passed on to the target.
     * The integer is the maximum number of events that can be stored
     * in the queue.
     * @return queue capacity (maximum number of events)
     */
    public int getQueueCapacity(){
    	return queueCapacity;
    }
    
    /**
     * Returns the capacity of the disk reader buffer. This is the maximum
     * number of events that the buffer can store, while reading from a
     * log file.
     * @return capacity of disk reader buffer (maximum number of events)
     */
    public int getBufferCapacity(){
    	return bufferCapacity;
    }
    
    /**
     * Returns the threshold for the disk reader buffer. If fewer events left
     * in the buffer than the threshold, the buffer will be shifted.
     * @return threshold for shifting the disk reader buffer
     */
    public int getBufferThreshold(){
    	return bufferThreshold;
    }
    
    /**
     * Returns the minimum of bytes that will be skipped while searching
     * a specific position in the log file (if source is DISK). If the
     * search distance is smaller than this minimum, the disk reader
     * will switch to line-by-line reading, instead of doing 'jumps'.
     * @return minimum number of bytes that can be skipped, before switching
     * to line-by-line reading (for disk reader)
     */
    public long getBufferMinimumSkipAmount(){
    	return bufferMinimumSkipAmount;
    }
    
    /**
     * Returns the number of lines that should be used to estimate the
     * byte-per-second (BPS) ratio of a replay file. The disk reader will read
     * at maximum this number of lines and then calculates the BPS.
     * The BPS value is used to calculate the size of the 'jumps', when
     * searching a far position in the log file.
     * @return maximum number of lines to be used for estimating
     * the byte-per-second value for the replay file 
     */
    public int getNumOfLinesToEstBPSFromReplay(){
    	return numOfLinesToEstBPSFromReplay;
    }
    
    /**
     * Returns the progress value that is to small to continue 'jumping'
     * while searching a specific position in the replay file. While
     * the disk reader is searching for the demanded timestamp in the
     * replay file, it will make 'jumps' to be faster. After each jump the
     * progress is calculated, to make any further jumps more precise
     * (and of course to see if the target position is reached).
     * But if the progress is so small, that it gives no speed advantage,
     * than the disk reader will switch back to line-by-line reading. 
     */
    public double getToSmallProgress(){
    	return toSmallProgress;
    }
    
    /**
     * Returns the progress value that is big enough to re-calculate the BPS
     * (byte-per-second) value for the replay file.
     * The initial BPS value is an estimation, based on the first lines of the
     * replay file. But while 'jumping' through the file in search for a
     * specific timestamp, the disk reader gets more information about the
     * density of the file, so it can calculate a more precise value for the
     * area where the searched timestamp 'lives'.
     */
    public double getBigEnoughProgress(){
    	return bigEnoughProgress;
    }

    /**
     * Returns <code>true</code> if RPC (remote-procedure-call) is disabled.
     * Be default RPC is enabled when source is DISK.
     * @return <code>true</code> if RPC connection is disabled,
     * <code>false</code> otherwise
     */
    public boolean isNoRPC(){
    	return noRPC;
    }
    
    /**
     * Returns the port for the RPC-Server.
     */
    public String getRPCServerAddress(){
    	return rpcServerAdress;
    }
    public int getRPCServerPort(){
    	return rpcServerPort;
    }
    
    /**
     * Returns the name of the file where the lag history has to be saved.
     * @return Filename
     */
    public String getLagLogFile(){
    	return lagLogFile;
    }

	public String getNamespaceLabel() {
		return namespaceLabel;
	}
	
	/**
	 * Returns the source connection (where the data comes from).
	 * @return source connection
	 */
	public Connection getSource(){
		return source;
	}
	
	/**
	 * Returns the target connection (where the received data
	 * will be send).
	 * @return target connection
	 */
	public Connection getTarget(){
		return target;
	}
	
	/**
	 * Returns a String representing the directory that contains the protobuf
	 * classes.  The protobuf classes are used with RSB-modes.
	 * @return protobuf directory
	 */
	public String getProtobufDir(){
		return protobufDir;
	}
	
	/**
	 * Set the name of the directory where to look for protobuf classes.
	 * Protobuf classes are used by RSB.
	 * @param directory (as a String)
	 */
	public void setProtobufDir(String dir){
		protobufDir = dir;
	}
	
	/**
	 * Returns the name of the file with class matching definitions.
	 * Class matching is used by RSB-IIO-modes, to translate events from
	 * one protocol to the other.
	 * 
	 * @return name of file with class matching definitions
	 */
	public String getClassMatchFile(){
		return classMatchFile;
	}
	
	/**
	 * Sets the name of the file with class matching definitions.
	 * Class matching is used by RSB-IIO-modes, to translate events from
	 * one protocol to the other.
	 * 
	 * @param name of file with class matching definitions
	 */
	public void setClassMatchFile(String filename){
		classMatchFile = filename;
	}

	/**
	 * Returns the name of the file containing XML definitions of a mapping
	 * between XIO tag names and data types.
	 * @param name of file with XIO mapping definitions
	 */
	public String getXioCodesFilename() {
		return xiocodesFilename;
	}
	
	/**
	 * Set a name for a file containing XML definitions of a mapping
	 * between XIO tag names and data types.
	 * @param filename
	 */
	public void setXioCodesFilename(String filename) {
		xiocodesFilename = filename;
	}
	
	/**
	 * Reads and parses the slot file. Used for predefining slots.
	 * Be sure to call this AFTER loading all external classes (e.g. protobuf)
	 * and adding XIO codes to XIOMaps.<br>
	 * If done to early, some types will not be recognized.
	 */
	public void readSlotFile(){
		// a xml file with slot definitions is given
		
		if(preScopes != null){
			// if file already has been read
			return;
		}
		
		if(slotFile == null){
			return;
		}
		
        File file = new File(slotFile);
        
    	if(file.exists() && file.isFile()){
    		// if the filename is okay
    		preScopes = SensorFileReader.parse(file);
    		if(preScopes == null){
    			logger.error("Parsing content of slotFile '"+slotFile+"' failed.");
    		}
    		else{
    			slotPredefined = true;
    		}
    	}
    	else{
    		logger.error("Could not find "+slotFile);
    	}
	}
	
	/**
	 * Should data be written uncompressed to disk?
	 * @return <code>true</code> if data should be written in raw form (instead of compressed form)
	 */
	public boolean writeRaw(){
		return writeRaw;
	}
	
	/**
	 * Check if venice.hub should quit if idle for a specific time.
	 * @return maximum idle time (ms)
	 */
	public long getQuitIfIdle(){
		return quitIfIdle;
	}
	
	/**
	 * Gets the time-to-live for multicast.
	 * @return time-to-live for multicast
	 */
	public int getMulticastTTL(){
		return multicastTTL;
	}

	/**
	 * For activating initialization values. They will be send through new
	 * created predefined IIO out-slots. 
	 */
	public void setSendInitValue(boolean siv){
		sendInitValue = siv;
	}
	
	/**
	 * Tells if initialization values are active.
	 * If they are, they will be send once over a new created out-slot.
	 * This helps InstantIO to be ready without delay when the real data
	 * arrives.
	 * 
	 * @return state of initialization value activation
	 */
	public boolean getSendInitValue(){
		return sendInitValue;
	}
	
	/**
	 * Set the socket port for unicast for InstantIO.
	 * Unicast is normally not used, as long as multicast
	 * is working.
	 * @param newUnicastPort
	 */
	public void setUnicastPort(int newUnicastPort){
		unicastPort = newUnicastPort;
	}
	
	/**
	 * Sets the IP address for unicast for InstantIIO. 
	 * Unicast is normally not used, as long as multicast
	 * is working.
	 * @param newUnicastAddress
	 */
	public void setUnicastAddress(String newUnicastAddress){
		unicastAddress = newUnicastAddress;
	}
	
	/**
	 * Sets the unicast servers for InstantIO.
	 * Unicast is normally not used, as long as multicast
	 * is working.
	 * @param newUnicastServers
	 */
	public void setUnicastServers(String newUnicastServers){
		unicastServers = newUnicastServers;
	}
	
	/**
	 * Gets the port for unicast for InstantIO.
	 * @return port for unicast
	 */
	public int getUnicastPort(){
		return unicastPort;
	}
	
	/**
	 * Returns the IP address for unicast for InstantIO.
	 * If set to <code>null</code>, then unicast is not used.
	 * @return IP address for unicast or <code>null</code> if unicast is not
	 * used.
	 */
	public String getUnicastAddress(){
		return unicastAddress;
	}
	
	/**
	 * Returns a String containing the list of servers used for unicast
	 * for instantIO, or <code>null</code> if unicast is not used.
	 * @return list of servers or <code>null</code> if unicast is not used
	 */
	public String getUnicastServers(){
		return unicastServers;
	}
	
	/**
	 * Sets the slot flag for the IIO network node.
	 * The flags are setting the behavior of importing and exporting slots
	 * from or to other network nodes in the IIO network.
	 * See documentation of VeniceLib for more details.
	 * @return slot flags setting for network node
	 */
	public SlotFlags getSlotFlags(){
		return slotFlags;
	}
	
	/**
	 * Returns the address used for VP (VenicePort) mode.
	 * @return address used for VP
	 */
	public String getVPAddress(){
		return vpAddress;
	}
	
	/**
	 * Returns the port used for VP (VenicePort) mode.
	 * @return port used for VP
	 */
	public int getVPPort(){
		return vpPort;
	}
	
	/**
	 * Sets the address used for VP (VenicePort) mode.
	 * @param newVPAddr address to be set for VP
	 */
	public void setVPAddress(String newVPAddr){
		vpAddress = newVPAddr;
	}
	
	/**
	 * Sets the port used for VP (VenicePort) mode.
	 * @param newVPPort port to be set for VP
	 */
	public void setVPPort(int newVPPort){
		vpPort = newVPPort;
	}
	
	/**
	 * Sets the name of the VP-file, that contains the definition of the data
	 * that will be received over VP port.
	 * @param newVPFile name of XML file with VP data definitions
	 */
	public void setVPFile(String newVPFile){
		vpFile = newVPFile;
	}
	
	/**
	 * Returns the name of the VP-file, that contains the definition of the
	 * data that will be received over VP port.
	 * @return name of XML file with VP data definitions
	 */
	public String getVPFile(){
		return vpFile;
	}
	
	/**
	 * Returns <code>true</code> if threads are created with maximum
	 * execution priority. For example the source readers and target writers
	 * are created as Threads.
	 * @return <code>true</code> if threads are created with maximum
	 * execution priority, <code>false</code> otherwise
	 */
	public boolean isMaxPrio(){
		return maxprio;
	}
}

