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
package venice.hub;

/**
 * Control classes are the user interfaces to VeniceHub.
 * They receive user commands and forward them to VeniceHub,
 * by calling the appropriate method.
 * <p>
 * Example methods from VeniceHub:<br>
 * <ul>
 * <li><code>setPause(boolean pause)</code></li>
 * <li><code>switchPause()</code></li>
 * <li><code>quit()</code></li>
 * <li><code>seekForRelativePosition(long relativeTimestamp)</code>
 * <li><code>seekForTimestamp(long seekTime)</code>
 * <li><code>reset()</code></li>
 * </ul>
 * There are more methods, look for the public static methods in VeniceHub.
 * <p>
 * This should be used as a thread. Stop it by calling <code>stopThread()</code>.
 */
public abstract class VeniceControl implements Runnable{
	protected boolean active;
	protected boolean finished;
	
	public VeniceControl(){
		active = true;
		finished = false;
	}

	@Override
	public void run() {
		// implemented by sub class
	}
	
    /**
     * Called by VeniceHub when this thread has to be stopped.
     */
    public void stopThread() {
    	active = false;
    }
    
    /**
     * Returns <code>true</code> if this Thread can be safely killed.
     * <p>
     * After the call of <code>stopThread</code> this Thread will try to get into a safe closing state.
     * @return <code>true</code> if it is safe to kill this thread
     */
    public boolean isFinished(){
    	return finished;
    }
}
