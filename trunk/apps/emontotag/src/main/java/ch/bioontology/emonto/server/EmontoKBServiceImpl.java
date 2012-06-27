package ch.bioontology.emonto.server;

import ch.bioontology.emonto.shared.EmontoTerm;

public class EmontoKBServiceImpl  {
	
	public void initializeKB() {
		//TODO pass error handling back to client!
		if (!OntologyCache.getInstance().isInitialized())
			OntologyCache.getInstance().initialize();

	}

	public EmontoTerm[] getAllTerms() {
		initializeKB();
		
		if (!OntologyCache.getInstance().isInitialized()) {
			return null;
		} else {
			EmontoTerm[] result = OntologyCache.getInstance().getEmontoTerms().toArray(new EmontoTerm[] {});
			return result;
		}
	}

	public EmontoTerm[] getTerms(String[] symbols) {
		if (!OntologyCache.getInstance().isInitialized()) {
			return null;
		} else {
			EmontoTerm[] result = OntologyCache.getInstance().searchEmontoTerms(symbols).toArray(new EmontoTerm[] {});
			return result;
		}
	}

	
	
}
