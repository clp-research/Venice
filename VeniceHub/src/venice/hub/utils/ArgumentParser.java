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

import venice.hub.utils.Configuration.Connection;

import com.beust.jcommander.Parameter;

/**
 * Defines command line arguments and how to parse them.<br>
 * The command line parser is also capable of reading options from a file.
 * To give a file with options, use the &#64;-notation.<br>
 * <em>Example</em>:<br>
 * <code>java -jar VeniceHub.jar &#64;config_logger.args</code><br>
 * <br>
 * The file config_logger.args can look like this:<br>
 * <br>
 * <code>
 * --input<br>
 * IIO<br>
 * <br>
 * --output<br>
 * Disk<br>
 * <br>
 * --file<br>
 * log.xio.gz<br>
 * <br>
 * --writeRaw<br>
 * </code>
 * 
 * @author Oliver Eickmeyer
 *
 */
public class ArgumentParser {
	
	@Parameter(names = {"-h", "--help"}, hidden = true)
	private boolean help;
	public boolean needsHelp(){
		return help;
	}
	
	@Parameter(names = {"-i", "--input"}, description = "Input source (Disk, IIO, RSB, VP)", converter = ConnectionConverter.class)
	private Connection input = Connection.IIO;
	public Connection getInput(){
		return input;
	}
	
	@Parameter(names = {"-o", "--output"}, description = "Output target (Disk, IIO, RSB, VP)", converter = ConnectionConverter.class)
	private Connection output = Connection.DISK;
	public Connection getOutput(){
		return output;
	}
	
	@Parameter(names = {"--importflag"}, description = "sets import slot flag for IIO network node (TRUE, FALSE)", arity = 1)
	private Boolean imflag = null;
	public Boolean getImportFlag(){
		return imflag;
	}
	
	@Parameter(names = {"--exportflag"}, description = "sets export slot flag for IIO network node (TRUE, FALSE)", arity = 1)
	private Boolean exflag = null;
	public Boolean getExportFlag(){
		return exflag;
	}
	
	@Parameter(names = {"-f", "--file"}, description = "Name of a log file (if using Disk as input or output)")
	private String filename = "log.xio.gz";
	public String getFilename(){
		return filename;
	}
	
	@Parameter(names = {"-s", "--slotfile"}, description = "Name of a XML file with slot definitions")
	private String slotFilename;
	public String getSlotFilename(){
		return slotFilename;
	}
	
	@Parameter(names = {"--mcaddress"}, description = "Multicast Address")
	private String multicastAddress = "224.21.12.68";
	public String getMulticastAddress(){
		return multicastAddress;
	}
	
	@Parameter(names = {"--mcport"}, description = "Multicast Port")
	private int multicastPort = 4711;
	public int getMulticastPort(){
		return multicastPort;
	}
	
	@Parameter(names = {"--mcttl"}, description = "Multicast Time to Live")
	private int multicastTTL = 0;
	public int getMulticastTTL(){
		return multicastTTL;
	}
	
	@Parameter(names = {"--ucaddress"}, description = "Unicast Address")
	private String unicastAddress;
	public String getUnicastAddress(){
		return unicastAddress;
	}
	
	@Parameter(names = {"--ucport"}, description = "Unicast Port")
	private int unicastPort;
	public int getUnicastPort(){
		return unicastPort;
	}
	
	@Parameter(names = {"--ucservers"}, description = "Unicast Servers")
	private String unicastServers;
	public String getUnicastServers(){
		return unicastServers;
	}
	
	@Parameter(names = {"--writeRaw"}, description = "write unzipped data to disk")
	private boolean writeRaw = false;
	public boolean getWriteRaw(){
		return writeRaw;
	}
	
	@Parameter(names = {"--namespaceLabel"}, description = "Namespace label for creating InstantIO slots")
	private String namespaceLabel = "VeniceHub";
	public String getNamespaceLabel(){
		return namespaceLabel;
	}
	
	@Parameter(names = {"--headerlines"}, description = "number of lines to ignore")
	private int headerlines = 2;
	public int getHeaderlines(){
		return headerlines;
	}
	
	@Parameter(names = {"--header"}, description = "header line for logfiles")
	private String header = "<?xml version=\"1.0\"?>";
	public String getHeader(){
		return header;
	}
	
	@Parameter(names = {"--rsbtoXIO"}, description = "write XIO strings to RSB")
	private boolean rsbtoXIO = false;
	public boolean getRSBtoXIO(){
		return rsbtoXIO;
	}
	
	@Parameter(names = {"--rsbStringsAreXIO"}, description = "Strings received via RSB are XIO lines")
	private boolean rsbStringsAreXIO = false;
	public boolean getRSBStringsAreXIO(){
		return rsbStringsAreXIO;
	}
	
	@Parameter(names = {"--rsbDefInfScope"}, description = "default scope for RSB informers")
	private String RSBDefaultInformerScope = "/";
	public String getRSBDefaultInformerScope(){
		return RSBDefaultInformerScope;
	}
	
