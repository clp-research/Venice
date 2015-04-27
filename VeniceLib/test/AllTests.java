import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ 
		SetupTests.class,
		venice.lib.parser.XIOParserTest.class,
		venice.lib.parser.NamedPairListTest.class,
		venice.lib.parser.SensorFileReaderTest.class,
		venice.lib.parser.LUTablesTest.class,
		venice.lib.networkIIO.IIONamespaceBuilderTest.class,
		venice.lib.networkRSB.RSBNamespaceBuilderTest.class,
		venice.lib.AbstractSlotTest.class
	})


public class AllTests {

}