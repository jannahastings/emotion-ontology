package ch.bioontology.emotion.utilities;

import java.io.File;
import java.io.FileWriter;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphEdge;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLQuantifiedProperty;
import owltools.graph.OWLQuantifiedProperty.Quantifier;
import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;
import owltools.reasoner.ExpressionMaterializingReasoner;
import owltools.reasoner.GraphReasonerFactory;


public class PrepareNeuroAnnoOntoFile {
	private FileWriter ontologyFileOut;
	private static OWLQuantifiedProperty subClassOf = new OWLQuantifiedProperty(Quantifier.SUBCLASS_OF);

	public void openOutputFile() throws Exception {
		ontologyFileOut = new FileWriter(new File("output/neuroanno-ontology.on"));
		ontologyFileOut.write("(curator=JH)(type=neuroanno)\n");
	}

	public void parseOntologyFileAndComputeInheritance() throws Exception {
		ParserWrapper parser = new ParserWrapper();

		OWLOntology ontology = parser.parseOWL("C:/Work/Ontologies/mfo-and-emo/emotion-ontology/ontology/internal/neuroanno.owl");

		OWLGraphWrapper graph = new OWLGraphWrapper(ontology);
		Mooncat mooncat = new Mooncat(graph);
		mooncat.mergeOntologies();

		GraphReasonerFactory reasonerFactory = new GraphReasonerFactory();

		ExpressionMaterializingReasoner reasoner 
		= new ExpressionMaterializingReasoner(ontology);
		reasoner.setWrappedReasoner(reasonerFactory.createReasoner(ontology));
		reasoner.materializeExpressions();

		//		parser.saveOWL(ontology, new OBOOntologyFormat(), "C:/Work/Source/Java/chebi-biology-enrichment/output/neuro-plus-inferences.obo", graph);

		OWLObject root = graph.getOWLObjectByIdentifier("MF:0000020");
		Set<OWLObject> visited = new HashSet<OWLObject>();

		recursivelyOutputGraph(reasoner, graph, visited, root);

	}


	private void recursivelyOutputGraph(OWLReasoner reasoner, OWLGraphWrapper graph, Set<OWLObject> visited, OWLObject current) throws Exception {
		if (!visited.contains(current)) {
			System.out.println("Processing: "+graph.getIdentifier(current)+"("+graph.getLabel(current)+")");
			visited.add(current);
			
			ontologyFileOut.write(graph.getIdentifier(current)+" = "+graph.getLabel(current)+" [");
			boolean first = true; 

			//parents
			
			HashSet<OWLObject> outgoingSeen = new HashSet<OWLObject>();
			for (OWLGraphEdge e : graph.getOutgoingEdges(current)) {
				System.out.println("o");
				if (!outgoingSeen.contains(e.getSource()) && e.isSourceNamedObject() && e.getSingleQuantifiedProperty().equals(subClassOf)) {
					outgoingSeen.add(e.getSource());
					
					ontologyFileOut.write((first?"isa: ":"")+graph.getIdentifier(e.getSource())+" ");

					first = false;
					
				}
			}

			ontologyFileOut.write("]\n");
			ontologyFileOut.flush();

			//children
			HashSet<OWLObject> incomingSeen = new HashSet<OWLObject>();
			for (OWLGraphEdge e : graph.getIncomingEdges(current)) {
				System.out.println("i");
				if (!incomingSeen.contains(e.getSource())&& e.isSourceNamedObject() && e.getSingleQuantifiedProperty().equals(subClassOf)) {
					incomingSeen.add(e.getSource());
					recursivelyOutputGraph(reasoner, graph, visited, e.getSource());
				}
			}


		}


	}

	public void closeOutputFile() throws Exception {
		ontologyFileOut.flush();
		ontologyFileOut.close();
	}

	public static final void main(String[] args) throws Exception {
		PrepareNeuroAnnoOntoFile preparer = new PrepareNeuroAnnoOntoFile();
		preparer.openOutputFile();
		preparer.parseOntologyFileAndComputeInheritance();
		preparer.closeOutputFile();
	}
}
