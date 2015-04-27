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

import java.util.ArrayList;

/**
 * Holds the names of classes and methods for a class-to-class match.
 * Everything is handled only with strings, so all the classes and methods
 * defined by a NamedPair are given by strings.
 * There have to be a source class and a target class.
 * There are two modes for the methods for the target class:<br>
 * <em>Target by constructor</em> and <em>target by builder</em>.<br>
 * <em>Target by constructor</em> means, that the target class is instantiated by
 * its constructor. That constructor takes as parameters the types defined
 * in the <code>typeNameList</code> (and in that order).<br>
 * <em>Target by builder</em> means, that the target class is instantiated by a
 * builder. That builder is got from the target class by the static
 * <code>newBuilder</code>-method and has a <code>build</code>-method
 * that returns an instance of the target-class. The fields are set with the
 * setter-methods (invoked on the builder).
 * 
 */
public class NamedPair {
	private String sourceName; // Class
	private ArrayList<String> getterNameList; // Methods

	private String targetName; // Class
	
	private String builderName; // Class
	private ArrayList<String> setterNameList; // Methods
	private boolean repeated; // is it an array?
	private String buildName; // Method
	
	private ArrayList<String> typeNameList; // Classes
	
	private boolean useConstructor;
	
	/**
	 * Creates an empty pair.
	 */
	public NamedPair(){
		sourceName = null;
		getterNameList = new ArrayList<>();
		targetName = null;
		builderName = null;
		setterNameList = new ArrayList<>();
		buildName = null;
		typeNameList = new ArrayList<>();
		useConstructor = false;
		repeated = false;
	}
	
	/**
	 * Adds a pair of a getter-method and a setter-method. Also activates the
	 * <em>Target by builder</em> mode.
	 * @param getterName Name of the getter-method
	 * @param setterName Name of the setter-method
	 * @param typeName Name of the type (=class) of the parameter (for both methods).
	 */
	public void addMethodPair(String getterName, String setterName, String typeName){
		getterNameList.add(getterName);
		setterNameList.add(setterName);
		typeNameList.add(typeName);
		useConstructor = false;
	}
	
	/**
	 * Sets a constructor that have a single parameter. Also activates the
	 * <em>Target by constructor</em> mode.
	 * @param typeName Name of the type of the parameter.
	 */
	public void setConstructorWithSingleParam(String typeName){
		typeNameList = new ArrayList<>();
		typeNameList.add(typeName);
		useConstructor = true;
	}
	
	/**
	 * Adds a constructor parameter. Also activates the
	 * <em>Target by constructor</em> mode.
	 * @param index Position of the parameter (beginning with 0).
	 * @param typeName Name of the type of the parameter.
	 */
	public void addConstructorParam(int index, String typeName){
		if(typeNameList == null) typeNameList = new ArrayList<>();
		while(index >= typeNameList.size())
			typeNameList.add(null);
		typeNameList.set(index, typeName);
		useConstructor = true;
	}
	
	/**
	 * Sets if this describes a singlefield or a multifield.
	 * @param repeated <code>false</code>=singlefield (default),
	 * <code>true</code>=multifield
	 */
	public void setRepeated(boolean repeated){
		this.repeated = repeated;
	}
	
	/**
	 * Returns if this describes a singlefield or a multifield.
	 * @return <code>false</code>=singlefield,
	 * <code>true</code>=multifield
	 */
	public boolean isRepeated(){
		return repeated;
	}
	
	/**
	 * Checks if <em>Target by constructor</em> mode or
	 * <em>Target by builder</em> mode is active.
	 * @return <code>true</code> if a constructor is used; <code>false</code> if a builder is used 
	 */
	public boolean isUsingConstructor(){
		return useConstructor;
	}
	
	/**
	 * Gets the name of the source class.
	 * @return The name of the source class.
	 */
	public String getSourceName(){
		return sourceName;
	}
	
	/**
	 * Sets the name of the source class.
	 * @param sourceName The name of the source class.
	 */
	public void setSourceName(String sourceName){
		this.sourceName = sourceName;
	}
	
	/**
	 * Gets the name of the target class.
	 * @return The name of the target class.
	 */
	public String getTargetName() {
		return targetName;
	}
	
	/**
	 * Sets the name of the target class.
	 * @param targetName The name of the target class.
	 */
	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}
	
	/**
	 * Gets the name of the builder.
	 * @return The name of the builder.
	 */
	public String getBuilderName() {
		return builderName;
	}
	
	/**
	 * Sets the name of the builder.
	 * 
	 * @param builderName Name of the builder.
	 */
	public void setBuilderName(String builderName) {
		this.builderName = builderName;
	}
	
	/**
	 * Returns the name of the i'th getter method.
	 * @param i Index of the getter method.
	 * @return Name of the i'th getter method, or <code>null</code>
	 * if there is no value with this index.
	 */
	public String getGetterName(int i){
		if(i < getterNameList.size())
			return getterNameList.get(i);
		else
			return null;
	}
	
	/**
	 * Sets the name of the getter-method for the i'th value.
	 * @param i Index of the value, that is got by the getter.
	 * @param getterName The name of the getter-method.
	 */
	public void setGetterName(int i, String getterName){
		if(getterNameList == null) getterNameList = new ArrayList<>();
		while(i >= getterNameList.size())
			getterNameList.add(null);
		getterNameList.set(i, getterName);
	}
	
	/**
	 * Returns the name of the i'th setter method.
	 * @param i Index of the setter method.
	 * @return Name of the i'th setter method, or <code>null</code>
	 * if there is no value with this index.
	 */
	public String getSetterName(int i){
		if(i < setterNameList.size())
			return setterNameList.get(i);
		else
			return null;
	}
	
	/**
	 * Returns the name of the type of the i'th value.
	 * @param i Index of the value.
	 * @return Name of the type, or <code>null</code> if there
	 * is no value with this index.
	 */
	public String getTypeName(int i){
		if(i < typeNameList.size())
			return typeNameList.get(i);
		else
			return null;
	}
	
	/**
	 * Gets the name of the build-method.
	 * @return the name of the build-method
	 */
	public String getBuildName() {
		return buildName;
	}
	
	/**
	 * Sets the name of the build-method.
	 * @param buildName the name of the build-method
	 */
	public void setBuildName(String buildName) {
		this.buildName = buildName;
	}
	
	/**
	 * Returns a String representation of this object.
	 */
	public String toString(){
		return sourceName + " to " + targetName;
	}
	
	/**
	 * @return the number of fields (the relevant values for
	 * matching both classes).
	 */
	public int numOfFields(){
		return typeNameList.size();
	}
}
