package external.communication.threads;

import external.communication.CommunicationClient;
import external.communication.EventHook;

/**
 * This threads observes the behaviour of the player in ELAN. If something interessting happens, 
 * like play or pause commands, than this thread reacts with invoking an event and send it via the client
 * to the IR playbacktool.
 * 
 * @author jeeickme
 */
public class PlayerBehaviourThread extends Thread {

    private boolean stop = false;
    private EventHook ev = null;
    private CommunicationClient client = null;
    private boolean wasPlay;
    private long lastTime;

    /**
     * init the thread
     * 
     * @param ev    EventHook that initialized this thread
     * @param client communication client that is used for invoking events.
     */
    public PlayerBehaviourThread(EventHook ev, CommunicationClient client) {
        this.ev = ev;
        this.client = client;
    }
    
    /**
     * method to stop synchronization thread.
     */
    public void stopThread() {
        this.stop = true;
    }

    /**
     * starts the thread.
     */
    @Override
    public void run() {
        if (this.ev.isPlayer()) {
            //init
            lastTime = this.ev.getMediaTime();
            wasPlay = this.ev.isPlayerPlaying();
        }

        while (!stop) {
            if (this.ev.isPlayer()) {
                if (checkPlayChange()) {
                    if (wasPlay) {
                        this.client.sendEvent("pause", this.ev.getMediaTime());
                    } else {
                        this.client.sendEvent("play", this.ev.getMediaTime());
                    }
                    wasPlay = this.ev.isPlayerPlaying();
                }
                if (checkTimeChange()) {
                    if (!wasPlay) {
                        this.client.sendEvent("seek_pause", this.ev.getMediaTime());
                    }
                    lastTime = this.ev.getMediaTime();
                }
            }
        }
    }

    /**
     * checks if the time has changed on the media
     * @return boolean
     */
    private boolean checkTimeChange() {
        long playerTime = this.ev.getMediaTime();
        if (playerTime != lastTime) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * check if the playing state has changend on the ELAN player
     * @return boolean
     */
    private boolean checkPlayChange() {
        boolean playerState = this.ev.isPlayerPlaying();
        if (playerState != wasPlay) {
            return true;
        } else {
            return false;
        }
    }
}
