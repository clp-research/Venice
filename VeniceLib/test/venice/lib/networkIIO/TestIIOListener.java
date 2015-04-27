package venice.lib.networkIIO;

import java.util.ArrayList;

import org.instantreality.InstantIO.InSlot;
import org.instantreality.InstantIO.Namespace;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;

import venice.lib.AbstractSlot;

/**
 * An IIO-Listener, used by unit tests.
 * It will listen for the creation of slots from a list of
 * expected slots.   The latter has to be provided by the unit test.
 * The unit test can then ask this listener, if all expected slots
 * have been slotDetected (after waiting a bit).
 * 
 * @author Oliver Eickmeyer
 *
 */
public class TestIIOListener implements Namespace.Listener, InSlot.Listener {
	private ArrayList<AbstractSlot> expectedSlots;
	private boolean[] slotDetected;
	
	public TestIIOListener(){
		Root.the().addListener(this);
	}
	
	/**
	 * Sets a list of slots expected to be created.
	 */
	public void setExpectedSlots(ArrayList<AbstractSlot> slots){
		expectedSlots = slots;
		slotDetected = new boolean[expectedSlots.size()];
		for(int i=0; i<slotDetected.length; i++){
			slotDetected[i] = false;
		}
	}
	
	/**
	 * Returns <code>true</code>, if all slots of the list of expected
	 * slots have been slotDetected.
	 */
	public boolean areAllSlotsDetected(){
		boolean r = true;
		for(int i=0; i<slotDetected.length; i++){
			if(!slotDetected[i]){
				r = false;
				break;
			}
		}
		return r;
	}

	@Override
	public void inSlotAdded(Namespace namespace, String label, InSlot inslot) {
	}

	@Override
	public void inSlotRemoved(Namespace namespace, String label, InSlot inslot) {
	}

	/**
	 * Gets called by the IntantIO network node, when it detects a new outslot.
	 */
	@Override
	public void outSlotAdded(Namespace namespace, String label, OutSlot outslot) {
		String fullLabel = IIONamespaceBuilder.concatNamespaceAndLabel(namespace.getLabel(), label);
		if(slotDetected != null){
			for(int i=0; i<expectedSlots.size(); i++){
				AbstractSlot es = expectedSlots.get(i);
				if( fullLabel.equals( es.getScope() ) && outslot.getType().equals(es.getType())){
					slotDetected[i] = true;
				}
			}
		}
	}

	@Override
	public void outSlotRemoved(Namespace namespace, String label, OutSlot outslot) {
	}

	@Override
	public void routeAdded(Namespace arg0, String arg1, String arg2) {
	}

	@Override
	public void routeRemoved(Namespace arg0, String arg1, String arg2) {
	}

	@Override
	public void newData(InSlot slot) {
		
	}

	@Override
	public void startInSlot(InSlot arg0) {
	}

	@Override
	public void stopInSlot(InSlot arg0) {
	}

}
