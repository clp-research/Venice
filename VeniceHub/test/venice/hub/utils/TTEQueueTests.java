package venice.hub.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import venice.lib.parser.SlotEvent;

public class TTEQueueTests {
	
	@Test
	public void testQueueBasicFunctionality(){
		TTEQueue queue = TTEQueue.getInstance();
		
		// new TTEQueue should be empty:
		assertEquals(0, queue.size());
		assertEquals(true, queue.isEmpty());
		assertNull(queue.peek());
		
		SlotEvent se = new SlotEvent("Teststring", "", "Testslot", String.class, System.currentTimeMillis());
		
		try {
			queue.put(se);
		} catch (InterruptedException e) {
			// nothing
		}
		
		assertEquals(1, queue.size());
		assertEquals(false, queue.isEmpty());
		SlotEvent sePeek = queue.peek();
		assertEquals(se.getValue(), sePeek.getValue());
		assertEquals(se.getLabel(), sePeek.getLabel());
		assertEquals(se.getType(), sePeek.getType());
		assertEquals(se.getTime(), sePeek.getTime());
		
		// empty the queue
		queue.poll();
		
		// create a timed SlotEvent that will be available after a short time
		long targetTime = System.currentTimeMillis() + 100L;
		SlotEvent seTimed = new SlotEvent("StringTimed", "", "SlotTimed", String.class, targetTime);
		try {
			queue.put(seTimed);
		} catch (InterruptedException e) {
			// nothing
		}
		
		// at this point the SlotEvent should not be available
		assertEquals(null, queue.poll());
		
		// wait until it should be available
		while(System.currentTimeMillis() < targetTime){
			try {
				long waitingTime = targetTime - System.currentTimeMillis();
				if(waitingTime < 1) waitingTime = 1;
				Thread.sleep(waitingTime);
			} catch (InterruptedException e) {
				// nothing
			}
		}
		
		// now it should be available
		SlotEvent sePoll = queue.poll();
		assertEquals(seTimed.getValue(), sePoll.getValue());
		assertEquals(seTimed.getLabel(), sePoll.getLabel());
		assertEquals(seTimed.getType(), sePoll.getType());
		assertEquals(seTimed.getTime(), sePoll.getTime());
	}
}
