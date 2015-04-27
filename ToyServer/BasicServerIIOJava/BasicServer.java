import java.io.IOException;

import venice.lib.AbstractSlotListener;
import venice.lib.networkIIO.IIONamespaceBuilder;
import venice.lib.networkIIO.SlotFlags;

/**
 * Demonstrates how to set up a basic server with
 * VeniceLib.
 * It creates a network node for InstantIO with an out-slot and
 * will send keyboard input to that out-slot.
 * It also shows received data that comes over InstantIO.
 *  
 * @author Oliver Eickmeyer
 */
public class BasicServer implements AbstractSlotListener{
	
	// the label of the out-slot used to send our data
	private String outslotlabel;
	
	/**
	 * Sets the prefix for all exported namespaces created by this instance.
	 */
	public static final String serverlabel = "BasicServer";
	
	/**
	 * The label of the namespace that will be used for the
	 * creation of slots.
	 */
	public static final String namespace = "example";
	
	/**
	 * Creates a new BasicServer.
	 * As an optional command line argument a name can be
	 * given.  The name will be used for the creation of the out-slot.
	 * Otherwise a default name is used.
	 * @param args command line arguments
	 */
	public static void main(String[] args){
		String label = "unknown"; // default label for the server
		
		// if an other label was given via command line argument, use it
		if(args.length > 0){
			label = args[0];
		}
		
		// create the server
		new BasicServer(label);
	}
	
	/**
	 * Constructs a new BasicServer.
	 * 
	 * @param label The label to use for the out-slot
	 */
	public BasicServer(String label){
		this.outslotlabel = label;
		initialize();
		run();
	}
	
	/**
	 * Initializes the server.
	 * Sets up the network connection.
	 */
	protected void initialize(){
		
		/* Sets the Time-To-Live (TTL) for multicast messages.
		 * a value of zero means only local computer
		 */
		venice.lib.networkIIO.IIONamespaceBuilder.setMulticastTTL(1);
		
		// set the prefix for the labels of exported namespaces
		venice.lib.networkIIO.IIONamespaceBuilder.setPrefix(serverlabel);
		
		/*
		 *  Set the slots flags both to true.
		 *  So the InstantIO networknode will export his slots to other
		 *  networknodes and also import slots from other networknodes.
		 */
		SlotFlags slotFlags = new SlotFlags();
		slotFlags.setExporting(true);
		slotFlags.setImporting(true);
		IIONamespaceBuilder.setSlotFlags(slotFlags);
		
		// prepare the namespace to be used
		IIONamespaceBuilder.prepareNamespace(namespace);
		
		/* Initialize out-slots:
		 * In this case it means only to tell VeniceLib that it
		 * should create slots dynamically, without preparing
		 * predefined slots.
		 * The slots that are used by the BasicServer will be
		 * created in that moment, when the first data is send to it.
		 */
		IIONamespaceBuilder.initializeOutSlots();
		IIONamespaceBuilder.initializeInSlots();
		
		/* Set this BasicServer as the receiver for incoming data.
		 * From now, VeniceLib will call the newData method when
		 * new data comes in.
		 */
		IIONamespaceBuilder.setMasterInSlotListener(this);
	}
	
	/**
	 * Receives keyboard inputs and sends them to network,
	 * until stopped.
	 */
	protected void run(){
		boolean active = true;
		byte[] inputbuffer = new byte[255];
    System.out.println("enter 'q' to quit");
		while(active){
			int charsread = 0; // number of chars read from input
			String input = ""; // input of standard input
			try {
				// read from standard input
				charsread = System.in.read(inputbuffer, 0, 255);
				input = (new String(inputbuffer, 0, charsread)).trim();
			} catch (IOException e) {
				System.err.println("Error while reading from standard input.");
			}
			
			// parse input
			if(input.equals("q")){
				active = false;
			}
			else{
				IIONamespaceBuilder.write(outslotlabel, input, namespace);
			}
		}
		System.out.println("Program stopped");
	}

	/**
	 * Is called by VeniceLib when it receives new data over network.
	 * 
	 * @param data the new data received via network
	 * @param namespace the namespace of the slot where the data was received
	 * @param serverlabel the serverlabel of the slot where the data was received
	 * @param type the type of the data
	 */
	@Override
	public void newData(Object data, String namespace, String label, Class<?> type) {
		if(namespace == null || label == null || data == null){
			// received useless message: ignore it
			return;
		}
		if(namespace.equals(BasicServer.namespace) && label.equals(this.outslotlabel)){
			// received our own message: ignore it
			return;
		}
		
		// print the message:
		System.out.println("Received message from namespace '"+namespace+"'");
		System.out.println(label+": "+data);
	}
	
}
