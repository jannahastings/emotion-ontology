package ch.bioontology.emotion.utilities;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.obolibrary.macro.ManchesterSyntaxTool;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;

public class CreateSubsetOntologyFile {

	public void createSubset(String ontologyIRI, String newOntIRI, String rootExpression) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(ontologyIRI);
		OWLOntology ont = graph.getSourceOntology();
		// Create the REASONER			
		OWLReasonerFactory reasonerFactory = new ReasonerFactory();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		ManchesterSyntaxTool parser = null;	
		OWLClassExpression ce = null;

		try {
			parser = new ManchesterSyntaxTool(ont);
			ce = parser.parseManchesterExpression(rootExpression);
		} catch(ParserException e){
			String errMessage = "There was a problem parsing input "+rootExpression;
			throw new Exception(errMessage);
		} finally {
			// always dispose parser to avoid a memory leak
			if (parser != null) {
				parser.dispose();
			}
		}

		System.out.println("Before classify and execute query");
		
		//Execute query		
		OWLDataFactory f = man.getOWLDataFactory();
		OWLClass qc = f.getOWLClass(IRI.create("http://owltools.org/Q"));
		OWLEquivalentClassesAxiom ax = f.getOWLEquivalentClassesAxiom(ce, qc);
		man.addAxiom(ont, ax);

		OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
		graph.setReasoner(reasoner);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		
		Set<OWLClass> subset = new HashSet<OWLClass>();
		NodeSet<OWLClass> node = reasoner.getSubClasses(qc, false);
		if (node != null) {
			Set<OWLClass> classes = node.getFlattened();
			for (OWLClass owlClass : classes) {
				if (!owlClass.isBottomEntity() && !owlClass.isTopEntity()) {
					if (reasoner.isSatisfiable(owlClass))
						System.out.println("Adding "+owlClass+" to subset.");
						subset.add(owlClass);
				}
			}
		}
		
		System.out.println("Before extract classes");
		
		OWLOntology infOnt = man.createOntology(IRI.create(newOntIRI));
		
		OWLClass current; int i=0;
		for (OWLObject obj: graph.getAllOWLObjects()){
			System.out.println(i++);
			if (obj instanceof OWLClass) {
				System.out.println("Class: "+((OWLClass)obj).getIRI());
			}
			if (subset.contains(obj)){
				
				current = (OWLClass) obj;
				
				//All axioms relating to classes in the subset to be added to the new ontology
				for (OWLAxiom x : ont.getReferencingAxioms(current)){
					if (subset.containsAll(x.getClassesInSignature())) {
						man.addAxiom(infOnt, x); 
					}
				}
				Set<OWLAnnotation> annotations = current.getAnnotations(ont);
				for (OWLAnnotation ann : annotations){
					man.applyChange(new AddAxiom(infOnt, f.getOWLAnnotationAssertionAxiom(current.getIRI(), ann)));
				}
			}
		}
	
		// Save the new ontology
		if (newOntIRI.endsWith("obo")) {
			man.saveOntology(infOnt, new OBOOntologyFormat(), IRI.create(new File(newOntIRI+".temp").toURI()));
		} else if (newOntIRI.endsWith("owl")) {
			man.saveOntology(infOnt, new RDFXMLOntologyFormat(), IRI.create(new File(newOntIRI).toURI()));
		} else {
			System.err.println("Cannot save ontology: unrecognised file extension: "+newOntIRI);
		}

		System.out.println("Ontology saved. Complete.");
		System.exit(0);
	}


	public static final void main(String[] args) throws Exception {
		String brain = "http://purl.obolibrary.org/obo/UBERON_0000955";
		String uberon = "input/uberon-basic.owl";
		String output = "output/uberon-subset.owl";
		
		CreateSubsetOntologyFile subsetCreator  = new CreateSubsetOntologyFile();
		
		subsetCreator.createSubset(uberon, output, brain+" or ( http://purl.obolibrary.org/obo/BFO_0000050 some "+brain+" )");
		
		
	}
}


