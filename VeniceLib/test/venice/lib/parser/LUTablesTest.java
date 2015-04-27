package venice.lib.parser;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class LUTablesTest {
	private static Logger logger;
	
	@BeforeClass
	public static void beforeClass(){
		logger = Logger.getLogger(LUTablesTest.class);
	}
	
	/**
	 * Tests XIOMaps for loading definitions from file correctly.<br>
	 * Creates a test file and let XIOMaps load defs from it.
	 * After that it is checked if defs have been loaded.
	 */
	@Test
	public void testLoadAdditionalDefs(){
		
		// first, create a test file
		
		String fileName = "testLoadAdditionalDefs.xml";
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
			writer.write("<codes>\n");
			writer.write("<def class=\"venice.lib.parser.XIOMaps\" code=\"sflut\"/>\n");
			writer.write("</codes>\n");
			writer.close();
		} catch (IOException e) {
			logger.error("Could not write to file "+fileName+"!");
		}
		
		file.deleteOnExit(); // file will be deleted when JVM exits
		
		
		// now create XIOMaps and let it load the test file
		
		int size1before = XIOMaps.getClass2strMap().size();
		int size2before = XIOMaps.getStr2classMap().size();
		
		XIOMaps.loadXIOCodes(fileName);
		
		int size1after = XIOMaps.getClass2strMap().size();
		int size2after = XIOMaps.getStr2classMap().size();
		
		
		// check, if the test file was loaded correctly
		
		assertEquals(size1after, size1before+1);
		assertEquals(size2after, size2before+1);
		
		assertEquals("sflut", XIOMaps.getClass2strMap().get(venice.lib.parser.XIOMaps.class));
		assertEquals(venice.lib.parser.XIOMaps.class, XIOMaps.getStr2classMap().get("sflut"));
	}

}
