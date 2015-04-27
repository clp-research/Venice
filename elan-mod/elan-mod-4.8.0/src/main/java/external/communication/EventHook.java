package external.communication;

import mpi.eudico.client.annotator.ViewerManager2;

/*
 *  This is the main class of ELAN_mod. It is a container for the hooks in ELAN that grabs the videoManager.
 *  Also this class is starting up the communication between ELAN_mod
 *  and the InstantReality replaytool, as well as the communication with an external remote controll app.
 *
 */
public class EventHook {

    private static EventHook instance = null;
    private ViewerManager2 player = null;
    private CommunicationClient browserClient;
    private CommunicationServer controllServer;
    private CommunicationHandler comHandler;

    private long beginTracking = 0;
    // default connection settings
    
    private boolean isRemoteServer = false;

    private EventHook() {
       
    }

    /**
     * singelton structure to assure there is only one instance
     *
     * @return instance of EventHook
     *
     */
    public static EventHook getInstance() {

        if (instance == null) {
            instance = new EventHook();
        }

        return instance;

    }

    /**
     * This method starts a new CommunicationHandler.
     *
     */
    public void setCommunicationHandler(){
        this.comHandler = new CommunicationHandler(this);
    }
    
    /**
     * set the play which is used from ELAN
     *
     */
    public void setPlayer(ViewerManager2 player) {
        this.player = player;
        // the player is needed to be existent befor starting up the remote Server
        //startRemoteServer();

    }

    /**
     * check whether a player is set or not
     *
     */
    public boolean isPlayer() {
        return player != null;
    }


    /**
     * Method that returns wether if the ELAN mediaplayer is in playing state or not
     * 
     * @return  boolean
     */
    public boolean isPlayerPlaying() {
        return this.player.getMasterMediaPlayer().isPlaying();
    }

    
    /**
     * Gets the actualt mediatime of the ELAN medialplayer
     * 
     * @return time in milliseconds as long
     */
    public long getMediaTime() {
        long time = player.getMasterMediaPlayer().getMediaTime();
        return time;
    }

    /**
     * Gets a refference of the ELAN viewerManager
     * 
     * @return ViewerManager2
     * @see ViewerManager2
     */
    public ViewerManager2 getPlayer() {
        return this.player;
    }
    
    public void stopRPCConnection(){
      comHandler.stopConnection();
    }
}
