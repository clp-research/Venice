package venice.lib.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit Tests for testing the methods of the XIOParser classes.
 * 
 * @author Oliver Eickmeyer
 *
 */
public class XIOParserTest {
	
	/**
	 * Tests the method for finding the slot label in a string.
	 */
	@Test
	public void testFindSlotLabel(){
		
		assertEquals("slot1", XIOParser.findSlotLabel("tests/unittest/slot1"));
		assertEquals("slot2", XIOParser.findSlotLabel("slot2"));
		assertEquals("", XIOParser.findSlotLabel(""));
		assertEquals(null, XIOParser.findSlotLabel(null));
		assertEquals("", XIOParser.findSlotLabel("tests/unittest/"));
		
	}
	
	/**
	 * Tests the method for finding the namespace label in a string.
	 */
	@Test
	public void testFindNamespaceLabel(){
		
		assertEquals("tests/unittest", XIOParser.findNamespaceLabel("tests/unittest/slot1"));
		assertEquals("", XIOParser.findNamespaceLabel("slot2"));
		assertEquals("", XIOParser.findNamespaceLabel(""));
		assertEquals(null, XIOParser.findNamespaceLabel(null));
		assertEquals("tests/unittest", XIOParser.findNamespaceLabel("tests/unittest/"));
		
	}
	
	/**
	 * Tests parsing in both ways: Line to event, and event to line.<br>
	 * Tests both XIO formats: old and new.<br>
	 * Tests XIORegExParser and XIODomParser.
	 */
	@Test
	public void testParsing() {
		
		// create some test sets
		List<Testset> testlist = new ArrayList<Testset>();
		
		// test parsing of an easy line with both, old and new format
		testlist.add( new Testset(
				"<sfstring value=\"hello\" timestamp=\"123\" sensorName=\"testsensor\"/>",
				"<irio:sfstring value=\"hello\" timestamp=\"123\" sensorName=\"testsensor\"></irio:sfstring>",
				String.class,
				"hello",
				123l,
				"testsensor"
				));

		// create a test set with sfint32
		int int32value = 987;
		testlist.add( new Testset(
				"<sfint32 value=\""+int32value+"\" timestamp=\"12\" sensorName=\"testSFInt32\"/>",
				Integer.class,
				int32value,
				12l,
				"testSFInt32"
				));
				
		// create a test set with mffloat
		Float[] floatvalues = new Float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
		testlist.add( new Testset(
				"<mffloat value=\""+Arrays.toString(floatvalues)+"\" timestamp=\"12\" sensorName=\"testMFFloat\"/>",
				floatvalues.getClass(),
				floatvalues,
				12l,
				"testMFFloat"
				));
		
		// create a test set with mfstring
		String[] stringvalues = new String[]{"Alpha", "Beta", "Gamma"};
		testlist.add( new Testset(
				"<mfstring value=\""+Arrays.toString(stringvalues)+"\" timestamp=\"123\" sensorName=\"testsensor\"/>",
				stringvalues.getClass(),
				stringvalues,
				123l,
				"testsensor"
				));
		
		// create a test with the word 'timestamp' appearing in the value
		// to test the preparser
		testlist.add( new Testset(
				"<sfstring value=\"timestamp\" timestamp=\"123\" sensorName=\"testsensor\"/>",
				"<irio:sfstring value=\"timestamp\" timestamp=\"123\" sensorName=\"testsensor\"></irio:sfstring>",
				String.class,
				"timestamp",
				123l,
				"testsensor"
				));
		
		// create a test with the word 'timestamp' appearing in the slotlabel
		// to test the preparser
		testlist.add( new Testset(
				"<sfstring value=\"hello\" timestamp=\"123\" sensorName=\"timestamp\"/>",
				"<irio:sfstring value=\"hello\" timestamp=\"123\" sensorName=\"timestamp\"></irio:sfstring>",
				String.class,
				"hello",
				123l,
				"timestamp"
				));
		
		// create a test with some format differences
		List<String> lines = new ArrayList<String>();
		lines.add("<sfstring value=\"hello test\" timestamp=\"123\" sensorName=\"timestamp\"/>");
		lines.add("<sfstring value=\"hello test\" timestamp=\"123\" sensorname=\"timestamp\"/>");
		lines.add("<SFSTRING value=\"hello test\" timestamp=\"123\" sensorname=\"timestamp\"/>");
//		lines.add("<sfstring  value = \"hello test\"  timestamp=\"123\"  sensorName=\"timestamp\" />"); // RegEx preparsing fails here
//		lines.add("<sfstring value=\"hello test\" sensorName=\"timestamp\" timestamp=\"123\"/>"); // RegEx preparsing fails here
		testlist.add( new Testset(
				lines,
				String.class,
				"hello test",
				123l,
				"timestamp"
				));
		
		// test every parser
		for(XIOParser parser : getParsers()){
			
			// test parser with every test set
			for(Testset testset : testlist){
				
				// test with every line in the test set
				for(String line : testset.getLines()){
				
					// test parsing of lines into events
					SlotEvent event = parser.stringToEvent(line);
					String msg = parser.getClass().getSimpleName()+".stringToEvent ";
					assertEquals(msg, testset.getType(), event.getType());
					assertEquals(msg, testset.getType().isArray(), event.getType().isArray());
					if(event.getType().isArray())
						assertArrayEquals(msg+"and array ", (Object[])testset.getValue(), (Object[])event.getValue());
					else
						assertEquals(msg, testset.getValue(), event.getValue());
					assertEquals(msg, testset.getLabel(), event.getLabel());
					assertEquals(msg, testset.getTime(), event.getTime());
					
					// test parsing of an event into a line
					msg = parser.getClass().getSimpleName()+".eventToString ";
					String parsedline = parser.eventToString(testset.getEvent());
					assertEquals(msg, testset.getDefaultLine(), parsedline);
					
					// test preparsing of timestamp
					msg = parser.getClass().getSimpleName()+".preparseTS ";
					assertEquals(msg, testset.getTime(), parser.preparseTS(line));
				}
			}
		}
	}
	
