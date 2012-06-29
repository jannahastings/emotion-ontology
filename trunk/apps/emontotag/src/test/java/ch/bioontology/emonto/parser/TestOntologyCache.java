package ch.bioontology.emonto.parser;

import org.junit.Test;
import static org.junit.Assert.*;

import ch.bioontology.emonto.server.OntologyCache;
import ch.bioontology.emonto.shared.EmontoTerm;


public class TestOntologyCache {
	
	@Test
	public void testInitialize() {
		OntologyCache cache = OntologyCache.getInstance();
		assertTrue (cache != null);
		
		cache.initialize(true);  //test tagging only
		
		assertTrue( (cache.getAllEmontoTerms().size()) > 10);
		assertTrue( cache.getEmotionTerms().size() > 2);
		assertTrue( cache.getFeelingTerms().size() > 2);
		assertTrue( cache.getThoughtTerms().size() > 2);
		
		EmontoTerm appraisalTerm = cache.getEmontoTerm("MFOEM:000092");
		assertTrue (appraisalTerm != null);
		assertTrue (appraisalTerm.getLabel().equals("appraisal as avoidable consequences"));
		assertTrue (appraisalTerm.getTagLabel().equals("there are consequences but they are avoidable"));
		assertTrue (appraisalTerm.getDefinition() != null);
		
		
	}

}