	@Parameter(names = {"--queueCapacity"}, description = "number of events that can be hold in memory")
	private int queueCapacity = 10000;
	public int getQueueCapacity(){
		return queueCapacity;
	}
	
	@Parameter(names = {"--bufferCapacity"}, description = "max. number of lines for diskreaders buffer")
	private int bufferCapacity = 1000000;
	public int getBufferCapacity(){
		return bufferCapacity;
	}
	
	@Parameter(names = {"--bufferThreshold"}, description = "threshold for diskreaders buffer to switch")
	private int bufferThreshold = 100000;
	public int getBufferThreshold(){
		return bufferThreshold;
	}
	
	@Parameter(names = {"--bufferMinimumSkip"}, description = "min. bytes to use fast skipping while seeking")
	private int bufferMinimumSkipAmount = 2000;
	public int getBufferMinimumSkipAmount(){
		return bufferMinimumSkipAmount;
	}
	
	@Parameter(names = {"--numLinesToEstBPS"}, description = "num. of lines for Byte/Sec. estimating")
	private int numOfLinesToEstBPSFromReplay = 10000;
	public int getNumOfLinesToEstBPSFromReplay(){
		return numOfLinesToEstBPSFromReplay;
	}
	
	@Parameter(names = {"--toSmallProgress"}, description = "to re-estimate BPS while seeking")
	private double toSmallProgress = 0.01;
	public double getToSmallProgress(){
		return toSmallProgress;
	}
	
	@Parameter(names = {"--bigEnoughProgress"}, description = "to keep BPS while seeking")
	private double bigEnoughProgress = 0.99;
	public double getBigEnoughProgress(){
		return bigEnoughProgress;
	}
	
	@Parameter(names = {"--noRPC"}, description = "don't create RPC server when replaying")
	private boolean noRPC = false;
	public boolean isNoRPC(){
		return noRPC;
	}
	
	@Parameter(names = {"--rpcServerAdress"}, description = "adress for RPC connection")
	private String rpcServerAdress = "localhost";
	public String getRPCServerAdress(){
		return rpcServerAdress;
	}
	
	@Parameter(names = {"--rpcServerPort"}, description = "port for RPC connection")
	private int rpcServerPort = 4243;
	public int getRPCServerPort(){
		return rpcServerPort;
	}
	
	@Parameter(names = {"--lagHistory"}, description = "enable lag measuring and history")
	private boolean lagHistory = false;
	public boolean isLagHistoryEnabled(){
		return lagHistory;
	}
	
	@Parameter(names = {"--lagLogN"}, description = "store only every Nth lag point in history")
	private int lagLogN = 1;
	public int getLagLogN(){
		return lagLogN;
	}
	
	@Parameter(names = {"--lagLogFile"}, description = "name of file for lag history")
	private String lagLogFile = "lag.dat";
	public String getLagLogFile(){
		return lagLogFile;
	}
	
	@Parameter(names = {"--silent"}, description = "suppress messages")
	private boolean silent = false;
	public boolean isSilent(){
		return silent;
	}
	
	@Parameter(names = {"--offset"}, description = "replay offset (ms)")
	private long offset = 0;
	public long getOffset(){
		return offset;
	}
	
	@Parameter(names = {"--parser"}, description = "which parser to use (DOM or REGEX)", validateWith = XIOParserValidator.class)
	private String parser = "REGEX";
	public String getParser(){
		return parser;
	}
	
	@Parameter(names = {"--protobuf"}, description = "protobuf directory")
	private String protobuf;
	public String getProtobuf(){
		return protobuf;
	}
	
	@Parameter(names = {"--classMatcher"}, description = "name of XML file with class matching definitions")
	private String classmatch;
	public String getClassMatcher(){
		return classmatch;
	}
	
	@Parameter(names = {"-x", "--xiocodes"}, description = "name of XML file with XIO code definitions")
	private String xiocodefile;
	public String getXIOCodeFile(){
		return xiocodefile;
	}
	
	@Parameter(names = {"--quitIfIdle"}, description = "quit if idle (0 for no quiting)")
	private long quitIfIdle = 0;
	public long getQuitIfIdle(){
		return quitIfIdle;
	}
	
	@Parameter(names = {"--sendInitValue"}, description = "send initialization value for predefined IIO slots")
	private boolean sendInitValue = false;
	public boolean getSendInitValue(){
		return sendInitValue;
	}
	
	@Parameter(names = {"--vpport"}, description = "set port for VenicePort connection")
	private int vpPort;
	public int getVPPort(){
		return vpPort;
	}
	
	@Parameter(names = {"-v", "--vpfile"}, description = "set slot file for VP mode")
	private String vpFile;
	public String getVPFile(){
		return vpFile;
	}
	
	@Parameter(names = {"--maxprio"}, description = "sets maximum priority for reader/writer threads")
	private boolean maxprio = false;
	public boolean getMaxPrio(){
		return maxprio;
	}
}
