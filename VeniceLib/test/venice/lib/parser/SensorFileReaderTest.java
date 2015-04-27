package venice.lib.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import venice.lib.AbstractSlot;

public class SensorFileReaderTest {
	
	private static Logger logger;
	
	@BeforeClass
	public static void beforeClass(){
		logger = Logger.getLogger(LUTablesTest.class);
	}
	
	@Test
	public void testParseFixedFile(){
		ArrayList<AbstractSlot> slotList = SensorFileReader.parse(new File("C:\\Users\\das\\Documents\\Eclipse\\venice.hub\\sensor.xml"));
		
		// check the output of the parser
		
		assertEquals(3, slotList.size());
		
		assertEquals("timestring", slotList.get(0).getLabel());
		assertEquals("VeniceHub/testlog", slotList.get(0).getNamespace());
		assertEquals(String.class, slotList.get(0).getType());
		
		assertEquals("milliseconds", slotList.get(1).getLabel());
		assertEquals("VeniceHub/testlog", slotList.get(1).getNamespace());
		assertEquals(Integer.class, slotList.get(1).getType());
		
		assertEquals("MFString", slotList.get(2).getLabel());
		assertEquals("VeniceHub/testlog", slotList.get(2).getNamespace());
		assertEquals(String[].class, slotList.get(2).getType());
	}
	
	@Test
	public void testParse(){
		// create a test file
		
		String fileName = "testParse"+System.currentTimeMillis()+".xml";
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
		
		// test values
		String sensor = "testvenicelib"+System.currentTimeMillis();
		String namespace = "testnamespace"+System.currentTimeMillis();
		String slot1 = "testslotA"+System.currentTimeMillis();
		String slot2 = "testslotB"+System.currentTimeMillis();
		String type1 = "sfstring";
		String type2 = "mffloat";
		
		try {
			writer.write("<?xml version=\"1.0\"?>\n");
			writer.write("<Sources>\n");
			
			// old style
			writer.write("  <Sensor name=\"" + sensor + "\">\n");
			writer.write("    <Namespace name=\"" + namespace + "\">\n");
			writer.write("      <slot name=\"" + slot1 + "\" type=\"" + type1 + "\"/>\n");
			writer.write("      <slot name=\"" + slot2 + "\" type=\"" + type2 + "\"/>\n");
			writer.write("    </Namespace>\n");
			writer.write("  </Sensor>\n");
			
			// new style
			writer.write("  <testAbc>\n");
			writer.write("    <Def>\n");
			writer.write("      <Xyz type=\"sfint32\"/>\n");
			writer.write("    </Def>\n");
			writer.write("    <Uvw type=\"sfbool\"/>\n");
			writer.write("  </testAbc>\n");
			writer.write("  <testOutside type=\"mfstring\"/>\n");
			
			writer.write("</Sources>\n");
			writer.close();
		} catch (IOException e) {
			logger.error("Could not write to file "+fileName+"!");
		}
		
		file.deleteOnExit(); // file will be deleted when JVM exits
		
		// let the parser read the file
		ArrayList<AbstractSlot> slotList = SensorFileReader.parse(new File(fileName));
		
		// check the output of the parser
		
		assertEquals(5, slotList.size());
		
		assertEquals(slot1, slotList.get(0).getLabel());
		assertEquals(sensor+"/"+namespace, slotList.get(0).getNamespace());
		assertEquals(String.class, slotList.get(0).getType());
		
		assertEquals(slot2, slotList.get(1).getLabel());
		assertEquals(sensor+"/"+namespace, slotList.get(1).getNamespace());
		assertEquals(Float[].class, slotList.get(1).getType());
		
		assertEquals("Xyz", slotList.get(2).getLabel());
		assertEquals("testAbc/Def", slotList.get(2).getNamespace());
		assertEquals(Integer.class, slotList.get(2).getType());
		
		assertEquals("Uvw", slotList.get(3).getLabel());
		assertEquals("testAbc", slotList.get(3).getNamespace());
		assertEquals(Boolean.class, slotList.get(3).getType());
		
		assertEquals("testOutside", slotList.get(4).getLabel());
		assertEquals("", slotList.get(4).getNamespace());
		assertEquals(String[].class, slotList.get(4).getType());
	}
}
