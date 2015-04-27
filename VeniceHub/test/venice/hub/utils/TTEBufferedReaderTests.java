package venice.hub.utils;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import venice.hub.VeniceHub;
import venice.lib.parser.SlotEvent;
import venice.lib.parser.XIODomParser;

/**
 * Tests functionality of the TTEBufferedReader.
 * Will create a test file and let the TTEBufferedReader
 * read that file. It will check for data loss.
 * 
 * @author Oliver Eickmeyer
 *
 */
public class TTEBufferedReaderTests {
	
	private static Logger logger;
	
	@BeforeClass
	public static void beforeClass(){
		logger = Logger.getLogger(TTEBufferedReaderTests.class);
	}

	@Test
	public void test(){
		
		// create a test file
		
		int n = 1000; // number of events for the testfile
		String fileName = "testTBR"+System.currentTimeMillis()+".xio";
		File file = new File(fileName);
		boolean fileCreated = false;
		try {
			fileCreated = file.createNewFile();
		} catch (IOException e) {
			logger.fatal("Could not create test file "+fileName+"!");
		}
		
		assertTrue(fileCreated);
		
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
		} catch (IOException e) {
			logger.fatal("Could not create file writer for "+fileName+"!");
		}
		
		assertNotNull(writer);
		
		try {
			writer.write("<?xml version=\"1.0\"?>\n");
			writer.write("<headerline/>\n");
			for(int i = 0; i < n; i++){
				writer.write("<sfint32 value=\""+i+"\" timestamp=\""+i+"\" sensorname=\"testslot\"/>\n");
			}
			writer.flush();
		}
		catch (IOException e) {
			logger.error("Could not write to file "+fileName+"!");
		}
		
		try {
			writer.close();
		} catch (IOException e1) {
			logger.error("Can not close writer.");
		}
		
		// create a TTEBufferedReader
		VeniceHub.setMessageEnabled(true);
		VeniceHub.setPreferredXIOParser(new XIODomParser());
		TTEBufferedReader tbr = new TTEBufferedReader(fileName);
		Thread tbrThread = new Thread(tbr, "Test_TBR");
    	tbrThread.start();
    	SynchronousQueue<SlotEvent> syncQ = tbr.getSyncQ();
    	
    	// get data from the TTEBufferedReader
    	// and check it for correctness
    	Integer counter = 0;
    	boolean active = true;
    	while(active){
    		SlotEvent se = syncQ.poll();
    		if(se != null){
    			assertTrue(counter < n);
    			assertEquals(counter, (Integer)se.getValue());
    			counter++;
	    		if(counter == n)
	    			active = false;
    		}
    	}
    	
    	// destroy TTEBufferedReader
    	tbr.stopThread();
		
    	// file will be deleted when JVM exits
		file.deleteOnExit();
	}
}
