package external.communication;

public interface RPCInterface {
	
	public void play();
	public void stop();
	public void seek(long timestamp);
        public void pause();

}
