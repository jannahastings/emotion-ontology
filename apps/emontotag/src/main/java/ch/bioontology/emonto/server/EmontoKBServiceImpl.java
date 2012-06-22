package ch.bioontology.emonto.server;

import ch.bioontology.emonto.client.EmontoKBService;
import ch.bioontology.emonto.shared.EmontoTerm;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class EmontoKBServiceImpl extends RemoteServiceServlet implements
		EmontoKBService {
	
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
