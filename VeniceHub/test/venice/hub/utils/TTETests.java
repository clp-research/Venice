package venice.hub.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import venice.hub.utils.TTE;
import venice.lib.parser.SlotEvent;

/**
 * Unit Tests for TTE
 *  
 * @author Oliver Eickmeyer
 */
public class TTETests {

	@Test
	public void testTTECreation(){
		SlotEvent se = new SlotEvent("Teststring", "TestNamespace", "Testslot", String.class, 123456789);
		TTE tte = new TTE(se);
		assertEquals("Teststring", tte.getValue());
		assertEquals("TestNamespace", tte.getNamespace());
		assertEquals("Testslot", tte.getLabel());
		assertEquals("TestNamespace/Testslot", tte.getScope());
		assertEquals(String.class, tte.getType());
		assertEquals(123456789L, tte.getTime());
	}
}
