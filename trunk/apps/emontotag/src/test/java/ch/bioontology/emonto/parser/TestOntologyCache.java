package ch.bioontology.emonto.parser;

import org.junit.Test;
import static org.junit.Assert.*;

import ch.bioontology.emonto.server.OntologyCache;


public class TestOntologyCache {
	
	@Test
	public void testInitialize() {
		OntologyCache cache = OntologyCache.getInstance();
		assertTrue (cache != null);
		
//		cache.initialize();
		
//		assertTrue( (cache.getEmontoTerms().size()) >= 10);
	}

}
