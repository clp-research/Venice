/*
 * Copyright (c) 2015 Dialogue Systems Group, University of Bielefeld
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
package venice.lib.networkRSB;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Message;

import rsb.Factory;
import rsb.InitializeException;
import rsb.Listener;
import rsb.RSBException;
import rsb.Informer;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import venice.lib.AbstractSlot;
import venice.lib.AbstractSlotListener;
import venice.lib.Configuration;
import venice.lib.parser.XIOMaps;

/**
 * Takes care of namespace- and slot-creating for the RSB network.
 * <p>
 * RSB-specific terminology:<br>
 * In-slot is {@link rsb.Listener}.<br>
 * Out-slot is {@link rsb.Informer}.<br>
 * In this documentation the generic terminology is used, to be symmetric to
 * the other NamespaceBuilders.
 * <p>
 * Scope consists of Namespace and Label. Namespace is the part before the last
 * slash of the Scope and Label is the part after the last slash of the Scope.
 * <p>
 * For method parameters Scopes are handled without leading and trailing
 * slashes (they will be added automatically when the RSBNamespaceBuilder
 * interacts with the RSB network). 
 */
public class RSBNamespaceBuilder{
	private static Logger logger;
	private static HashMap<String, Listener> inSlotMap  = new HashMap<String, Listener>();
	@SuppressWarnings("rawtypes")
	private static HashMap<String, Informer> outSlotMap  = new HashMap<String, Informer>();
	private static boolean outSlotsPredefined;
	private static AbstractSlotListener masterInSlotListener;
	private static String prefix = "/";
	private static boolean protobufInitialized = false;
	private static String protobufDir = null;
	private static String matchFilename = "match.xml";
	private static String xiocodesFilename = null;
	private static HashMap<String, Class<?>> primitiveTypeMap= new HashMap<>();
	private static ArrayList<ClassMatcher> classMatcherList = new ArrayList<>();
	private static HashMap<Class<?>, ClassMatcher> matchOtherToProtobufMap = new HashMap<>();
	private static HashMap<Class<?>, ClassMatcher> matchProtobufToOtherMap = new HashMap<>();
	
	static {
		// setup logger
		Configuration.setupLogger();
		logger = Logger.getLogger(RSBNamespaceBuilder.class);
		// create the map for primitive types
		primitiveTypeMap.put("int", int.class);
		primitiveTypeMap.put("float", float.class);
		primitiveTypeMap.put("boolean", boolean.class);
		primitiveTypeMap.put("string", String.class);
	}
	
	/**
	 * Private constructor, so no instances of this class can be made.
	 */
	private RSBNamespaceBuilder(){
		// nothing
	}
	
