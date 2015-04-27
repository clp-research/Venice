/*
 * Copyright (c) 2015 Dialog Systems Group, University of Bielefeld
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package venice.lib.networkIIO;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.instantreality.InstantIO.BufferedInSlot;
import org.instantreality.InstantIO.Color;
import org.instantreality.InstantIO.ColorRGBA;
import org.instantreality.InstantIO.InSlot;
import org.instantreality.InstantIO.Namespace;
import org.instantreality.InstantIO.NetworkNode;
import org.instantreality.InstantIO.OutSlot;
import org.instantreality.InstantIO.Root;
import org.instantreality.InstantIO.Rotation;
import org.instantreality.InstantIO.Vec2f;
import org.instantreality.InstantIO.Vec3f;

import venice.lib.AbstractSlot;
import venice.lib.AbstractSlotListener;
import venice.lib.Configuration;
import venice.lib.parser.XIOMaps;

/**
 * Creates all necessary objects for an InstantIO namespace, so namespaces and slots
 * can easily be used without accessing the instantreality.jar directly.
 * <p>
 * Usually the following methods are important:
 * <p>
 * <code>prepareNamespace</code> to activate the InstantIO communication.
 * For a simple logger application the namespace can be just <code>""</code>.
 * <p>
 * <code>setSlotFlags</code> to determine if the application wants to send or to
 * receive (sending: <code>new SlotFlags(true, false)</code>, receiving: 
 * <code>new SlotFlags(false, true)</code>).
 * <p>
 * <code>initializeInSlots</code> for receiving or
 * <code>initializeOutSlots</code> for sending.
 * <p>
 * <code>setMasterInSlotListener</code> for registering a receiving listener
 * (only for sending data).
 * <p>
 * To write data to outslots, use <code>write</code>.
 * <p>
 * To send to/receive from outside of the computer, increase the TTL with
 * <code>setMulticastTTL</code>.
 *  
 */
public class IIONamespaceBuilder{
	private static Logger logger;
	
