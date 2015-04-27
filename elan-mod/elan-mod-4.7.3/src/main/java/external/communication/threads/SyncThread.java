package external.communication.threads;

import external.communication.CommunicationClient;
import external.communication.Configuration;
import external.communication.EventHook;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This thread is meant to synchornize elan_mod with the playback tool every once in a short while.
 * Therefor it constantly send seek_play commands via the communicationClient that seek to the exact timestamp.
 * This generates a minimal lag, but asures that the ELAN videoplayback is synchronized with the instantReality scene.
 * 
 * @author jeeickme
 */
public class SyncThread extends Thread {

    private volatile boolean stop = false;
    private EventHook ev;
    private CommunicationClient client;
    private int sleeptime;

    /**
     * In order to manage synchornization there a some values needed.
     * 
     * @param ev    The Eventhook that started this thread.
     * @param client client that is used to send event.
     * @param conf  Configuration class to get important external config values.
     */
    public SyncThread(EventHook ev, CommunicationClient client, Configuration conf) {
        this.ev = ev;
        this.client = client;
        this.sleeptime = conf.syncFreq;
    }

    /**
     * method to stop synchronization thread.
     */
    public void requestStop() {
        stop = true;
    }

    /**
     * starts the synchronization thread.
     */
    
    @Override
    public void run() {
        while (!stop) {
            if (ev.isPlayer()) {
                try {
                    sleep(1000);
                    if (ev.isPlayerPlaying()) {
                        long time = this.ev.getPlayer().getMasterMediaPlayer().getMediaTime();
                        client.sendEvent("seek_play", time);
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(SyncThread.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