	/**
	 * Creates in-slots and takes care of setting them up. If an array with
	 * {@link AbstractSlot}s is given, only those in-slots are created. If the
	 * array is <code>null</code> (or if the overloaded version without
	 * parameters is used), a in-slot is created that listens to the top most
	 * namespace (so, only prefix).<br>
	 * To every in-slot a handler will be added, that informs the main
	 * application of new data (if the main application has registered a
	 * master-lister). 
	 * 
	 * @param predefinedSlots an ArrayList with predefined in-slots, or
	 * <code>null</code> if not needed
	 */
	public static void initializeInSlots(ArrayList<AbstractSlot> predefinedSlots){
		if(!protobufInitialized) {
			logger.warn("Warning: Protobuf is not initialized.");
		}
		
		Factory factory = Factory.getInstance();
		if(predefinedSlots == null || predefinedSlots.size() == 0){
			// if there is no array with predefined slots, use a listener
			// that listens to the top level scope (so, only prefix)
			try {
				logger.info("creating listener for "+prefix);
				Listener listener = factory.createListener(prefix);
				listener.activate();
				listener.addHandler(new RSBHandler(), true);
				inSlotMap.put(prefix, listener);
			} catch (RSBException | InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		else{
			// if an array with predefined slots is given, create corresponding listener
			for(AbstractSlot abstrSlot: predefinedSlots){
				try {
					String fullLabel = "/" + abstrSlot.getScope();
					logger.debug("creating RSB-Listener for "+fullLabel);
					Listener listener = factory.createListener(fullLabel);
					listener.activate();
					listener.addHandler(new RSBHandler(), true);
					inSlotMap.put(fullLabel, listener);
				} catch (RSBException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	/**
	 * Initializes inslots in dynamic mode. See the parameterized version
	 * for more information. Essentially this method calls the
	 * parameterized version with <code>null</code> for the parameter.
	 */
	public static void initializeInSlots(){
		initializeInSlots(null);
	}
	
	/**
	 * Creates out-slots (='informer') and takes care of setting them up. If
	 * an array with {@link AbstractSlot}s is given, only those out-slots are
	 * created. If the array is <code>null</code> (or if the overloaded
	 * version without parameters is used), out-slots are created dynamically
	 * on demand.
	 * <p>
	 * If using predefined out-slots, all data that is written to an unknown
	 * out-slot will be ignored.<p>
	 * If class matching pairs are given, out-slots will be created for the
	 * matching class (otherwise for the given type of the abstract slot).
	 * 
	 * @param predefinedSlots an ArrayList with predefined in-slots, or
	 * <code>null</code> if not needed
	 */
	public static void initializeOutSlots(ArrayList<AbstractSlot> predefinedSlots){
		if(!protobufInitialized) {
			logger.warn("Warning: Protobuf is not initialized.");
			return;
		}
		Factory factory = Factory.getInstance();
		if(predefinedSlots == null || predefinedSlots.size() == 0){
			// if there is no array with predefined out-slots, use dynamic Informer creation
			outSlotsPredefined = false;
		}
		else{
			// if an array with predefined out-slots is given, create corresponding RSB-Informers
			outSlotsPredefined = true;
			for(AbstractSlot abstrSlot: predefinedSlots){
				try {
					String rsbLabel = prefix + abstrSlot.getScope();
					Class<?> type = abstrSlot.getType();
					Class<?> informerType;
					logger.debug("creating Informer for "+rsbLabel+" ("+type.getName()+")");
					
					// first check, if this type is matched to a protobuf type
					ClassMatcher cm = matchOtherToProtobufMap.get(type);
					
					if(cm != null)
						informerType = cm.target; // matched class
					else
						informerType = type; // original class

					Informer<?> informer = factory.createInformer(rsbLabel, informerType);
					informer.activate();
					outSlotMap.put(abstrSlot.getScope(), informer);
					logger.debug("  slot successfully created ("+informerType.getName()+")");
				} catch (RSBException e) {
					e.printStackTrace();
				}
			}			
		}
	}
	
	/**
	 * Initializes dynamic out-slot creation. New out-slots will be created
	 * on root namespace (plus prefix, if given).
	 * It's the same like <code>initializeOutSlots(null)</code>.
	 * See overloaded methods for more details.
	 */
	public static void initializeOutSlots(){
		initializeOutSlots(null);
	}
	
	/**
	 * Parses class match entrys from XML file.
	 * 
	 * @return NamedPairList with class matches found in XML file
	 */
	private static NamedPairList parseMatches(){
		
		if(matchFilename == null) return null;
		
		File matchFile = new File(matchFilename);
		
		if(!matchFile.exists()){
			logger.error("Error: MatchFile '"+matchFilename+"' doesn't exist.");
			return null;
		}
		if(!matchFile.isFile()){
			logger.error("Error: MatchFile '"+matchFilename+"' is not a file.");
			return null;
		}
		
		DocumentBuilder builder = null;
    	try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		if(builder == null) return null; // DocumentBuilder doesn't want to work
		
		Document document=null;
		try {
			document = builder.parse(matchFile);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
		}
		if(document==null) return null; // XML Document can't be parsed
		
		Element primeElement = document.getDocumentElement();
		
		NodeList matchList = primeElement.getElementsByTagName("match");
		NamedPairList namedPairList = new NamedPairList();
		for(int iMatch=0; iMatch<matchList.getLength(); iMatch++){
			NamedPair namedPair = new NamedPair();
			
			Node match = matchList.item(iMatch);
			
			namedPair.setSourceName(findAttr("from", match));
			namedPair.setTargetName(findAttr("to", match));
			
			if(findAttr("repeated", match) != null && findAttr("repeated", match).equals("true") )
				namedPair.setRepeated(true);
			
			for(Node pair: findChildElements(match, "methodpair")){
				namedPair.addMethodPair(
						findAttr("getter", pair), 
						findAttr("setter", pair), 
						findAttr("type", pair)
						);
			}
			for(Node constr: findChildElements(match, "constructor")){
				// first check if there is a single parameter as an attribute
				String singleTypeName = findAttr("parameter", constr);
				if(singleTypeName != null){
					// if there is a single parameter as an attribute
					namedPair.setConstructorWithSingleParam(singleTypeName);
				}
				else{
					// if there are parameter nodes (instead of a single parameter-attribute)
					for(Node param: findChildElements(constr, "parameter")){
						String typeName = findAttr("type", param);
						String indexName = findAttr("index", param);
						int index = Integer.parseInt(indexName);
						namedPair.addConstructorParam(index, typeName);
					}
				}
			}
			for(Node gn: findChildElements(match, "getter")){
				String getterName = findAttr("name", gn);
				String indexName = findAttr("index", gn);
				int index = 0;
				if(indexName != null) index = Integer.parseInt(indexName);
				namedPair.setGetterName(index, getterName);
			}
			namedPairList.add(namedPair);
		}
		
		logger.info("matching class pairs for RSB:");
		
		for(int i=0; i<namedPairList.size(); i++){
			NamedPair np = namedPairList.get(i);
			logger.info("  - "+np);
			if(np.isUsingConstructor()){
				String s = "Constructor "+np.getTargetName()+"(";
				for(int j=0; j<np.numOfFields(); j++){
					if(j>0) s+=", ";
					s+=np.getTypeName(j);
				}
				s += ")";
				logger.debug("    > "+s);
				for(int j=0; j<np.numOfFields(); j++){
					logger.debug("    > getter "+j+" "+np.getGetterName(j)+"("+np.getTypeName(j)+")");
				}
			}
			else{
				for(int j=0; j<np.numOfFields(); j++){
					logger.debug("    > getter "+np.getGetterName(j)+"("+np.getTypeName(j)+") ->" +
					  " setter "+np.getSetterName(j)+"("+np.getTypeName(j)+")");
				}
			}
			
		}
		
		return namedPairList;
	}
	
	/**
	 * Finds an attribute in an XML element.
	 * @param attr The attribute to find.
	 * @param element The element where to search.
	 * @return The value of the attribute, or <code>null</code> if the
	 * attribute was not found.
	 */
    private static String findAttr(String attr, Node element){
    	String name = null;
    	if(element.getAttributes() != null)
    		if(element.getAttributes().getNamedItem(attr) != null)
    			name = element.getAttributes().getNamedItem(attr).getNodeValue();
    	return name;
    }
    
    /**
     * Finds child elements with a given name in an XML element.
     * @param parent The parent element.
     * @param childName The name of the child element to find.
     * @return A list with found child elements.
     */
    private static ArrayList<Node> findChildElements(Node parent, String childName){
    	ArrayList<Node> list = new ArrayList<Node>();
    	for(int i=0; i<parent.getChildNodes().getLength(); i++)
    		if(parent.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE)
				if( parent.getChildNodes().item(i).getNodeName().equalsIgnoreCase(childName) )
					list.add(parent.getChildNodes().item(i));
    	return list;
    }
	
    /**
     * Initializes protobuf classes.<br>
     * Parses the class matcher file.<br>
     * Loads protobuf classes.<br>
     * Creates entrys for XIOMaps (class <-> XIO tag)<br>
     */
    public static void initializeProtobuf(){
    	if(protobufDir == null){
    		protobufInitialized = true;
    		return;
    	}
    	
    	NamedPairList npl = parseMatches();
    	loadProtobufClasses(npl);
    	XIOMaps.loadXIOCodes(xiocodesFilename);
    	
    	protobufInitialized = true;
    }
    
    /**
     * Loads protobuf classes.
     * 
     * @param namedPairList List with class-to-class matches
     */
    private static void loadProtobufClasses(NamedPairList namedPairList){
		File protobufPath = new File(protobufDir);
		File dirForClassLoader = null;

		if(protobufPath.isDirectory()){
			dirForClassLoader = protobufPath.getParentFile();
		}
		else{
			logger.error("Error: Given protobuf path doesn't exist or is not a directory: "+protobufPath);
			return;
		}
		
		URL url;
		try {
			url = dirForClassLoader.toURI().toURL();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return;
		}
		URL[] urls = new URL[]{url};
		ClassLoader classLoader = new URLClassLoader(urls);
		XIOMaps.setAdditionalClassLoader(classLoader);
		
		File[] fileList = protobufPath.listFiles();
		for(int i=0; i<fileList.length; i++){
			String filename = fileList[i].getName();
			String ending = ".class";
			if(filename.endsWith(ending) && !filename.contains("$")){
				String primeClassname = filename.substring(0, filename.length() - ending.length());
				logger.debug("loading "+primeClassname+" from "+filename);
				
				Class<?> primeClass;
				try {
					primeClass = classLoader.loadClass("protobuf."+primeClassname);
					logger.debug("  primeClass: " + primeClass.getName());
				} catch (ClassNotFoundException e) {
					logger.error("Can not load class protobuf."+primeClassname);
					e.printStackTrace();
					continue;
				}
				
				Method getDescriptor = null;
				try {
					getDescriptor = primeClass.getMethod("getDescriptor");
				} catch (NoSuchMethodException | SecurityException e) {
					logger.error("Error: Method getDescriptor not found for "+primeClass.getName());
					continue;
				}
				if(getDescriptor == null){
					logger.error("Error: Found no Descriptor for "+primeClass.getName());
					continue;
				}
				
				FileDescriptor fileDescriptor = null;
				try {
					fileDescriptor = (FileDescriptor) getDescriptor.invoke(primeClass);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					logger.error("Error: Can't invoke "+getDescriptor.getName()+" on "+primeClass.getName());
					continue;
				}
				if(fileDescriptor == null){
					logger.error("Error: Found no FileDescriptor for "+primeClass.getName());
					continue;
				}
				
				// there is only one type defined per message, so it's 0
				String typeName = fileDescriptor.getMessageTypes().get(0).getName();
				String typeFullName = "protobuf."+primeClassname+"$"+typeName;
				Class<?> typeClass = null;
				try {
					typeClass = classLoader.loadClass(typeFullName);
				} catch (ClassNotFoundException e) {
					logger.error("Error: ClassLoader failed to load "+typeFullName);
					continue;
				}
				if(typeClass == null){
					logger.error("Error: "+typeFullName+" was not loaded!");
					continue;
				}
				
				Method newBuilderMethod = null;
				try {
					newBuilderMethod = typeClass.getMethod("newBuilder");
				} catch (NoSuchMethodException | SecurityException e1) {
					logger.error("Error: Method newBuilder() not found");
					continue;
				}
				if(newBuilderMethod == null){
					logger.error("Error: Method newBuilder() not found");
					continue;
				}
				
				Class<?> builderClass = null;
				Class<?>[] subClasses = typeClass.getClasses();
				for(int s=0; s<subClasses.length; s++){
					if(subClasses[s].getName().endsWith(typeName+"$Builder")){
						builderClass = subClasses[s];
					}
				}
				if(builderClass == null){
					logger.error("builderclass not found");
					continue;
				}
				
				Object builderInstance = null;
				try {
					builderInstance = newBuilderMethod.invoke(typeClass);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e2) {
					// nothing
				}
				if(builderInstance == null){
					logger.debug("Error: Failed to create an instance of builder for "+typeClass.getName());
					continue;
				}
				
				Method buildMethod = null;
				try {
					buildMethod = builderClass.getMethod("build");
				} catch (NoSuchMethodException | SecurityException e1) {
					logger.error("Error: Method build() not found.");
					continue;
				}
				if(buildMethod == null){
					logger.error("Error: Method build() not found.");
					continue;
				}
				
				
				// register this class in protobuf message converter
				
				Method getDefaultInstance;
				try {
					getDefaultInstance = typeClass.getMethod("getDefaultInstance");
				} catch (NoSuchMethodException | SecurityException e) {
					logger.error("Error: Can not access method from "+typeClass.getName());
					e.printStackTrace();
					continue;
				}
				ProtocolBufferConverter<?> conv;
				try {
					conv = new ProtocolBufferConverter((Message)getDefaultInstance.invoke(typeClass));
					DefaultConverterRepository.getDefaultConverterRepository().addConverter(conv);
				} catch (IllegalAccessException | IllegalArgumentException
						| InvocationTargetException e) {
					logger.error("Can not register converter for "+typeClass.getName());
					e.printStackTrace();
					continue;
				}
				logger.info("registering "+typeClass.getName()+" for RSB");
				
				
				// this protobuf class as a
				// TARGET
				
				if(namedPairList != null && namedPairList.size() > 0){
				
					NamedPair namedPairT = namedPairList.findNamedPairForTarget(typeClass.getCanonicalName());
					if(namedPairT == null){
						logger.error("Error: Abstract name pair not found for "+typeClass.getCanonicalName()+" as target");
						
						for(int iP = 0; iP<namedPairList.size(); iP++){
							if(iP == 0)
								logger.error("Pairs are: - "+namedPairList.get(iP));
							else
								logger.error("           - "+namedPairList.get(iP));
						}
						continue;
					}
					logger.debug("Working on pair '"+namedPairT+"'");
					
					Method[] setterMethodArray = new Method[namedPairT.numOfFields()];
					Class<?>[] typeArray = new Class[namedPairT.numOfFields()];
					boolean somethingMissing = false;
					for( int iField=0; iField<namedPairT.numOfFields(); iField++){
						String setterName = namedPairT.getSetterName(iField);
						if(setterName == null){
							logger.error("Error: Name for a setter-method not found.");
							somethingMissing = true;
							break;
						}
						typeArray[iField] = primitiveTypeMap.get(namedPairT.getTypeName(iField));
						if(typeArray[iField] == null){
							logger.error("Error: Class for type "+namedPairT.getTypeName(iField)+" not found.");
							logger.debug("Known Classes are:");
							for(Map.Entry<String, Class<?>> me : primitiveTypeMap.entrySet()){
								logger.debug(" - "+me.getKey());
							}
							somethingMissing = true;
							break;
						}
						try {
							setterMethodArray[iField] = builderClass.getMethod(setterName, typeArray[iField]);
						} catch (NoSuchMethodException | SecurityException e) {
							logger.error("Error: Method "+setterName+" for Class "+builderClass+" not found.");
							somethingMissing = true;
							break;
						}
					}
					if(somethingMissing) continue;
					
					Method resetter = null;
					try {
						resetter = builderClass.getMethod("clear");
					} catch (NoSuchMethodException | SecurityException e1) {
						logger.error("Error: Method 'clear' not found for "+builderClass.getName());
					}
					
					Class<?> sourceClass = null;
					try {
						sourceClass = Class.forName(namedPairT.getSourceName());
					} catch (ClassNotFoundException e) {
						logger.error("Error: Source class "+namedPairT.getSourceName()+" not found.");
						continue;
					}
					
					Method[] getterMethodArray = new Method[namedPairT.numOfFields()];
					somethingMissing = false;
					for(int iM = 0; iM<namedPairT.numOfFields(); iM++){
						try {
							if(namedPairT.isRepeated()){
								Class<?> componentClass = sourceClass.getComponentType();
								getterMethodArray[iM] = componentClass.getMethod(namedPairT.getGetterName(iM));
							}
							else{
								getterMethodArray[iM] = sourceClass.getMethod(namedPairT.getGetterName(iM));
							}
						} catch (NoSuchMethodException | SecurityException e) {
							logger.error("Error: Can't find getter method "+namedPairT.getGetterName(iM)+" for source class "+sourceClass.getName());
							somethingMissing = true;
							break;
						}
					}
					if(somethingMissing) continue;
					
					logger.debug("Parameters for "+typeClass.getName()+" as TARGET");
					logger.debug("  source class = "+sourceClass.getName());
					for(int iM=0; iM<getterMethodArray.length; iM++){
						if(iM == 0)
							logger.debug("  getters: - "+getterMethodArray[iM].getName());
						else
							logger.debug("           - "+getterMethodArray[iM].getName());
					}
					logger.debug("  target class = "+typeClass.getName());
					logger.debug("  builder class = "+builderClass.getName());
					for(int iS=0; iS<typeArray.length; iS++){
						if(iS == 0)
							logger.debug("  setters: - "+setterMethodArray[iS].getName());
						else
							logger.debug("           - "+setterMethodArray[iS].getName());
					}
					logger.debug("  build method:   "+buildMethod.getName());
					for(int iT=0; iT<typeArray.length; iT++){
						if(iT == 0)
							logger.debug("  types: - "+typeArray[iT].getName());
						else
							logger.debug("         - "+typeArray[iT].getName());
					}
					
					ClassMatcher cm = new ClassMatcher(
							sourceClass,
							typeClass,
							builderInstance,
							resetter,
							getterMethodArray,
							setterMethodArray,
							buildMethod,
							typeArray,
							namedPairT.isRepeated());
					
					classMatcherList.add(cm);
					matchOtherToProtobufMap.put(sourceClass, cm);
					
					
					// this protobuf class as a 
					// SOURCE
					
					NamedPair namedPairS = namedPairList.findNamedPairForSource(typeClass.getCanonicalName());
					if(namedPairS == null){
						logger.error("Error: Abstract name pair not found for "+typeClass.getCanonicalName()+" as source");
						
						for(int iP = 0; iP<namedPairList.size(); iP++){
							if(iP == 0)
								logger.debug("Pairs are: - "+namedPairList.get(iP));
							else
								logger.debug("           - "+namedPairList.get(iP));
						}
						continue;
					}
					logger.debug("Working on pair '"+namedPairS+"'");
					Class<?> targetClass = null;
					try {
						targetClass = Class.forName(namedPairS.getTargetName());
					} catch (ClassNotFoundException e) {
						logger.error("Error: Target class "+namedPairS.getTargetName()+" not found.");
						continue;
					}
					
					getterMethodArray = new Method[namedPairS.numOfFields()];
					somethingMissing = false;
					for(int iM = 0; iM<namedPairS.numOfFields(); iM++){
						try {
							if(namedPairS.isRepeated()){
								getterMethodArray[iM] = typeClass.getMethod(namedPairS.getGetterName(iM), int.class);
							}
							else{
								getterMethodArray[iM] = typeClass.getMethod(namedPairS.getGetterName(iM));
							}
						} catch (NoSuchMethodException | SecurityException e) {
							logger.error("Error: Can't find getter method "+namedPairS.getGetterName(iM)+" for "+typeClass.getName());
							somethingMissing = true;
							break;
						}
					}
					if(somethingMissing) continue;
					
					Constructor<?> constructor = null;
					try {
						if(namedPairS.isRepeated()){
							constructor = targetClass.getComponentType().getConstructor(typeArray);
						}
						else{
							constructor = targetClass.getConstructor(typeArray);
						}
					} catch (NoSuchMethodException | SecurityException e) {
						logger.error("Error: Constructor for "+targetClass.getName()+" not found.");
						continue;
					}
					
					logger.debug("Parameters for "+typeClass.getName()+" as SOURCE");
					logger.debug("  source class = "+typeClass.getName());
					for(int iM=0; iM<getterMethodArray.length; iM++){
						if(iM == 0)
							logger.debug("  getters: - "+getterMethodArray[iM].getName());
						else
							logger.debug("           - "+getterMethodArray[iM].getName());
					}
					logger.debug("  target class = "+targetClass.getName());
					logger.debug("  constructor  = "+constructor.getName());
					for(int iT=0; iT<typeArray.length; iT++){
						if(iT == 0)
							logger.debug("  types: - "+typeArray[iT].getName());
						else
							logger.debug("         - "+typeArray[iT].getName());
					}
					
					cm = new ClassMatcher(
							typeClass,
							targetClass,
							getterMethodArray,
							constructor,
							typeArray,
							namedPairS.isRepeated());
					
					classMatcherList.add(cm);
					matchProtobufToOtherMap.put(typeClass, cm);
					
				} // end if namedPairList
			} // end if filename
			
		}
		
		protobufInitialized = true;
    }
	
	/**
	 * Remove all slots.
	 */
	public static void removeAll(){
		// remove all in-slots
		for(Map.Entry<String, Listener> lis: inSlotMap.entrySet()){
			try {
				lis.getValue().deactivate();
			} catch (RSBException | InterruptedException e) {
				e.printStackTrace();
			}
		}

		// remove all out-slots
		for(@SuppressWarnings("rawtypes") Map.Entry<String, Informer> inf: outSlotMap.entrySet()){
			try {
				inf.getValue().deactivate();
			} catch (RSBException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Sends data to the outslot with the given label and namespace.
	 * If that outslot doesn't exists, it will be created (dynamic mode)
	 * or ignored (predefined mode).
	 * <p>
	 * The concatenation of the Namespace a slash and the Label gives
	 * the Scope.
	 * <p> 
	 * Example:<br>
	 * Namespace <code>'Venice/test'</code> and Label <code>'slotA'</code>
	 * gives the scope <code>'Venice/test/slotA'</code>.
	 * 
	 * @param label Label of the outslot (w/o leading or trailing slash)
	 * @param data the data to be send (class should match the datatype of the
	 * outslot.
	 * @param namespace Namespace of the outslot (w/o leading or trailing slash)
	 * @return <code>true</code> if the data was send without problems,
	 * otherwise <code>false</code>
	 */
	public static boolean write(String label, Object data, String namespace){
		return write(namespace + "/" + label, data);
	}
	
	/**
	 * Sends given data to given outslot (='informer'). If that outslot does
	 * not exist, it will be created (dynamic mode) or ignored (predefined mode).
	 * <p>
	 * A Scope consists of a Namespace and a Label. The part before the last
	 * slash is the Namespace and the part after the last slash is the Label.
	 * <p>
	 * Example:<br>
	 * The scope <code>'Venice/test/slotA'</code> consists of the Namespace
	 * <code>'Venice/test'</code> and the Label <code>'slotA'</code>.
	 *
	 * @param scope the scope of the outslot (='informer'), w/o leading or trailing slash
	 * @param data the data to be send (class should match the datatype of the
	 * out-slot.
	 * @return <code>true</code> if the data was send without problems,
	 * otherwise <code>false</code>
	 */
	public static boolean write(String scope, Object data){
		
		logger.debug("(write) got data to write, type: "+data.getClass().getName());
		if(data.getClass().getName().startsWith("protobuf")){
			Message m = (Message) data;
			List<FieldDescriptor> fdList = m.getDescriptorForType().getFields();
			for(FieldDescriptor fd : fdList){
				logger.debug(" field: "+fd.getName());
				if(fd.isRepeated()){
					int nField = m.getRepeatedFieldCount(fdList.get(0));
					for(int iField=0; iField<nField; iField++){
						Object value = m.getRepeatedField(fd, iField);
						logger.debug("  value "+iField+": "+value);
					}
				}
				else{
					Object value = m.getField(fd);
					logger.debug("  value: "+value);
				}
			}
		}
		
		// first, try to find a slot with the given scope in the map
		@SuppressWarnings("rawtypes")
		Informer informer = outSlotMap.get(scope);
		
		if(informer == null && !outSlotsPredefined){
			// if no outslot with the given scope was found
			// in dynamic mode, create missing outslot
			try {
				informer = Factory.getInstance().createInformer(prefix + scope);
				informer.activate();
				outSlotMap.put(scope, informer);
			} catch (InitializeException e) {
				e.printStackTrace();
			}
		}
		
		if(informer == null){
			// this will happen, if no outslot with this name exists and
			// RSB is used in predefined mode
			return false; // do nothing
		}
		else{
			// this will happen, if
			//  - outslot is found, or
			//  - outslot is not found, but created (because of dynamic mode)
			Class<?> type = data.getClass();
			if(type == String.class)
				try {
					logger.debug("(write) sending: "+data);
					informer.send(data);
				} catch (RSBException e) {
					logger.error("Failed to send "+data.toString()+" ("+data.getClass().getName()+")"+" to "+prefix+scope+" (RSBException)");
					return false;
				}
			else{
				logger.debug("processing non-string");
				ClassMatcher cm = matchOtherToProtobufMap.get(type);
				if(cm == null){
					logger.debug("no cm");
					// no matching class found, try to send data directly
					try {
						informer.send(data);
					} catch (RSBException e) {
						logger.error("Failed to send "+data.toString()+" ("+data.getClass().getName()+")"+" to "+prefix+scope+" (RSBException)");
						return false;
					}
				}
				else{
					// matching class found, convert data to target class
					logger.debug("cm available");
					if(cm.isRepeated){
						
						// data is a
						// MULTIFIELD
						
						Object[] dataArray = (Object[]) data;
						logger.debug("(write) detected multifield with "+dataArray.length+" fields.");
						cm.resetBuilder();
						for(int iA=0; iA<dataArray.length; iA++){
							logger.debug("(write)   working on field "+iA);
							for(int iV=0; iV<cm.type.length; iV++){
								logger.debug("(write)     working on value "+iV);
								Object value;
								try {
									value = cm.getter[iV].invoke(dataArray[iA]);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
									logger.error("Error: Failed to get value.");
									return false;
								}
								try {
									cm.setter[iV].invoke(cm.builderInstance, value);
								} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
									logger.error("Error: Failed to set value.");
									e.printStackTrace();
									return false;
								}
							}
						}
						try {
							informer.send(cm.build.invoke(cm.builderInstance));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | RSBException e) {
							logger.error("Error: Failed to send data to informer.");
							return false;
						}
						
					}
					else{
						
						// data is a
						// SINGLEFIELD
						
						for(int i=0; i<cm.type.length; i++){
							Object value;
							try {
								value = cm.getter[i].invoke(data);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
								logger.error("Error: Failed to get value.");
								return false;
							}
							//logger.debug("    value["+i+"]="+value+" ("+cm.type[i].getName()+")");
							try {
								cm.setter[i].invoke(cm.builderInstance, value);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
								logger.error("Error: Failed to set value.");
								e.printStackTrace();
								return false;
							}
						}
						try {
							informer.send(cm.build.invoke(cm.builderInstance));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | RSBException e) {
							logger.error("Error: Failed to send data to informer.");
							return false;
						}					
					}
				}
			}
			return true;
		}
	}
	
	/**
	 * Here the application can register its listener to receive incoming data.
	 * @param inSlotListener A listener object, which will receive incoming data.
	 */
	public static void setMasterInSlotListener(AbstractSlotListener inSlotListener){
		masterInSlotListener = inSlotListener;
	}
	
	/**
	 * @return The registered listener that receives incoming data.
	 */
	public static AbstractSlotListener getMasterInSlotListener(){
		return masterInSlotListener;
	}
	
	/**
	 * Sets a prefix for all namespaces for all slots created by this
	 * NamespaceBuilder. This is especially useful for dynamic slot creation
	 * if the slots should not be created on the root, but on a namespace
	 * given by the application.
	 * @param prfx
	 */
	public static void setPrefix(String prfx){
		prefix = prfx;
		if(!prefix.startsWith("/")) prefix = "/" + prefix;
		if(!prefix.endsWith("/")) prefix += "/";
		logger.info("prefix set to "+prefix);
	}
	
	/**
	 * Sets the path to the protobuf folder.
	 * @param dir A path to the protobuf folder.
	 */
	public static void setProtobufDir(String dir){
		protobufDir = dir;
	}
	
	/**
	 * Sets the name of a XML file with class-to-class matching
	 * definitions.
	 * @param dir Filename (can include path)
	 */
	public static void setMatchFile(String dir){
		matchFilename = dir;
	}
	
	/**
	 * Gets the map of matching between protobuf classes and other classes.
	 * @return Map with class-matching-definitions
	 */
	public static HashMap<Class<?>, ClassMatcher> getMatchProtobufToOtherMap(){
		return matchProtobufToOtherMap;
	}
	
	/**
	 * Set the name of a XML file with XIO code mapping.
	 * @param filename A filename (can include path)
	 */
	public static void setXioCodesFilename(String filename){
		xiocodesFilename = filename;
	}
	
	/**
	 * Returns the name of the file with XIO code mapping.
	 * @return Name of the file with XIO code mapping
	 */
	public static String getXioCodesFilename(){
		return xiocodesFilename;
	}

}