	// class fields with default values
	private static String multicastAddress = "224.21.12.68";
	private static int multicastPort = 4711;
	private static int multicastTTL = 0;
	private static String unicastAddress = null;
	private static int unicastPort = 0;
	private static String unicastServers = null;
	private static String prefix = "{SlotLabel}";
	private static NetworkNode node = null;
	private static boolean outSlotsPredefined = false;
	private static String xiocodesFilename = null;
	private static boolean sendInitValue = false;
	private static SlotFlags slotFlags = new SlotFlags();
	
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(IIONamespaceBuilder.class);
	}
	
	private static HashMap<String, Namespace> namespaceMap = new HashMap<String, Namespace>();

	private static HashMap<String, InSlot > inSlotMap  = new HashMap<String, InSlot >();
	private static HashMap<String, OutSlot> outSlotMap = new HashMap<String, OutSlot>();
	
	private static AbstractSlotListener masterInSlotListener;
	
	/**
	 * Creates a namespace with the given label. Will also create a
	 * <code>NetworkNode</code>, if called the first time.
	 * @param label label of the demanded namespace, can be empty, but not
	 * <code>null</code>
	 * @return <code>Namespace</code> object of the created (or already
	 * existing) namespace
	 */
	private static Namespace createNamespace(String label){
 	    
		// first create a node if necessary
		if(node == null) initializeNetworkNode();
		
		if(!namespaceMap.containsKey(label)){
			// create the namespace
			Namespace nmspc = new Namespace();
			Root.the().addNamespace(label, nmspc);
			namespaceMap.put(label, nmspc);
			logger.debug("created namespace: "+nmspc.getLabel());
		}
		
		return namespaceMap.get(label);
	}
	
	/**
	 * An application has to use this method to tell venice.lib what
	 * namespaces are to be created. The method <code>setSlotFlags</code> can be
	 * used to demand a read-only or write-only configuration, see there
	 * for more details.<br>
	 * The NetworkNode will automatically be created on the first use.
	 * 
	 * @param label label of the demanded namespaces, can be empty, but not <code>null</code>
	 */
	public static void prepareNamespace(String label){
		createNamespace(label);
	}
	
	/**
	 * Creates a <code>NetworkNode</code> for InstantIO, to use it with
	 * namespaces and slots.
	 * It will be created when the first namespace is created.
	 * 
	 * @return Returns an active <code>NetworkNode</code>
	 */
	private static void initializeNetworkNode(){
		logger.debug("initializing network node");
		node = new NetworkNode();
		node.setPrefix(prefix);
		
		// set Flags for importing and exporting Slots
		activateSlotFlags();
		
		// setup multicast
		node.setMulticastTtl(multicastTTL);
		if(multicastAddress != null){
			try {
				node.setMulticastAddress(InetAddress.getByName(multicastAddress));
			} catch (UnknownHostException e) {
				logger.error("Can't find multicast address "+multicastAddress);
			}
		}
		if(multicastPort > 0) node.setMulticastPort(multicastPort);
		
		// setup unicast
		if(unicastAddress != null){
			try{
				node.setAddress(InetAddress.getByName(unicastAddress));
			} catch (UnknownHostException e) {
				logger.error("Can't find unicast address "+unicastAddress);
			}
		}
		if(unicastPort > 0) node.setPort(unicastPort);
		if(unicastServers != null) node.setServers(unicastServers);

		Root.the().addNamespace(node);
		
		if(xiocodesFilename != null){
			// load XIO codes from file
			logger.debug("loading XIO codes");
			XIOMaps.loadXIOCodes(xiocodesFilename);
		}
		else{
			// create default XIO codes for InstantIO
			logger.debug("using default XIO codes");
			XIOMaps.putPair("sfvec2f", org.instantreality.InstantIO.Vec2f.class);
			XIOMaps.putPair("mfvec2f", org.instantreality.InstantIO.Vec2f[].class);
			XIOMaps.putPair("sfvec3f", org.instantreality.InstantIO.Vec3f.class);
			XIOMaps.putPair("mfvec3f", org.instantreality.InstantIO.Vec3f[].class);
			XIOMaps.putPair("sfvec4f", org.instantreality.InstantIO.Vec4f.class);
			XIOMaps.putPair("mfvec4f", org.instantreality.InstantIO.Vec4f[].class);
			XIOMaps.putPair("sfvec2d", org.instantreality.InstantIO.Vec2d.class);
			XIOMaps.putPair("mfvec2d", org.instantreality.InstantIO.Vec2d[].class);
			XIOMaps.putPair("sfvec3d", org.instantreality.InstantIO.Vec3d.class);
			XIOMaps.putPair("mfvec3d", org.instantreality.InstantIO.Vec3d[].class);
			XIOMaps.putPair("sfvec4d", org.instantreality.InstantIO.Vec4d.class);
			XIOMaps.putPair("mfvec4d", org.instantreality.InstantIO.Vec4d[].class);
			XIOMaps.putPair("sfrotation", org.instantreality.InstantIO.Rotation.class);
			XIOMaps.putPair("mfrotation", org.instantreality.InstantIO.Rotation[].class);
			XIOMaps.putPair("sfcolor", org.instantreality.InstantIO.Color.class);
			XIOMaps.putPair("mfcolor", org.instantreality.InstantIO.Color[].class);
			XIOMaps.putPair("sfcolor4", org.instantreality.InstantIO.ColorRGBA.class);
			XIOMaps.putPair("mfcolor4", org.instantreality.InstantIO.ColorRGBA[].class);
			XIOMaps.putPair("sfmatrix3d", org.instantreality.InstantIO.Matrix3d.class);
			XIOMaps.putPair("mfmatrix3d", org.instantreality.InstantIO.Matrix3d[].class);
			XIOMaps.putPair("sfmatrix3f", org.instantreality.InstantIO.Matrix3f.class);
			XIOMaps.putPair("mfmatrix3f", org.instantreality.InstantIO.Matrix3f[].class);
			XIOMaps.putPair("sfmatrix4d", org.instantreality.InstantIO.Matrix4d.class);
			XIOMaps.putPair("mfmatrix4d", org.instantreality.InstantIO.Matrix4d[].class);
			XIOMaps.putPair("sfmatrix4f", org.instantreality.InstantIO.Matrix4f.class);
			XIOMaps.putPair("mfmatrix4f", org.instantreality.InstantIO.Matrix4f[].class);
		}
	}
	
	/**
	 * Activates the flags for importing and exporting slots for the
	 * network node.  If the network node is not active yet, this
	 * method is silently doing nothing.
	 * <p>
	 * If a flag is set to <code>null</code>, <code>false</code> will be
	 * used.
	 */
	private static void activateSlotFlags(){
		if(node != null){
			if(slotFlags.isImporting() == null) node.setImportSlots(false);
			else node.setImportSlots(slotFlags.isImporting());
			if(slotFlags.isExporting() == null) node.setExportSlots(false);
			else node.setExportSlots(slotFlags.isExporting());
			logger.debug("setting export flag "+node.getExportSlots()+", import flag "+node.getImportSlots());
		}
	}
	
	/**
	 * Sets the flags for importing and exporting slots for the
	 * network node.
	 * 
	 * @param newSlotFlags new settings for the slot flags
	 */
	public static void setSlotFlags(SlotFlags newSlotFlags){
		slotFlags = newSlotFlags;
		activateSlotFlags();
	}
	
	/**
	 * Returns the actual setting of the slot flags.
	 */
	public static SlotFlags getSlotFlags(){
		return slotFlags;
	}
	
	/**
	 * Activates the mode of dynamic out-slot creation.
	 */
	public static void initializeOutSlots(){
		outSlotsPredefined = false;
		return;
	}
	
	/**
	 * Creates Outslots for writing data into instantIO network using predefined slots,
	 * and activates the 'predefined'-mode (instead of dynamic slot creation).
	 * 
	 * If predefined slots are given (not <code>null</code> and not empty)
	 * only those slots will be created.
	 * Data send to non-predefined slots will be ignored.
	 * If no slots are predefined, outslot will be created dynamically.
	 * It will also create namespaces.
	 * 
	 * @param predefinedSlots An array with predefined slots;
	 * if empty or <code>null</code> slots will be created dynamically.
	 * 
	 * @deprecated There are some good reasons for not using primitive arrays
	 * in interfacing methods, so this is replaced by an ArrayList-version.
	 */
	@Deprecated
	public static void initializeOutSlots(AbstractSlot[] predefinedSlots){
		
		// check for predefined slots
		if(predefinedSlots != null && predefinedSlots.length > 0){
			outSlotsPredefined = true;
		}
		else{
			outSlotsPredefined = false;
			return;
		}
		
		// if there are predefined slots, create them:
		Namespace namespace;
		String namespaceLbl;
		OutSlot outSlot;
		for(AbstractSlot abstrSlot: predefinedSlots){
			outSlot = new OutSlot(abstrSlot.getType());
			
			namespaceLbl = abstrSlot.getNamespace();
			namespace = findNamespace(namespaceLbl, true);
            namespace.addOutSlot(abstrSlot.getLabel(), outSlot);
            //namespace.addExternalRoute(abstrSlot.getLabel(), "{NamespaceLabel}/{SlotLabel}");
            
			outSlotMap.put(abstrSlot.getLabel(), outSlot);
			try { Thread.sleep(10); } // to not lose slots, because IIO needs some time to create slots
			catch (InterruptedException e) {}
			
			if(sendInitValue){
				Object initValue = createInitValue(outSlot.getType());
				if(initValue != null) outSlot.push(initValue);
				else logger.error("have no initialization value for "+outSlot.getType().getName());
			}
		}
	}
	
	/**
	 * Creates Outslots for writing data into instantIO network using predefined slots,
	 * and activates the 'predefined'-mode (instead of dynamic slot creation).
	 * <p>
	 * If predefined slots are given (not <code>null</code> and not empty)
	 * only those slots will be created.
	 * Data send to non-predefined slots will be ignored.
	 * If no slots are predefined, outslot will be created dynamically.
	 * It will also create namespaces.
	 * 
	 * @param predefinedSlots An ArrayList with predefined slots;
	 * if empty or <code>null</code> slots will be created dynamically.
	 */
	public static void initializeOutSlots(ArrayList<AbstractSlot> predefinedSlots){
		// check for predefined slots
		if(predefinedSlots != null && predefinedSlots.size() > 0){
			outSlotsPredefined = true;
		}
		else{
			outSlotsPredefined = false;
			return;
		}
		
		// if there are predefined slots, create them:
		Namespace namespace;
		String namespaceLbl;
		OutSlot outSlot;
		for(AbstractSlot abstrSlot: predefinedSlots){
			outSlot = new OutSlot(abstrSlot.getType());
			
			namespaceLbl = abstrSlot.getNamespace();
			namespace = findNamespace(namespaceLbl, true);
			logger.debug("adding slot "+abstrSlot.getLabel()+" to namespace "+namespaceLbl);
            namespace.addOutSlot(abstrSlot.getLabel(), outSlot);
            namespace.addExternalRoute(abstrSlot.getLabel(), "{NamespaceLabel}/{SlotLabel}");
            
			outSlotMap.put(abstrSlot.getScope(), outSlot);
			try { Thread.sleep(10); } // to not lose slots, because IIO needs some time to create slots
			catch (InterruptedException e) {}
			
			if(sendInitValue){
				Object initValue = createInitValue(outSlot.getType());
				if(initValue != null) outSlot.push(initValue);
				else logger.error("have no initialization value for "+outSlot.getType().getName());
			}
		}
	}
	
	/**
	 * Creates an initialization value for the given type (of an out-slot).
	 * @param type the type of the out-slot
	 * @return an initialization value of appropriate type
	 */
	public static Object createInitValue(Class<?> type){
		if(type.equals(String.class)) return "init";
		else if(type.equals(String[].class)) return new String[]{"init"};
		else if(type.equals(Boolean.class)) return new Boolean(false);
		else if(type.equals(Float.class)) return new Float(.0f);
		else if(type.equals(Double.class)) return new Double(.0d);
		else if(type.equals(Integer.class)) return new Integer(0);
		else if(type.equals(Vec2f.class)) return new Vec2f(.0f, .0f);
		else if(type.equals(Vec2f[].class)) return new Vec2f[]{new Vec2f(.0f, .0f)};
		else if(type.equals(Vec3f.class)) return new Vec3f(.0f, .0f, .0f);
		else if(type.equals(Vec3f[].class)) return new Vec3f[]{new Vec3f(.0f, .0f, .0f)};
		else if(type.equals(Color.class)) return new Color(.0f, .0f, .0f);
		else if(type.equals(ColorRGBA.class)) return new ColorRGBA(.0f, .0f, .0f, .0f);
		else if(type.equals(Rotation.class)) return new Rotation(.0f, .0f, .0f, .0f);
		else if(type.equals(Rotation[].class)) return new Rotation[]{new Rotation(.0f, .0f, .0f, .0f)};
		else return null;
	}
	
	/**
	 * Initializes in-slots for dynamic mode.
	 */
	public static void initializeInSlots(){
		initializeInSlots(new ArrayList<AbstractSlot>());
	}
	
	/**
	 * Initializes in-slots. If predefined slots are given, then only
	 * those slots are created. Otherwise dynamic slot creation is used.
	 * 
	 * @param predefinedSlots array with abstract slot definitions or
	 * <code>null</code>
	 * @deprecated There are some good reasons for not using primitive arrays
	 * in interfacing methods, so this is replaced by an ArrayList-version.
	 */
	@Deprecated
	public static void initializeInSlots(AbstractSlot[] predefinedSlots){
		if(predefinedSlots != null && predefinedSlots.length > 0){
			logger.debug("creating predefined in-slots:");
			for(AbstractSlot abstrSlot: predefinedSlots){
				logger.debug("    creating "+abstrSlot.getLabel());
				InSlot inSlot = new BufferedInSlot(abstrSlot.getType(), abstrSlot.getLabel(), 80);
		        inSlot.addListener(new IIOInSlotListener(abstrSlot.getNamespace(), abstrSlot.getLabel()));

		        Namespace namespace = findNamespace(abstrSlot.getNamespace(), true);
	            namespace.addInSlot(abstrSlot.getLabel(), inSlot);

				inSlotMap.put(abstrSlot.getLabel(), inSlot);
				try { Thread.sleep(10); } // to not lose slots, because IIO needs some time to create slots
				catch (InterruptedException e) {}
			}
		}
		else{
			logger.debug("using dynamic in-slot creation");
			new IIONamespaceListener();
		}
	}
	
	/**
	 * Initializes in-slots. If predefined slots are given, then only
	 * those slots are created. Otherwise dynamic slot creation is used.
	 * 
	 * @param predefinedSlots array with abstract slot definitions or
	 * <code>null</code>
	 */
	public static void initializeInSlots(ArrayList<AbstractSlot> predefinedSlots){
		if(predefinedSlots != null && predefinedSlots.size() > 0){
			logger.debug("creating predefined in-slots:");
			for(AbstractSlot abstrSlot: predefinedSlots){
				InSlot inSlot = new BufferedInSlot(abstrSlot.getType(), abstrSlot.getLabel(), 80);
		        inSlot.addListener(new IIOInSlotListener(abstrSlot.getNamespace(), abstrSlot.getLabel()));
		        
		        Namespace namespace = findNamespace(abstrSlot.getNamespace(), true);
	            namespace.addInSlot(abstrSlot.getLabel(), inSlot);
	            namespace.addExternalRoute(abstrSlot.getLabel(), "{NamespaceLabel}/{SlotLabel}");
	            logger.debug("added inSlot '"+abstrSlot.getLabel()+"' to namespace '"+namespace.getLabel()+"'");

				inSlotMap.put(abstrSlot.getScope(), inSlot);
				try { Thread.sleep(10); } // to not lose slots, because IIO needs some time to create slots
				catch (InterruptedException e) {}
			}
		}
		else{
			logger.debug("using dynamic in-slot creation");
			new IIONamespaceListener();
		}
	}
	
	/**
	 * Searches a namespace with given label in the map of namespaces.
	 * If label is empty or <code>null</code>, <code>Root.the()</code> will be returned.
	 * 
	 * @param label the label of the namespace
	 * @param createIfNotFound If <code>true</code>, a missing namespace will be created and returned.
	 * If <code>false</code>, a missing namespace will return <code>null</code>.
	 * @return <code>Namespace</code> object or <code>null</code>
	 */
	public static Namespace findNamespace(String label, boolean createIfNotFound){
		Namespace namespace = null;
		if(label == null || label.isEmpty()){
			// empty namespace means Root
			namespace = Root.the();
		}
		else{
			if(namespaceMap.containsKey(label)){
				// if label of namespace is given, search it in the map
				namespace = namespaceMap.get(label);
			}
		}
		if(createIfNotFound && namespace == null){
			namespace = createNamespace(label);
		}
		return namespace;
	}
	
	/**
	 * Writes data to the <code>OutSlot</code> with the given label.
	 * If there is no <code>OutSlot</code> with this label, the data will be ignored.
	 * 
	 * @param outSlotLabel Label of the <code>OutSlot</code> where the data has to be written
	 * @param value The data to be written
	 * @return <code>true</code> if the data could be written to the outslot, otherwise <code>false</code>
	 */
	public static boolean write(String outSlotLabel, Object value, String nmspcLbl){
		String fullLabel = concatNamespaceAndLabel(nmspcLbl, outSlotLabel);
		
		// first, try to find a slot with the given label in the map
		OutSlot outSlot = outSlotMap.get(fullLabel);
		if(outSlot == null){
			// could not find the slot in the map
			if(outSlotsPredefined)
				return false; // if predefined mode is active -> ignore data
			else{
				// if dynamic mode is active, create a new slot
				if(value == null) return false; // no value -> ignore data and do nothing
				
				// define new outslot with type, by using type of value
	            outSlot = new OutSlot(value.getClass());

	            Namespace namespace = findNamespace(nmspcLbl, true);
	            
	            // add to namespace
	            namespace.addOutSlot(outSlotLabel, outSlot);
	            namespace.addExternalRoute(outSlotLabel, "{NamespaceLabel}/{SlotLabel}");
	            
	            // add new outslot to outslot map
	            outSlotMap.put(fullLabel, outSlot);
			}
		}
		outSlot.push(value);
		return true;
	}
	
	/**
	 * Concatenates labels of a namespace and a slot, using the format
	 * <code>namespace/label</code>.
	 * If the namespace is <code>null</code> or empty, then only the label
	 * will be returned (without the slash).
	 *   
	 * @param namespace The label of a namespace, or <code>null</code>
	 * @param label The label of a slot
	 * @return Concatenation of the namespace and the label
	 */
	public static String concatNamespaceAndLabel(String namespace, String label){
		if(namespace != null && ! namespace.isEmpty())
			return namespace + "/" + label;
		else
			return label;
	}

	/**
	 * Returns the multicast address that is used by InstantIO
	 * network operations. It is returned as a <code>String</code>
	 * representation (i.e. <code>"224.21.12.68"</code>).
	 * @return multicast address for InstantIO
	 */
	public static String getMulticastAddress() {
		return multicastAddress;
	}

	/**
	 * Sets the multicast address for the use with InstantIO
	 * network operations. It has to be given as a String, like
	 * <code>"224.21.12.68"</code>.
	 *
	 * @param multicastAddress <code>String</code> representation
	 * of a multicast address
	 */
	public static void setMulticastAddress(String multicastAddress) {
		IIONamespaceBuilder.multicastAddress = multicastAddress;
	}

	/**
	 * Returns the actual used multicast port that is used by InstantIO
	 * network operations.
	 * @return port for multicast
	 */
	public static int getMulticastPort() {
		return multicastPort;
	}

	/**
	 * Sets the multicast port for InstantIO network operations.
	 * 
	 * @param multicastPort port for multicast
	 */
	public static void setMulticastPort(int multicastPort) {
		IIONamespaceBuilder.multicastPort = multicastPort;
	}
	
	/**
	 * Sets the time-to-live (TTL) for multicast.
	 * The TTL is decreased by every router, that the data passes.
	 * Data with TTL 0 will not be send any further.
	 * For sending to/receiving from <code>localhost</code> only, set TTL to 0.
	 * To increase the range, increase the TTL. The exact behavior
	 * depends on the network and internet protocols.
	 * @param ttl time-to-live for multicast
	 */
	public static void setMulticastTTL(int ttl){
		multicastTTL = ttl;
	}
	
	/**
	 * Gets the time-to-live (TTL) for multicast.
	 * @return time-to-live for multicast
	 */
	public static int getMulticastTTL(){
		return multicastTTL;
	}
	
	/**
	 * Set a port for unicast. If set to zero, no port is used.
	 * @param newUnicastPort port number or <code>0</code> to deactivate
	 */
	public static void setUnicastPort(int newUnicastPort){
		unicastPort = newUnicastPort;
	}
	
	/**
	 * Set an address for unicast. If set to null, no address is used.
	 * @param newUnicastAddress IP address or <code>null</code>
	 */
	public static void setUnicastAddress(String newUnicastAddress){
		unicastAddress = newUnicastAddress;
	}
	
	/**
	 * Give a list of unicast servers or <code>null</code>.
	 * @param newUnicastServers String with a list of unicast servers, or
	 * <code>null</code>
	 */
	public static void setUnicastServers(String newUnicastServers){
		unicastServers = newUnicastServers;
	}
	
	/**
	 * Sets a prefix that gets added to every namespace.
	 * The placeholder for slot-label is <code>{SlotLabel}</code>, which is
	 * also the default.
	 * @param nodePrefix <code>String</code> to be used as prefix for all
	 * namespaces
	 */
	public static void setPrefix(String nodePrefix){
		prefix = nodePrefix+"/{SlotLabel}";
	}
	
	/**
	 * A map of all created inslots. Normally this should not be used by
	 * an application, because it violates the abstraction layer. But if
	 * an application needs direct access to
	 * <code>org.instantreality.InstantIO.InSlot</code>
	 * objects, this is the method to use.
	 * @return map of all created <code>InSlot</code>s
	 */
	public static HashMap<String, InSlot> getInSlotMap(){
		return inSlotMap;
	}
	
	/**
	 * Sets the listener that gets notified when new data comes in.
	 * This is the usual connection between venice.lib and an
	 * application that wants to receive data from network.
	 * @see AbstractSlotListener
	 * @param inSlotListener an object that implements the
	 * <code>AbstractSlotListener</code> interface
	 */
	public static void setMasterInSlotListener(AbstractSlotListener inSlotListener){
		masterInSlotListener = inSlotListener;
	}
	
	/**
	 * Returns the listener that is registered to receive new data.
	 * @see AbstractSlotListener
	 * @return <code>AbstractSlotListener</code> that is registered to receive
	 * new data
	 */
	public static AbstractSlotListener getMasterInSlotListener(){
		return masterInSlotListener;
	}
	
	/**
	 * Set the name of a file with mapping information about xio codes
	 * and types.
	 * <p>
	 * One of the default xio codes is <code>sfstring</code> and it is
	 * by default mapped to <code>String</code>. If new xio codes are
	 * needed, or if they have to be mapped to other types, a xio code
	 * file ist needed.
	 * <p>
	 * Example file 1:<br>
	 * <code>
	 * &lt;codes&gt;<br>
	 * &lt;def class="java.lang.String" code="sfstring"/&gt;<br>
	 * &lt;/codes&gt;
	 * </code>
	 * <p>
	 * Example file 2:<br>
	 * <code>
	 * &lt;codes&gt;<br>
	 * &lt;def class="java.lang.String" code="sfstring"/&gt;<br>
	 * &lt;def class="[Ljava.lang.String;" code="mfstring"/&gt;<br>
	 * &lt;def class="org.instantreality.InstantIO.Vec2f" code="sfvec2f"/&gt;<br>
	 * &lt;/codes&gt;
	 * </code>
	 * <p>
	 * Type names are case sensitive. XIO codes are not case sensitive.
	 * <p>
	 * Use this only, if the default values are not sufficient. See
	 * initialization methods for default values.
	 * 
	 * @param filename Name of a XML file with XIO code definitions
	 */
	public static void setXioCodesFilename(String filename){
		xiocodesFilename = filename;
	}
	
	/**
	 * Returns the name of the file used for the mapping of XIO codes and
	 * types. See the <code>setXioCodesFilename</code> for more details.
	 * 
	 * @return Name of the file with XIO code definitions
	 */
	public static String getXioCodesFilename(){
		return xiocodesFilename;
	}
	
	/**
	 * Defines if an initialization value should be send for a new created
	 * out-slot.<br>
	 * If set to <code>true</code>, every time a new out-slot is created,
	 * venice.lib will send a value. This helps to have all slots be ready,
	 * when the real data comes in.
	 * @param sendInitializationValue <code>true</code> if initialization values
	 * have to be used, <code>false</code> otherwise
	 */
	public static void setSendInitValue(boolean sendInitializationValue){
		sendInitValue = sendInitializationValue;
		logger.info("use of initialization values: "+sendInitValue);
	}
}
