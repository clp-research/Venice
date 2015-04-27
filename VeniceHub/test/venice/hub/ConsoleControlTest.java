package venice.hub;

import static org.junit.Assert.*;

import org.junit.Test;

import venice.hub.ConsoleControl;
import venice.hub.VeniceControl;

public class ConsoleControlTest {

	/**
	 * Tests if ConsoleControl can be created and stopped.
	 */
	@Test
	public void testConsoleControl(){
		VeniceControl consoleControl = new ConsoleControl();
        Thread consoleControlThread = new Thread(consoleControl, "Console controller (test)");
        consoleControlThread.start();
        
        assertFalse(consoleControl.isFinished());
        
        // wait a bit
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        // now stop the thread
        consoleControl.stopThread();
        
        // and wait a bit again
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
        
        // now the thread should be finished
        assertTrue(consoleControl.isFinished());
	}
}
