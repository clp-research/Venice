package external.communication;

/**
 * Interface for remotecontrol
 * 
 * @author jeeickme
 */

public interface RPCRemoteInterface {
	
	public void play_pause();
        public void play_selection();
        public void set_selection(long beginTime, long endTime);
        public void toogleLoopMode();
	public void seek(long timestamp);

}
