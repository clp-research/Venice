package venice.lib.parser;

import static org.junit.Assert.*;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import venice.lib.networkRSB.NamedPair;
import venice.lib.networkRSB.NamedPairList;

public class NamedPairListTest {
	private static Logger logger;
	
	@BeforeClass
	public static void beforeClass(){
		logger = Logger.getLogger(NamedPairListTest.class);
	}
	
	@Test
	public void testFindNamedPair(){
		NamedPairList npl = new NamedPairList();
		
		String sourceName = "a.random.source";
		String targetName = "a.random.target";
		
		NamedPair np = new NamedPair();
		np.setSourceName(sourceName);
		np.setTargetName(targetName);
		npl.add(np);
		
		assertNotNull(npl.findNamedPairForTarget(targetName));
		assertEquals(targetName, npl.findNamedPairForTarget(targetName).getTargetName());
		
		assertNotNull(npl.findNamedPairForSource(sourceName));
		assertEquals(sourceName, npl.findNamedPairForSource(sourceName).getSourceName());
	}
}
