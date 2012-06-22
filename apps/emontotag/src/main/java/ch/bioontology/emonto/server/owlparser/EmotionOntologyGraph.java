package ch.bioontology.emonto.server.owlparser;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.OWLGraphWrapper;

public class EmotionOntologyGraph extends OWLGraphWrapper {

	private List<String> roots = new ArrayList<String>();
	
	public EmotionOntologyGraph(OWLOntology ontology)
			throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
				
		roots.add("MFOEM:000001"); // emotion occurrent
		

		
	}

	
	public List<String> getRoots() {
		return roots;
	}
	
}
