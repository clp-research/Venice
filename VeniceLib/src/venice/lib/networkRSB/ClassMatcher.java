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
package venice.lib.networkRSB;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Collects reflection classes and methods for matching data holding classes
 * between different protocols. If a builder is present, it also holds an
 * instance of that builder class, ready to use for creating instances of the
 * target class.
 * <p>
 * All needed information have to be given on construction and can't be changed
 * afterwards. As this are final fields, they are directly accessed (no methods
 * to set or get fields.) 
 *
 */
public class ClassMatcher {
	/**
	 * The source class, that get matched to the target class.
	 */
	public final Class<?> source;
	/**
	 * An array of methods that are used to get a value from an object of
	 * the source class.
	 */
	public final Method[] getter;
	/**
	 * The target class, to which the source class gets matched.
	 */
	public final Class<?> target;
	
	/**
	 * The builder object that builds an instance of the target class.
	 */
	public final Object builderInstance;
	/**
	 * The method of the builder to reset all values.
	 */
	public final Method resetter;
	/**
	 * The method of the builder to set a value. 
	 */
	public final Method[] setter;
	/**
	 * The method of the builder to build an object of the
	 * target class with the set values.
	 */
	public final Method build;
	
	/**
	 * The constructor of the target class.
	 */
	public final Constructor<?> constructor;
	
	/**
	 * The types of the values that are getting matched between
	 * both classes.
	 */
	public final Class<?>[] type;
	
	/**
	 * An indicator, if an instance of the target class is build
	 * with a constructor or with a builder.
	 */
	public final boolean useConstructor;
	
	/**
	 * An indicator, if this is a multifield class.
	 */
	public final boolean isRepeated;
	
	/**
	 * Constructs the settings for a matching situation with
	 * a constructor for the target.<br>
	 * It is assumed that the first getter-method provides the value for
	 * the first constructor parameter, the second getter for the second
	 * constructor parameter, and so on.
	 * 
	 * @param source Class to match
	 * @param target Class to match to
	 * @param getter getter-methods for the source class
	 * @param constructor the constructor for the matching target class
	 * @param type the types for each pair of a getter and a constructor parameter
	 * @param isRepeated <code>false</code> for singlefields, <code>true</code> for multifields
	 */
	public ClassMatcher(
			Class<?> source,
			Class<?> target,
			Method[] getter,
			Constructor<?> constructor,
			Class<?>[] type,
			boolean isRepeated
			){
		this.source = source;
		this.target = target;
		this.getter = getter;
		this.constructor = constructor;
		this.type = type;
		this.isRepeated = isRepeated;
		
		this.builderInstance = null;
		this.resetter = null;
		this.setter = null;
		this.build = null;
		
		this.useConstructor = true;
	}
	
	/**
	 * Constructs the settings for a matching situation with
	 * a builder for the target.<br>
	 * The builder is used to build an instance of the target class.
	 * The setter-methods are invoked on the builder.
	 * It is assumed that the first getter-method provides the value for the
	 * first setter-method (or the first parameter of the constructor), the
	 * second getter for the second setter (or second constructor parameter),
	 * and so on.
	 * 
	 * @param source Class to match
	 * @param target Class to match to
	 * @param builderInstance An instantiated builder that builds instances of
	 * the target class
	 * @param resetter method of builder class to reset values
	 * @param getter getter-methods of the source class
	 * @param setter setter-methods of the builder class
	 * @param type the types for each pair of a getter and a setter method
	 * @param isRepeated <code>false</code> for singlefields, <code>true</code> for multifields
	 */
	public ClassMatcher(
			Class<?> source,
			Class<?> target,
			Object builderInstance,
			Method resetter,
			Method[] getter,
			Method[] setter,
			Method build,
			Class<?>[] type,
			boolean isRepeated
			){
		this.source = source;
		this.target = target;
		this.builderInstance = builderInstance;
		this.resetter = resetter;
		this.getter = getter;
		this.setter = setter;
		this.build = build;
		this.type = type;
		this.isRepeated = isRepeated;
		
		constructor = null;
		
		useConstructor = false;
	}
	
	/**
	 * Resets the values of the builder instance.
	 * This is important for multifields, because values are usually added
	 * and not set. If no reset is done, the next data object would contain
	 * additionally the values of the last data object.<br>
	 * For singlefields it is usually not neccessary to reset the values,
	 * because they get overwritten.
	 */
	public void resetBuilder(){
		try {
			resetter.invoke(builderInstance);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			System.err.println("Error: Can't reset values on builder instance "+builderInstance.getClass().getName());
		}
	}
	
	/**
	 * Gives a human comprehensible string representation of this
	 * ClassMatcher instance.
	 * @return String string representation of this instance
	 */
	public String toString(){
		String s = "";
		s = source.getName() + " to " + target.getName();
		
		return s;
	}
}
