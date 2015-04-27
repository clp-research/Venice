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

import org.apache.log4j.Logger;

/**
 * Maintains a list of named pairs for the class matcher.
 * Everything is still abstract (means: only strings, no real classes
 * or methods).
 */
public class NamedPairList {
	private static Logger logger = Logger.getLogger(NamedPairList.class);
	ArrayList<NamedPair> pairList;
	
	public NamedPairList(){
		logger.setLevel(org.apache.log4j.Level.ERROR);
		pairList = new ArrayList<>();
	}
	
	/**
	 * Adds a new class-to-class matching pair to the list.
	 * @param namedPair
	 */
	public void add(NamedPair namedPair){
		pairList.add(namedPair);
	}
	
	/**
	 * Finds a name-pair by the name of the target class.
	 * 
	 * @param targetName Name of the target class of the matching pair.
	 * @return The corresponding NamedPair, or <code>null</code>
	 */
	public NamedPair findNamedPairForTarget(String targetName){
		return findNamedPair(targetName, false, true);
	}
	
	/**
	 * Finds a name-pair by the name of the source class.
	 * 
	 * @param sourceName Name of the source class of the matching pair.
	 * @return The corresponding NamedPair, or <code>null</code>
	 */
	public NamedPair findNamedPairForSource(String sourceName){
		return findNamedPair(sourceName, true, false);
	}
	
	/**
	 * Find a name-pair by the name of the source and/or the target class.
	 * 
	 * @param name Name of the source/target class of the matching pair
	 * @param isSource If searched named must match source class name.
	 * @param isTarget If searched named must match target class name.
	 * @return The corresponding NamedPair, or <code>null</code>
	 */
	public NamedPair findNamedPair(String name, boolean isSource, boolean isTarget){
		NamedPair pair = null;
		for(NamedPair np : pairList){
			if( (isSource && np.getSourceName().equals(name)) || (isTarget && np.getTargetName().equals(name))){
				pair = np;
				break;
			}
		}
		return pair;		
	}
	
	/**
	 * Get a NamedPair specified by index.
	 * @param index
	 * @return NamedPair at given index
	 */
	public NamedPair get(int index){
		return pairList.get(index);
	}
	
	/**
	 * Returns the number of named pairs maintained by this list.
	 * @return number of named pairs maintained by this list
	 */
	public int size(){
		return pairList.size();
	}
}
