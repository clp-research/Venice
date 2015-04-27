package external.communication;

import external.communication.threads.PlayerBehaviourThread;
import external.communication.threads.SyncThread;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle all needed RPC connections. On initialisation the handler starts up
 * the browserclient for communication with instantReality and the remote server to provide 
 * a connection for external remotecontrol.
 * 
 * @author jeeickme
 */
public class CommunicationHandler {

    private CommunicationClient browserClient;
    private CommunicationServer remoteServer;
    private Configuration config;
    private EventHook ev;
    private SyncThread syncT;
    private PlayerBehaviourThread playThread;

    /**
     * Constructor. Start up the engine for communication gentlemen.
     * 
     * @param ev    EventHook that calls the communication handler. 
     */
    
    public CommunicationHandler(EventHook ev) {

        config = new Configuration("config.properties");
        this.ev = ev;

        System.out.println("Starting up the browser client");
        startBrowserClient();
        System.out.println("Starting the Remote Server");
        startRemoteServer();

        this.playThread = new PlayerBehaviourThread(this.ev, this.browserClient);
        playThread.start();
        this.syncT = new SyncThread(this.ev, this.browserClient, this.config);
        syncT.start();

    }

    /**
     * starting the remote server for communication with any remote controll
     * application
     *
     */
    private void startRemoteServer() {
        try {
            this.remoteServer = new CommunicationServer(config.remotePort, config.remoteIP, ev.getPlayer());
        } catch (IOException ex) {
            Logger.getLogger(EventHook.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Failed to start Server");
        }
    }

    /**
     * starting the browser client for communication with the instantReality playback
     * application
     *
     */
    private void startBrowserClient() {
        browserClient = new CommunicationClient(ev, config.browserPort, config.browserIP);
    }

    /**
     * Get a reference of the browser client
     * @return reference of browserClient
     */
    public CommunicationClient getBrowserClient() {
        return this.browserClient;
    }

    /**
     * Get a reference of the remoteServer
     * @return reference of remoteServer
     */
    public CommunicationServer getRemoteServer() {
        return this.remoteServer;
    }
    
    public void stopConnection(){
      browserClient.stopClient();
    }
}
