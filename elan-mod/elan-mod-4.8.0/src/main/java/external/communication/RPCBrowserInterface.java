package external.communication;

/**
 * Interface for the communication with IR playbacktool
 * 
 * @author jeeickme
 */

public interface RPCBrowserInterface {
	
  public void play();
  public void stop();
  public void seek(long timestamp);
  public void pause();
  public void quit();

}
