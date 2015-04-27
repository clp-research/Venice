package venice.lib;

import static org.junit.Assert.*;

import org.junit.Test;

public class AbstractSlotTest {

	@Test
	public void testConstructor(){
		
		// test parameterless constructor
		
		AbstractSlot as = new AbstractSlot();
		
		assertEquals("", as.getNamespace());
		assertNull(as.getType());
		assertNull(as.getLabel());
		assertNull(as.getScope());
		
		// test constructor with 2 parameters
		
		String label = "testslot";
		Class<?> type = String.class;
		
		as = new AbstractSlot(label, type);
		
		assertEquals("", as.getNamespace());
		assertEquals(label, as.getLabel());
		assertEquals(type, as.getType());
		assertEquals(label, as.getScope());
		
		// test constructor with 3 parameters
		
		String namespace = "testnamespace";
		
		as = new AbstractSlot(namespace, label, type);
		
		assertEquals(namespace, as.getNamespace());
		assertEquals(label, as.getLabel());
		assertEquals(type, as.getType());
		assertEquals(namespace+"/"+label, as.getScope());
		
	}
	
}
