package venice.lib.networkIIO;

import java.util.ArrayList;

import org.instantreality.InstantIO.Color;
import org.instantreality.InstantIO.ColorRGBA;
import org.instantreality.InstantIO.Rotation;
import org.instantreality.InstantIO.Vec2f;
import org.instantreality.InstantIO.Vec3f;
import org.junit.Test;

import venice.lib.AbstractSlot;
import static org.junit.Assert.*;

public class IIONamespaceBuilderTest {
	
	/**
	 * Tests if OutSlots are created with correct
	 * namespaces, label and type.
	 */
	@Test
	public void testOutSlotCreation(){
		
		// define some slots for this test
		
		ArrayList<AbstractSlot> testSlots = new ArrayList<AbstractSlot>();
		String code = "testslot" + System.currentTimeMillis();
		testSlots.add(new AbstractSlot(code+"String", String.class));
		testSlots.add(new AbstractSlot(code+"Float", Float.class));
		testSlots.add(new AbstractSlot(code+"Boolean", Boolean.class));
		testSlots.add(new AbstractSlot(code+"Integer", Integer.class));
		testSlots.add(new AbstractSlot(code+"Double", Double.class));
		testSlots.add(new AbstractSlot(code+"Vec2f", Vec2f.class));
		testSlots.add(new AbstractSlot(code+"Vec3f", Vec3f.class));
		testSlots.add(new AbstractSlot(code+"Rotation", Rotation.class));
		
		// create a test listener
		
		TestIIOListener tIIOL = new TestIIOListener();
		tIIOL.setExpectedSlots(new ArrayList<AbstractSlot>(testSlots));
		
		// create the outslots
		
		IIONamespaceBuilder.setSlotFlags(new SlotFlags(false, true));
		assertFalse(IIONamespaceBuilder.getSlotFlags().importing);
		assertTrue(IIONamespaceBuilder.getSlotFlags().exporting);
		
		IIONamespaceBuilder.prepareNamespace("");
		IIONamespaceBuilder.initializeOutSlots(testSlots);
		
		// wait a bit
		
		try {
			Thread.sleep(100l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// check if all outslots were created
		
		assertTrue(tIIOL.areAllSlotsDetected());
	}
	
	/**
	 * Tests creation of namespaces.
	 */
	@Test
	public void testNamespaceCreation(){
		
		// define namespaces
		
		String code = "unittest" + System.currentTimeMillis();
		String namespaceLabel1 = code + "simple";
		String namespaceLabel2 = code + "/nested/test";
		
		// define test slots with same label but different namespaces
		
		String slotLabel = "testslot";
		ArrayList<AbstractSlot> testSlots = new ArrayList<AbstractSlot>();
		AbstractSlot slot1 = new AbstractSlot();
		slot1.setLabel(slotLabel);
		slot1.setType(String.class);
		slot1.setNamespace(namespaceLabel1);
		testSlots.add(slot1);
		AbstractSlot slot2 = new AbstractSlot();
		slot2.setLabel(slotLabel);
		slot2.setType(String.class);
		slot2.setNamespace(namespaceLabel2);
		testSlots.add(slot2);
		
		// create a test listener
		
		TestIIOListener tIIOL = new TestIIOListener();
		tIIOL.setExpectedSlots(new ArrayList<AbstractSlot>(testSlots));
		
		// create namespaces
		
		IIONamespaceBuilder.setSlotFlags(new SlotFlags(false, true));
		IIONamespaceBuilder.prepareNamespace(namespaceLabel1);
		IIONamespaceBuilder.prepareNamespace(namespaceLabel2);
		
		// create slots
		
		IIONamespaceBuilder.setSlotFlags(new SlotFlags(false, true));
		assertFalse(IIONamespaceBuilder.getSlotFlags().importing);
		assertTrue(IIONamespaceBuilder.getSlotFlags().exporting);
		IIONamespaceBuilder.initializeOutSlots(testSlots);
		
		// wait a bit
		
		try {
			Thread.sleep(100l);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// test, if slots where created
		
		assertTrue(tIIOL.areAllSlotsDetected());
		
	}

	/**
	 * Tests if correct initialization values of correct type are created
	 * for given out-slot type.
	 */
	@Test
	public void testCreateInitValue(){
		double delta = 0.001; // for comparison of floating point values
		
		// first test if we get null when using an unsupported class
		assertNull(IIONamespaceBuilder.createInitValue(IIONamespaceBuilderTest.class));
		
		
		// now test all supported classes
		
		assertTrue(IIONamespaceBuilder.createInitValue(String.class) instanceof String);
		assertEquals("init", IIONamespaceBuilder.createInitValue(String.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(String[].class) instanceof String[]);
		assertArrayEquals(new String[]{"init"}, (String[]) IIONamespaceBuilder.createInitValue(String[].class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Boolean.class) instanceof Boolean);
		assertEquals(false, IIONamespaceBuilder.createInitValue(Boolean.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Float.class) instanceof Float);
		assertEquals(.0f, ((Float) IIONamespaceBuilder.createInitValue(Float.class)).floatValue(), delta);
		
		assertTrue(IIONamespaceBuilder.createInitValue(Double.class) instanceof Double);
		assertEquals(.0d, ((Double) IIONamespaceBuilder.createInitValue(Double.class)).doubleValue(), delta);
		
		assertTrue(IIONamespaceBuilder.createInitValue(Integer.class) instanceof Integer);
		assertEquals(0, ((Integer) IIONamespaceBuilder.createInitValue(Integer.class)).intValue());
		
		assertTrue(IIONamespaceBuilder.createInitValue(Vec2f.class) instanceof Vec2f);
		assertEquals(new Vec2f(.0f, .0f), (Vec2f) IIONamespaceBuilder.createInitValue(Vec2f.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Vec2f[].class) instanceof Vec2f[]);
		assertArrayEquals(new Vec2f[]{new Vec2f(.0f, .0f)}, (Vec2f[]) IIONamespaceBuilder.createInitValue(Vec2f[].class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Vec3f.class) instanceof Vec3f);
		assertEquals(new Vec3f(.0f, .0f, .0f), (Vec3f) IIONamespaceBuilder.createInitValue(Vec3f.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Vec3f[].class) instanceof Vec3f[]);
		assertArrayEquals(new Vec3f[]{new Vec3f(.0f, .0f, .0f)}, (Vec3f[]) IIONamespaceBuilder.createInitValue(Vec3f[].class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Rotation.class) instanceof Rotation);
		assertEquals(new Rotation(.0f, .0f, .0f, .0f), (Rotation) IIONamespaceBuilder.createInitValue(Rotation.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Rotation[].class) instanceof Rotation[]);
		assertArrayEquals(new Rotation[]{new Rotation(.0f, .0f, .0f, .0f)}, (Rotation[]) IIONamespaceBuilder.createInitValue(Rotation[].class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(Color.class) instanceof Color);
		assertEquals(new Color(.0f, .0f, .0f), (Color) IIONamespaceBuilder.createInitValue(Color.class));
		
		assertTrue(IIONamespaceBuilder.createInitValue(ColorRGBA.class) instanceof ColorRGBA);
		assertEquals(new ColorRGBA(.0f, .0f, .0f, .0f), (ColorRGBA) IIONamespaceBuilder.createInitValue(ColorRGBA.class));
	}
}
