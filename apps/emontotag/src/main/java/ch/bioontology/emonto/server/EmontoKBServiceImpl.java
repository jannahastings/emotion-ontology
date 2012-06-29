package ch.bioontology.emonto.server;

import ch.bioontology.emonto.shared.EmontoTerm;

public class EmontoKBServiceImpl  {
	private boolean tagOnly = true;
	
	public void initializeKB() {
		//TODO pass error handling back to client!
		if (!OntologyCache.getInstance().isInitialized())
			OntologyCache.getInstance().initialize(tagOnly);

	}

	public EmontoTerm[] getAllTerms() {
		
		if (!OntologyCache.getInstance().isInitialized()) {
			return null;
		} else {
			EmontoTerm[] result = OntologyCache.getInstance().getAllEmontoTerms().toArray(new EmontoTerm[] {});
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
