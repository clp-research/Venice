import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
		SetupTests.class,
		venice.hub.VeniceHubTest.class,
		venice.hub.utils.TTETests.class,
		venice.hub.utils.TTEBufferTests.class,
		venice.hub.utils.TTEQueueTests.class,
		venice.hub.utils.TTEBufferedReaderTests.class,
		venice.hub.ConsoleControlTest.class
})

public class AllTests {}
