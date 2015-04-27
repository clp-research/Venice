package venice.lib.networkRSB;

import org.junit.Test;

import static org.junit.Assert.*;

public class RSBNamespaceBuilderTest {
	
	/**
	 * This unittest will create a RSB test handler listening to the default
	 * scope, then creates an outslot and then sends a test value to this
	 * outslot. After that it will check if the test value was received by the
	 * handler.
	 */
	@Test
	public void testSlotCreation(){
		
		// create a test handler for RSB events
		
		TestRSBSlotListener rsbListener = new TestRSBSlotListener();
		RSBNamespaceBuilder.setMasterInSlotListener(rsbListener);
		
		// dynamic slot creation
		
		RSBNamespaceBuilder.initializeOutSlots();
		RSBNamespaceBuilder.initializeInSlots();
		
		// wait a bit to give the initialization routine time to do the job
		
		try {
			Thread.sleep(100l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// define test values
		
		String code = String.valueOf(System.currentTimeMillis());
		String value = "unittest" + code;
		String label = "testscope" + code;
		
		// write test value to RSB
		
		RSBNamespaceBuilder.write(label, value);
		
		// wait a bit
		
		try {
			Thread.sleep(100l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		RSBNamespaceBuilder.removeAll();
		
		// check received values
		
		boolean foundTestValue = false;
		for(int i=0; i<rsbListener.getSize(); i++){
			Object dataRec = rsbListener.getData(i);
			Class<?> typeRec = rsbListener.getType(i);
			String labelRec = rsbListener.getLabel(i);
			
			if(	labelRec.equals(label) &&
				typeRec.equals(String.class) &&
				dataRec.equals(value) ){
				foundTestValue = true;
				break;
			}
		}
		assertTrue(foundTestValue);
		
	}
}
