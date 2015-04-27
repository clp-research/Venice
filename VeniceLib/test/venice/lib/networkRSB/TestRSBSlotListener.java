package venice.lib.networkRSB;

import java.util.LinkedList;

import venice.lib.AbstractSlotListener;

/**
 * An implementation of {@link AbstractSlotListener} for
 * RSB unittests.
 * It will listen to RSB network and store every received event in a history
 * list.
 * The entrys in the history list can be read via the methods getData,
 * getType and getLabel.  They need an index as parameter.  To get the number
 * of stored events, use getSize.
 *  
 * @author Oliver Eickmeyer
 *
 */
public class TestRSBSlotListener implements AbstractSlotListener {
	private LinkedList<HistoryEntry> history;
	
	private class HistoryEntry{
		private Object data;
		private Class<?> type;
		private String label;
		
		public HistoryEntry(Object data, Class<?> type, String label){
			this.data = data;
			this.type = type;
			this.label = label;
		}
		
		public Object getData(){
			return data; 
		}
		
		public Class<?> getType(){
			return type;
		}
		
		public String getLabel(){
			return label;
		}
	}
	
	public TestRSBSlotListener(){
		history = new LinkedList<HistoryEntry>();
	}
	
	@Override
	public void newData(Object data, String namespace, String label, Class<?> type) {
		history.add(new HistoryEntry(data, type, namespace+"/"+label));
	}
	
	public Object getData(int i){
		return history.get(i).getData();
	}
	
	public Class<?> getType(int i){
		return history.get(i).getType();
	}

	/**
	 * Returns the slotlabel of the i'th entry in the event history.
	 * @param i The index of the event.
	 * @return The label of the RSB slot where the event was received.
	 */
	public String getLabel(int i){
		return history.get(i).getLabel();
	}
	
	/**
	 * Returns the size of the event history.
	 */
	public int getSize(){
		return history.size();
	}

}
