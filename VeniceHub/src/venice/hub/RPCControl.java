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

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.log4j.Logger;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.loop.EventLoop;

import venice.hub.utils.Configuration;

/**
 * Sets up a RPC server for controlling VeniceHub via Remote-Procedure-Call.
 * <p>
 * A RPC client can call:
 * <ul>
 * <li><code>play()</code></li>
 * <li><code>pause()</code></li>
 * <li><code>play_pause()</code></li>
 * <li><code>seek(long timestamp)</code></li>
 * <li><code>quit()</code></li>
 * </ul>
 */
public class RPCControl extends VeniceControl{
	private static Logger logger;
	static {
		// setup logger as early as possible
		venice.lib.Configuration.setupLogger();
		logger = Logger.getLogger(RPCControl.class);
	}
	private EventLoop loop;
	private Server rpcServer;
	private String address;
	private int port;
	
	/**
	 * Constructs the RPC control.
	 */
	public RPCControl(){
		super();
		rpcServer = new Server();
        rpcServer.serve(this);
        address = Configuration.getInstance().getRPCServerAddress();
        port = Configuration.getInstance().getRPCServerPort();
        try {
			rpcServer.listen(new InetSocketAddress(address, port));
			VeniceHub.message("RPC Server created ("+address+":"+port+").");
		} catch (IOException e) {
			System.err.println("Can not listen to port "+port);
		}
	}
	
	/**
	 * Just waits until VeniceHub command this thread to stop.
	 */
	@Override
	public void run() {
		loop = EventLoop.defaultEventLoop();
		try {
			loop.join();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		finished = true;
		logger.debug("RPCControl finished");
	}
	
    /**
     * Called by VeniceHub when this thread has to be stopped.
     */
    public void stopThread() {
    	active = false;
    	loop.shutdown();
    	rpcServer.close();
    }
	
	/**
	 * Command VeniceHub to continue playing.
	 */
	public void play(){
		VeniceHub.message("RPC Server requested play");
		VeniceHub.setPause(false);
	}
	
	/**
	 * Command VeniceHab to pause.
	 */
	public void pause(){
		VeniceHub.message("RPC Server requested pause");
		VeniceHub.setPause(true);
	}

	/**
	 * Command VeniceHub to switch between pause and play.
	 */
	public void play_pause() {
		VeniceHub.message("RPC Server requested play/pause");
		VeniceHub.switchPause();
	}

	/**
	 * Command VeniceHub to seek for a certain timestamp.
	 * 
	 * @param timestamp The timestamp to be seeked
	 */
	public void seek(long timestamp) {
		VeniceHub.seekForRelativePosition(timestamp);
	}
	
	/**
	 * Command VeniceHub to quit.
	 */
	public void quit(){
		VeniceHub.quit();
	}
	
	@Override
	public String toString(){
		return "RPCControl";
	}
}
