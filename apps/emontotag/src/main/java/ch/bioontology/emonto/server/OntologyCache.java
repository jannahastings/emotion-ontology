package ch.bioontology.emonto.server;

import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;

import ch.bioontology.emonto.server.owlparser.EmotionOntologyGraph;
import ch.bioontology.emonto.server.owlparser.EmotionOntologyOWLParser;
import ch.bioontology.emonto.shared.EmontoTerm;

/**
 * In-memory store of all relevant emotion ontology data 
 * 
 * @author hastings
 *
 */
public class OntologyCache {
	
	/** Singleton */
	private static OntologyCache ms_instance = null;
	
	static {
		ms_instance = new OntologyCache();
	}
	
	public static OntologyCache getInstance() {
		return ms_instance;
	}
	
	private EmotionOntologyGraph emontoGraph;
	private Set<EmontoTerm> emontoTerms;
	private boolean initialized = false;
	
	private OntologyCache() {
		emontoTerms = new HashSet<EmontoTerm>();
	}
	
	public void initialize() {
		EmotionOntologyOWLParser parser = new EmotionOntologyOWLParser();
		parser.parseOntology();
		
		if (parser.getEmOntoGraph()!= null) {
			emontoGraph = parser.getEmOntoGraph();
			
			for (String root : emontoGraph.getRoots()) {
			
				OWLClass rootClass = emontoGraph.getOWLClassByIdentifier(root);
				Set<OWLObject> descendents = emontoGraph.getDescendantsReflexive(rootClass);
				System.out.println("emOntoGraph.getDescendants.Size: "+descendents.size());
				
				for (OWLObject term : descendents) {
					String id = emontoGraph.getIdentifier(term);
					String label = emontoGraph.getLabel(term);
					String definition = emontoGraph.getDef(term);
					EmontoTerm ontoTerm = new EmontoTerm(id, label, definition);
					emontoTerms.add(ontoTerm);
				}
			}
			
		}
		initialized = true;
	}

	public Set<EmontoTerm> getEmontoTerms() {
		return emontoTerms;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	
	/**
	 * The main search method for character-based searches of the ontology terminological knowledge 
	 * Currently uses id and name (label), but not definition
	 * Need to provide a separate search for definitions!
	 * 
	 * @param symbols
	 * @return
	 */
	public Set<EmontoTerm> searchEmontoTerms(String[] symbols) {
		//We need to search all the emonto terms
		//for hits from any of the search terms
		
		Set<EmontoTerm> result = new HashSet<EmontoTerm>();
		for (String symbol : symbols) {
			for (EmontoTerm term : getEmontoTerms()) {
				if (term.getId().contains(symbol)
					|| term.getLabel().contains(symbol) ) {
					result.add(term);
				}
			}
		}
		return result;
	}
}
