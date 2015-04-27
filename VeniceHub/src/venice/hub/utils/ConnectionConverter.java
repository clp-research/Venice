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
package venice.hub.utils;

import venice.hub.utils.Configuration.Connection;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * Converts an object into a Connection.
 * 
 * @author Oliver Eickmeyer
 *
 */
public class ConnectionConverter implements IStringConverter<Connection>{
	
	/**
	 * Converts a String into a Connection.
	 * @param arg String representation of a Connection
	 * @return Connection represented by the String argument
	 */
	@Override
	public Connection convert(String arg) {
		Connection result = null;
		try{
			result = Connection.valueOf(arg.toUpperCase());
		}
		catch(IllegalArgumentException e){
			throw new ParameterException("Parameter "+arg+" should be \"Disk\", \"IIO\", \"RSB\" or \"VP\"");
		}
		return result;
	}
}
