package venice.hub.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import venice.lib.parser.SlotEvent;

/**
 * Unit Tests for TTEBuffer
 * 
 * @author Oliver Eickmeyer
 */
public class TTEBufferTests {

	/**
	 * Tests with an empty TTEBuffer.
	 */
	@Test
	public void testEmptyTTEBuffer(){
		TTEBuffer tb = new TTEBuffer();
		assertEquals(0, tb.getReadingPosition());
		assertEquals(0, tb.size());
		assertEquals(0, tb.getFirstTimestamp());
		assertEquals(0, tb.getLastTimestamp());
		assertNull(tb.getNext());
		assertNull(tb.peek());
	}
	
	/**
	 * Tests filling the buffer.
	 */
	@Test
	public void testFilling(){
		TTEBuffer tb = new TTEBuffer();
		SlotEvent event1st = new SlotEvent("first", "", "testslot", String.class, 100l);
		SlotEvent event2nd = new SlotEvent("second", "", "testslot", String.class, 200l);
		SlotEvent event3rd = new SlotEvent("third", "", "testslot", String.class, 300l);
		tb.add(event1st);
		tb.add(event2nd);
		tb.add(event3rd);
		assertEquals(3, tb.size());
		assertEquals(100l, tb.getFirstTimestamp());
		assertEquals(300l, tb.getLastTimestamp());
		assertEquals(0, tb.getReadingPosition());
		assertEquals(event1st, tb.peek());
		
		// read the next one (should be the 1st)
		SlotEvent eventRead = tb.getNext();
		assertEquals(1, tb.getReadingPosition());
		assertEquals(event1st, eventRead);
		assertEquals(event2nd, tb.peek());
		
		// read the next one (should be the 2nd)
		eventRead = tb.getNext();
		assertEquals(2, tb.getReadingPosition());
		assertEquals(event2nd, eventRead);
		assertEquals(event3rd, tb.peek());
		
		// read the next one (should be the 3rd)
		eventRead = tb.getNext();
		assertEquals(3, tb.getReadingPosition());
		assertEquals(event3rd, eventRead);
		assertEquals(null, tb.peek());
		
		// read the next one (there should be nothing)
		eventRead = tb.getNext();
		assertEquals(3, tb.getReadingPosition());
		assertEquals(null, eventRead);
		assertEquals(null, tb.peek());
	}
	
	/**
	 * Tests navigating inside the buffer, that is changing the
	 * reading position.
	 */
	@Test
	public void testNavigating(){
		TTEBuffer tb = new TTEBuffer();
		long timefactor = 100l;
		int n = 1000; // number of events
		for(int i=0; i < n; i++){
			tb.add(new SlotEvent(i, "", "testslot", Integer.class, i * timefactor));
		}
		
		// jump to absolute position
		int testPosition = 500;
		tb.setReadingPosition(testPosition);
		assertEquals(testPosition, tb.getReadingPosition());
		assertEquals(testPosition * timefactor, tb.peek().getTime());
		
		// jump to relative position
		int testDelta = -10;
		tb.addReadingPosition(testDelta);
		assertEquals( testPosition + testDelta, tb.getReadingPosition());
		assertEquals((testPosition + testDelta) * timefactor, tb.peek().getTime());
		
		// jump to far
		tb.setReadingPosition(n + 1);
		assertEquals(null, tb.peek());
		assertEquals(null, tb.getNext());
		
		// jump below zero
		tb.setReadingPosition(-1);
		boolean failed = false;
		try{
			tb.peek();
		}
		catch(ArrayIndexOutOfBoundsException e){
			failed = true;
		}
		assertTrue(failed);
	}
	
	/**
	 * Tests removing events from the buffer.
	 */
	@Test
	public void testRemoving(){
		TTEBuffer tb = new TTEBuffer();
		long timefactor = 100l;
		int n = 1000; // number of events
		for(int i=0; i < n; i++){
			tb.add(new SlotEvent(i, "", "testslot", Integer.class, i * timefactor));
		}
		
		// remove all but the last one and check number of removed events
		assertEquals(n-1 , tb.removeUntil( (n-2) * timefactor));
		
		// check remaining size and timestamps
		assertEquals(1, tb.size());
		assertEquals( (n-1) * timefactor, tb.getFirstTimestamp());
		assertEquals( (n-1) * timefactor, tb.getLastTimestamp());
	}
}
