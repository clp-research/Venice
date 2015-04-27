package external.communication;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.msgpack.rpc.Client;
import org.msgpack.rpc.loop.EventLoop;

/**
 * Client class for the communication with the IR replaytool.
 * 
 * @author jeeickme
 */

public class CommunicationClient {

    static RPCBrowserInterface iface;
    private Client cli;
    private EventLoop loop;
    //default
    private String ip;
    private int port;
    private boolean clientCon = false;
    private EventHook ev;

    /**
     * 
     * Constructer. 
     * 
     * @param ev        Eventhook that hast started this client.
     * @param offset    offset for the timestamps. In milliseconds.
     * @param port      portnumber to publish at.
     * @param server    server to publish at.
     */
    
    public CommunicationClient(EventHook ev, int port, String server) {
        this.ev = ev;
        this.port = port;
        this.ip = server;
        this.startClient();
    }

    /**
     * Checks if the client is active at the moment.
     * @return boolean
     */
    public boolean isActiv() {
        return this.clientCon;
    }

    /**
     * Try to start the client on the given IP and port.
     */
    public void startClient() {
        
        try {
            loop = EventLoop.defaultEventLoop();
            cli = new Client(this.ip, this.port, loop);
            cli.setRequestTimeout(90);
            iface = cli.proxy(RPCBrowserInterface.class);
            this.clientCon = true;
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            System.err.println("host not found, check /src/client.properties");
        } 
    }

    /**
     * Sending an event over this client to the IR playbacktool
     * 
     * @param eType Type of event as string. ("play","pause","seek_play","seek_pause","stop")
     * @param timestamp timestamp of the event. In milliseconds.
     */
    public void sendEvent(String eType, long timestamp) {
        if (clientCon) {
            if (eType.equals("play")) {
                System.out.println("play");
                iface.play();
                System.out.println("seek" + " : " + timestamp);
                iface.seek(timestamp);
            } else if (eType.equals("stop")) {
                System.out.println("stop");
                iface.pause();
            } else if (eType.equals("seek_pause")) {

                iface.play();
                iface.seek(timestamp);
                iface.pause();

            } else if (eType.equals("seek_play")) {
                 iface.seek(timestamp);
            } else if (eType.equals("pause")) {
//            System.out.println("pause");
                iface.pause();
            }
        }
    }
    
    public void stopClient(){
      if (clientCon){

        // close RPC client
        cli.close();
        
        // shut down event loop
        loop.shutdown();
      }
    }
}
