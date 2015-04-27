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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.Rotation;
import org.instantreality.InstantIO.Vec2f;
import org.instantreality.InstantIO.Vec3f;

import venice.hub.utils.TTEQueue;
import venice.lib.AbstractSlot;
import venice.lib.parser.SlotEvent;
import venice.lib.parser.XIOMaps;

/**
 * Reads data from a TCP port.
 * 
 * @author Oliver Eickmeyer
 */
public class PortReader extends VeniceReader{

	static {
		// setup logger
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(PortReader.class);
	}
	
	public final static int NO_ERROR = 0;
	public final static int ERROR_FORMAT = 1;
	public final static int ERROR_CHECKSUM = 2;
	private Socket client;
	ArrayList<AbstractSlot> slotArray;
	
	protected void initialize(){
		
		/* ensure, that InstantIO-Types are mapped,
		 * so that the sensorfile-parser will recognize
		 * the InstantIO-Types
		 */
		ensureIIOTypeMapping();
				
		/* read sensor file,
		 * so that the line-parser knows how the line is build up
		 */
		if(config.getVPFile() == null)
			slotArray = null;
		else
			slotArray = venice.lib.parser.SensorFileReader.parse(new File(config.getVPFile()));
		
		if(slotArray == null){
			VeniceHub.message("Error: Missing sensor file for VenicePort protocol.");
			active = false;
		}
		
		/* check, if unsupported types are used and
		 * print out a warning, if so
		 */
		checkForUnsupportedTypes(slotArray);
		
		// start the TCP server
		startTCPServer(config.getVPPort());
	}
	
	/**
	 * Checks, if the InstantIO types used by this class are mapped and
	 * if not, map them.
	 * <br>
	 * Usually they will be mapped if an InstantIO-related target connection
	 * is active. But for a disk target, they can be missing (if no
	 * xiocodes-file is explicitly given with the --xiocodes argument).
	 */
	private void ensureIIOTypeMapping(){
		try{
			if(XIOMaps.getStr2classMap().get("sfvec2f") == null) XIOMaps.putPair("sfvec2f", Vec2f.class);
			if(XIOMaps.getStr2classMap().get("sfvec3f") == null) XIOMaps.putPair("sfvec3f", Vec3f.class);
			if(XIOMaps.getStr2classMap().get("sfrotation") == null) XIOMaps.putPair("sfrotation", Rotation.class);
			if(XIOMaps.getStr2classMap().get("mfvec2f") == null) XIOMaps.putPair("mfvec2f", Vec2f[].class);
			if(XIOMaps.getStr2classMap().get("mfvec3f") == null) XIOMaps.putPair("mfvec3f", Vec3f[].class);
			if(XIOMaps.getStr2classMap().get("mfrotation") == null) XIOMaps.putPair("mfrotation", Rotation[].class);
		}
		catch(NoClassDefFoundError e){
			// TODO: Find a better solution
		}
	}
	
