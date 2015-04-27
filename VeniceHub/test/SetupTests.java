
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

public class SetupTests {
	
	@Test
	public void setupTests() {
		BasicConfigurator.configure();
		Logger.getLogger("SetupTests").debug("setting up tests");
	}

}