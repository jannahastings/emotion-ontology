package ch.bioontology.emonto.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
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
	private Map<String,Set<EmontoTerm>> emontoTerms; //key root id
	private Map<String, EmontoTerm> allEmontoTerms; //key id
	
	private boolean initialized = false;
	
	private OntologyCache() {
		emontoTerms = new HashMap<String,Set<EmontoTerm>>();
		allEmontoTerms = new HashMap<String,EmontoTerm>();
	}
	
	public void initialize(boolean taggingOnly) {
		EmotionOntologyOWLParser parser = new EmotionOntologyOWLParser();
		parser.parseOntology();
		
		if (parser.getEmOntoGraph()!= null) {
			emontoGraph = parser.getEmOntoGraph();
			OWLAnnotationProperty tagLabelProperty = emontoGraph.getDataFactory().getOWLAnnotationProperty(IRI.create("http://purl.obolibrary.org/obo/MFOEM_000165"));
			if (tagLabelProperty == null) {
				System.out.println("Found null tagLabelProperty");
			}
			
			for (String root : emontoGraph.getRoots()) {
			    Set<EmontoTerm> termsForRoot = new HashSet<EmontoTerm>();
			    
				OWLClass rootClass = emontoGraph.getOWLClassByIdentifier(root);
				Set<OWLObject> descendents = emontoGraph.getDescendantsReflexive(rootClass);
				System.out.println("emOntoGraph.getDescendants.Size: "+descendents.size());
				
				for (OWLObject term : descendents) {
					String id = emontoGraph.getIdentifier(term);
					String label = emontoGraph.getLabel(term);
					String definition = emontoGraph.getDef(term);
					String tagLabel = emontoGraph.getAnnotationValue(term, tagLabelProperty);
					if (taggingOnly && tagLabel == null) continue; //skip non-tagging terms if tagging only is specified to initialize
					EmontoTerm ontoTerm = new EmontoTerm(id, label, definition, tagLabel);
					termsForRoot.add(ontoTerm);
					allEmontoTerms.put(id,ontoTerm);
				}
				emontoTerms.put(root,termsForRoot);
			}
			
		}
		initialized = true;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	
	public Collection<EmontoTerm> getAllEmontoTerms() {
		return allEmontoTerms.values();
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
			for (EmontoTerm term : getAllEmontoTerms()) {
				if (term.getId().contains(symbol)
					|| term.getLabel().contains(symbol) ) {
					result.add(term);
				}
			}
		}
		return result;
	}
	
	public Set<EmontoTerm> getEmotionTerms() {
		return emontoTerms.get(EmotionOntologyGraph.ROOT_EMOTION);
	}
	
	public Set<EmontoTerm> getFeelingTerms() {
		return emontoTerms.get(EmotionOntologyGraph.ROOT_FEELING);
	}
	
	public Set<EmontoTerm> getThoughtTerms() {
		return emontoTerms.get(EmotionOntologyGraph.ROOT_THOUGHT);
	}
	
	public EmontoTerm getEmontoTerm(String id) {
		return allEmontoTerms.get(id);
	}
	
}
