package external.communication;

import java.io.IOException;
import mpi.eudico.client.annotator.ElanMediaPlayerController;
import mpi.eudico.client.annotator.MediaPlayerControlSlider;
import mpi.eudico.client.annotator.ViewerManager2;
import mpi.eudico.client.annotator.commands.CommandAction;
import mpi.eudico.client.annotator.commands.ELANCommandFactory;
import org.msgpack.rpc.Server;
import org.msgpack.rpc.loop.EventLoop;

/**
 * This is the communicationServer class that handles incoming RPC commands and transforms them
 * to ELAN actions.
 * 
 * @author jeeickme
 */
public class CommunicationServer implements RPCRemoteInterface {

    // rpc interface
    private EventLoop rpcEventLoop;
    private Server rpcServer;
    private ViewerManager2 vM;

    /**
     * starting up the server.
     * 
     * @param port  port to which the server connects
     * @param serverIP  IP to which the server connects
     * @param vM    ViewerManager2 that will be controlled.
     * @throws IOException 
     */
    public CommunicationServer(int port, String serverIP, ViewerManager2 vM) throws IOException {
        System.out.println("rpc server listening on port " + port);

        this.rpcEventLoop = EventLoop.defaultEventLoop();
        this.rpcServer = new Server();

        rpcServer.serve(this);
        rpcServer.listen(port);

        this.vM = vM;
    }

    /**
     * RPCBrowserInterface - invoking the PlayPause command in ELAN
     */
    @Override
    public void play_pause() {
        try {
            CommandAction ca = ELANCommandFactory.getCommandAction(vM.getTranscription(), ELANCommandFactory.PLAY_PAUSE);
            ca.actionPerformed(null);
        } catch (Exception e) {
            System.out.println("Exception " + e + " appeared when executing the command.Action");
        }
    }
    /**
     * RPCBrowserInterface - invoking the seek command in ELAN
     */
    @Override
    public void seek(long timestamp) {
        vM.getMasterMediaPlayer().setMediaTime(timestamp);
    }
    
    /**
     * RPCBrowserInterface - invoking the play_selection command in ELAN
     */
    @Override
    public void play_selection(){
        try {
            CommandAction ca = ELANCommandFactory.getCommandAction(vM.getTranscription(), ELANCommandFactory.PLAY_SELECTION);
            ca.actionPerformed(null);
        } catch (Exception e) {
            System.out.println("Exception " + e + " appeared when executing the command.Action");
        }
    }
    
    /**
     * RPCBrowserInterface - invoking the set_selection command in ELAN
     */
    @Override
    public void set_selection(long beginTime, long endTime){
        vM.getSelection().setSelection(beginTime, endTime);
    }
    
    /**
     * RPCBrowserInterface - invoking the toogleLoopMode command in ELAN
     */
    @Override
    public void toogleLoopMode(){
        try {
            CommandAction ca = ELANCommandFactory.getCommandAction(vM.getTranscription(), ELANCommandFactory.LOOP_MODE);
            ca.actionPerformed(null);
        } catch (Exception e) {
            System.out.println("Exception " + e + " appeared when executing the command.Action");
        }
    }
    
    
}