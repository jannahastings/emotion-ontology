package ch.bioontology.emonto.parser;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ch.bioontology.emonto.server.EmontoKBServiceImpl;
import ch.bioontology.emonto.shared.EmontoTerm;


public class TestEmontoKBService {

	@Test
	public void testInitialize() {
		EmontoKBServiceImpl service = new EmontoKBServiceImpl();
		service.initializeKB();
		
		EmontoTerm[] allTerms = service.getAllTerms();
		assertTrue(allTerms.length>10);
		
		EmontoTerm[] allEmTerms = service.getTerms(new String[] {"em"});
		
		assertTrue(allTerms.length>0);
	}
	
}
