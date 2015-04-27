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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import venice.hub.VeniceWriter;
import venice.lib.parser.SlotEvent;

/**
 * Writes compressed data to disk.
 * <p>
 * Opens a log file, getting the file name from Configuration.
 * If a file with that name already exists the name will be changed.<br>
 * For example: If <code>log.xio.gz</code> already exists, it will be changed to
 * <code>log_001.xio.gz</code>. If that exists too, it will be changed to
 * <code>log_002.xio.gz</code>, and so on.
 * <p>
 * The data will be converted to XIO lines.<br>
 * A header will be written in the beginning.<br>
 * The data will be compressed with GZIP, if configured so in Configuration.
 * 
 * @see VeniceWriter
 */
public class DiskWriter extends VeniceWriter{

	private OutputStream stream;
	private String logPath;
	private final String DEFAULT_HEADER = "Header";
	private static String roottag = "venice";
	
	protected void initialize(){
    	// get name+path for log file from configuration properties
        logPath = config.getLogFilePath();
        
        // check if this file already exists:
        if(new File(logPath).isFile()){
        	int p0 = logPath.indexOf(".");
        	String prefix, suffix, newLogPath;
        	if(p0 > -1){
        		prefix = logPath.substring(0, p0);
        		suffix = logPath.substring(p0);
        	}
        	else{
        		prefix = logPath;
        		suffix = "";
        	}
        	
        	int counter=1;
        	do{
        		newLogPath = prefix + String.format("_%03d", counter++) + suffix;
        	}while(new File(newLogPath).isFile()); // repeat, if new name already exists

        	VeniceHub.message(logPath+" already exists.\nChanging log file name to "+newLogPath);
        	logPath = newLogPath;
        }
        
        try {
        	VeniceHub.message("Logging to: " + logPath);
        	
        	// create a FileOutputStream and if gzip is used,
        	// put a GZIPOutputStream before
            stream = ! config.writeRaw() ?
            		new GZIPOutputStream(new FileOutputStream(logPath)) :
            		new FileOutputStream(logPath);
            
            // writing default header lines (TODO: needs to be replaced by a real header!)
            for(int n=0;n<config.getHeaderLines();n++){
            	stream.write(DEFAULT_HEADER.getBytes());
            	stream.write("\n".getBytes());
            }
            stream.write(("<"+roottag+">\n").getBytes());
        } catch (IOException ex) {
        	ex.printStackTrace();
        }
	}
	
	protected long write(SlotEvent se){
		if(se==null) return NO_TIMESTAMP;
		long writingTime = NO_TIMESTAMP;
		String in = parser.eventToString(se);
		try {
			stream.write((in+"\n").getBytes());
			writingTime = System.currentTimeMillis();
			lastTimestamp = se.getTime();
			VeniceHub.setLastPushedTimestamp(lastTimestamp);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return writingTime;
	}
	
    protected void cleanUp(){
		try {
			stream.write(("</"+roottag+">\n").getBytes());
			stream.flush();
			if(! config.writeRaw()) ((GZIPOutputStream) stream).finish();
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
