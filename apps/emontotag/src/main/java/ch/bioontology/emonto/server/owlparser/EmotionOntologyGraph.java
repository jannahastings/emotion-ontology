package ch.bioontology.emonto.server.owlparser;

import java.util.Arrays;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.OWLGraphWrapper;

public class EmotionOntologyGraph extends OWLGraphWrapper {

	public static final String ROOT_EMOTION = "MFOEM:000001";
	public static final String ROOT_FEELING = "MFOEM:000006";
	public static final String ROOT_THOUGHT = "MFOEM:000005";
	
	public EmotionOntologyGraph(OWLOntology ontology)
			throws UnknownOWLOntologyException, OWLOntologyCreationException {
		super(ontology);
				
	}

	
	public List<String> getRoots() {
		return Arrays.asList(new String[] {ROOT_EMOTION, ROOT_FEELING, ROOT_THOUGHT});
	}
	
}