	/**
	 * Tests if the parser fails correctly on some bad XIO lines.
	 */
	@Test
	public void testParserFails(){
		for(XIOParser parser : getParsers()){
			String msg = "Fail tests of" + parser.getClass().getSimpleName() + " ";
			
			// parser test with missing sensorname
			String line = "<sfstring value=\"hello test\" timestamp=\"123\"/>";
			SlotEvent parsedEvent = parser.stringToEvent(line);
			assertEquals(msg, String.class, parsedEvent.getType());
			assertEquals(msg, line, parsedEvent.getValue());
			assertEquals(msg, XIOParser.INVALID_TIMESTAMP, parsedEvent.getTime());
			
			// preparser test with missing timestamp
			line = "<sfstring value=\"hello test\" sensorname=\"testsensor\"/>";
			long preparsedTS= parser.preparseTS(line);
			assertEquals(msg + ", preparsing ", XIOParser.INVALID_TIMESTAMP, preparsedTS);
		}
	}
	
	/**
	 * Returns a list with all parsers to be tested.
	 * @return
	 */
	private List<XIOParser> getParsers(){
		List<XIOParser> parserlist = new ArrayList<XIOParser>();
		parserlist.add( new XIODomParser() );
		parserlist.add( new XIORegExParser() );
		return parserlist;
	}
	
	/**
	 * Defines a test set to test the parser classes.<br>
	 * Each test set holds<br>
	 * - at least one XIO line that represents the event fields<br>
	 * - a complete set of event fields, represented by the XIO lines<br>
	 * <br>
	 * The first line is also called the default line and should use the
	 * format that is preferred by the parser when parsing an event to a line.
	 *  
	 * @author Oliver Eickmeyer
	 */
	private class Testset{
		private final List<String> lines;
		private final SlotEvent event;
		
		/**
		 * Constructs a new test set with a line and the corresponding
		 * event fields.
		 * @param line  a string representing the event
		 * @param type  class of the value of the event
		 * @param value an object holding the value of the event
		 * @param time  the timestamp of the event
		 * @param label the slotlabel of the event
		 */
		public Testset(String line, Class<?> type, Object value, Long time, String label){
			ArrayList<String> newlist = new ArrayList<String>();
			newlist.add(line);
			this.lines = newlist;
			event = new SlotEvent(value, "", label, type, time);
		}
		
		/**
		 * Constructs a new test set with two lines that should represent the given
		 * event fields.<br>
		 * The reason for this constructor is the more convenient use if a test
		 * is needed with a new XIO line (default) and an old XIO line (additional). 
		 * 
		 * @param defaultLine    representing the event in default format
		 * @param additionalline an additional line representing the event in alternative format
		 * @param type  class of the value of the event
		 * @param value an object holding the value of the event
		 * @param time  the timestamp of the event
		 * @param label the slotlabel of the event
		 */
		public Testset(String defaultLine, String additionalline, Class<?> type, Object value, Long time, String label){
			ArrayList<String> newlist = new ArrayList<String>();
			newlist.add(defaultLine);
			newlist.add(additionalline);
			this.lines = newlist;
			event = new SlotEvent(value, "", label, type, time);
		}
		
		/**
		 * Constructs a new test set with a list of lines and the corresponding
		 * event fields. The first line should be in the default format, that is
		 * used by the parsers when parsing an event into a line.
		 * 
		 * @param lines list of strings representing the event
		 * @param type  class of the value of the event
		 * @param value an object holding the value of the event
		 * @param time  the timestamp of the event
		 * @param label the slotlabel of the event
		 */
		public Testset(List<String> lines, Class<?> type, Object value, Long time, String label){
			this.lines = lines;
			event = new SlotEvent(value, "", label, type, time);
		}
		
		/**
		 * Returns the default line that should represent the event.
		 * Default means that this is the format the parser would produce
		 * when parsing the event.
		 * @return
		 */
		public String getDefaultLine(){
			return lines.get(0);
		}
		
		/**
		 * Returns the list of lines that should represent the event.
		 * @return a list of lines representing the event
		 */
		public List<String> getLines(){
			return lines;
		}
		
		/**
		 * Returns the type of the value of the event.
		 * @return event type
		 */
		public Class<?> getType(){
			return event.getType();
		}
		
		/**
		 * Returns the value of the event.
		 * @return event value
		 */
		public Object getValue(){
			return event.getValue();
		}
		
		/**
		 * Returns the timestamp of the event.
		 * @return event timestamp
		 */
		public long getTime(){
			return event.getTime();
		}
		
		/**
		 * Returns the slotlabel of the event.
		 * @return event slotlabel
		 */
		public String getLabel(){
			return event.getLabel();
		}
		
		/**
		 * Returns the event, that should be represented by the line.
		 * @return event that should be represented by the lines
		 */
		public SlotEvent getEvent(){
			return event;
		}
		
		/**
		 * The string representation is just the default XIO line.
		 * 
		 * @return a string representation of this <code>Testset</code> object
		 */
		public String toString(){
			return lines.get(0);
		}
		
	}

}