	private void startTCPServer(int port){
		// start TCP server
		ServerSocket server = null;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(server==null){
			VeniceHub.message("ERROR: Can't start TCP Server.");
			active = false;
			return;
		}
		
		// wait for client
		VeniceHub.message("Waiting for connection...");
		while(client==null){
			try {
				client = server.accept();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		VeniceHub.message("Connected to "+client.toString());
	}
	
	/**
     * Keeps the thread alive, until deactivated.
     */
    @Override
    public void run(){
    	// create buffered reader for data from client
		BufferedReader input=null;
		try {
			input = new BufferedReader(new InputStreamReader(client.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(input==null){
			VeniceHub.message("ERROR: Can't create buffered reader for client input.");
			active = false;
		}
		
        while(this.active){
        	// read data from client
    		String line=null;
			try {
				line = input.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if(line != null){
				// parse the line and push the content to queue
				int result = parseLineAndPush(line);
				if(result != NO_ERROR){
					active = false;
				}
			}
			else{
				VeniceHub.message("No more input from port. Closing connection.");
				active = false; // no line -> end connection
			}
            /*try {
            	synchronized(this){
            		wait(100L);
            	}
            } catch (InterruptedException ex) {
                // nothing
            }*/
            synchronized(this){
            	while(paused && active){
            		try {
            			// check eventually if this thread was commanded to stop
						wait(VeniceHub.CHECK_IF_STOPPED_WHILE_PAUSED_INTERVAL);
					} catch (InterruptedException e) {}
            	}
            }
        }
        VeniceHub.message("PortReader finished");
        finished = true;
    }
    
	/**
	 * Parses the line and pushes the content to the queue.
	 * 
	 * @param line the line to be parsed
	 */
	private int parseLineAndPush(String line){
		// split the string between the parenthesis by comma
		String[] tokens = line.split(", ");
		
		int numOfFields; // for multi fields
		Adler32 ckBuilder; // for checksum calculation (Checksum-Builder)
		String reducedLine; // for checksum (the line w/o checksum+timestamp)
		Long ck_in; // checksum parsed from the line
		
		Class<?> type;
		
		int m=0; // index for the position in the array of tokens
		
		// getting in-checksum:
		// checksum has always to be the first last value in the line
		ck_in = Long.parseLong(tokens[m++]);
		
		// Checksum
		ckBuilder = new Adler32();
		// reduce line by first token (checksum)
		reducedLine = line.substring(line.indexOf(',')+2);
		// generate checksum
		ckBuilder.update(reducedLine.getBytes());
		// check the checksum
		if(ck_in!=ckBuilder.getValue()){
			System.err.println("WARNING: Checksum "+ckBuilder.getValue()+" does not match for \""+line+"\"");
			return ERROR_CHECKSUM;
		}
		// go through each type-entry, parse content and push it (n: index of type-array)
		for(int n=0; n<slotArray.size(); n++){
			type = slotArray.get(n).getType();
			try{
				if(type==Integer.class){
					write(n, new Integer(tokens[m++]));
				}
				else if(type==Long.class){
					write(n, new Long(tokens[m++]));
				}
				else if(type==Float.class){
					write(n, new Float(tokens[m++]));
				}
				else if(type==String.class){
					write(n, tokens[m++]);
				}
				else if(type==Boolean.class){
					write(n, new Boolean(tokens[m++]));
				}
				else if(type==String[].class){
					numOfFields = Integer.parseInt(tokens[m++]); // first value has to indicate the number of fields
					String[] str = new String[numOfFields];
					for(int f=0; f<numOfFields; f++){
						str[f] = tokens[m++];
					}
					write(n, str);
				}
				else if(type==Vec2f.class){
					write(n, new Vec2f( Float.parseFloat(tokens[m++]),
							            Float.parseFloat(tokens[m++])));
				}
				else if(type==Vec3f.class){
					write(n, new Vec3f( Float.parseFloat(tokens[m++]),
							            Float.parseFloat(tokens[m++]),
							            Float.parseFloat(tokens[m++])));
				}
				else if(type==Rotation.class){
					write(n, new Rotation( Float.parseFloat(tokens[m++]),
							               Float.parseFloat(tokens[m++]),
							               Float.parseFloat(tokens[m++]),
							               Float.parseFloat(tokens[m++])));
				}
				else if(type==Vec2f[].class){
					numOfFields = Integer.parseInt(tokens[m++]); // first value has to indicate the number of fields
					Vec2f[] mfvec2f = new Vec2f[numOfFields];
					for(int f=0; f<numOfFields; f++){
						mfvec2f[f] = new Vec2f( Float.parseFloat(tokens[m++]),
								                Float.parseFloat(tokens[m++]));
					}
					write(n, mfvec2f);
				}
				else if(type==Vec3f[].class){
					numOfFields = Integer.parseInt(tokens[m++]); // first value has to indicate the number of fields
					Vec3f[] mfvec3f = new Vec3f[numOfFields];
					for(int f=0; f<numOfFields; f++){
						mfvec3f[f] = new Vec3f( Float.parseFloat(tokens[m++]),
								                Float.parseFloat(tokens[m++]),
								                Float.parseFloat(tokens[m++]));
					}
					write(n, mfvec3f);
				}
				else if(type==Rotation[].class){
					numOfFields = Integer.parseInt(tokens[m++]); // first value has to indicate the number of fields
					Rotation[] mfrot = new Rotation[numOfFields];
					for(int f=0; f<numOfFields; f++){
						mfrot[f] = new Rotation( Float.parseFloat(tokens[m++]),
								                 Float.parseFloat(tokens[m++]),
								                 Float.parseFloat(tokens[m++]),
								                 Float.parseFloat(tokens[m++]));
					}
					write(n, mfrot);
				}
			}catch(NumberFormatException | ArrayIndexOutOfBoundsException e){
				printError(e, tokens, m-1, type);
				return ERROR_FORMAT;
			}
		}
		return NO_ERROR;	
	}
	
	/**
	 * Writes object to the slot of the given index.
	 * @param i index of the slot
	 * @param value the value to be send
	 */
	private void write(int i, Object value){
		AbstractSlot as = slotArray.get(i);
		SlotEvent e = new SlotEvent(value, as.getNamespace(), as.getLabel(), as.getType(), System.currentTimeMillis());
		try {
			TTEQueue.getInstance().put(e);
		} catch (InterruptedException e1) {
			logger.error("Can't push "+value+" to "+as.getScope());
		}
	}
	
	/**
	 * Prints an error message for an error occuring while parsing an input line.
	 * @param tokens The tokens from the input line.
	 * @param m The index of the tokens that caused the format exception.
	 * @param type The expected type of the token.
	 */
	private void printError(Exception e, String[] tokens, int m, Class<?> type){
		VeniceHub.message("Error:");
		for(int i=0; i<tokens.length; i++){
			String s = tokens[i];
			if(i == m) s = "> " + s;
			else s = "  " + s;
			VeniceHub.message(String.format("%3d: %s", i, s));
		}
		if(e instanceof NumberFormatException){
			VeniceHub.message("Token "+m+" is of wrong type.");
		}
		else if(e instanceof ArrayIndexOutOfBoundsException){
			VeniceHub.message("To few tokens in the line.");
		}
		else{
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks the slots in the given slot array for unsupported types and
	 * prints out a warning.
	 * @param slotArray
	 */
	private void checkForUnsupportedTypes(ArrayList<AbstractSlot> slotArray){
		for(AbstractSlot as: slotArray){
			String typeName = as.getType().getName();
			String unsupported = null;
			if(typeName.equals("[Ljava.lang.Boolean;")) unsupported = "mfbool";
			if(typeName.equals("[Ljava.lang.Integer;")) unsupported = "mfint32";
			if(typeName.equals("java.lang.Long")) unsupported = "sflong";
			if(typeName.equals("[Ljava.lang.Long;")) unsupported = "mflong";
			if(typeName.equals("[Ljava.lang.Float;")) unsupported = "mffloat";
			if(typeName.equals("[Ljava.lang.Double;")) unsupported = "mfdouble";
			if(unsupported != null){
				System.err.println("Warning: "+unsupported+" is not supported by InstantIO.");
			}
		}
	}
}
