package ch.bioontology.emotion.utilities;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.io.ParserWrapper.OWLGraphWrapperNameProvider;

public class CreateSingleOboFile {
	
	private String owlIRI; 
	
	public CreateSingleOboFile(String owlIRI) {
		this.owlIRI = owlIRI;
	}
	
	public void mergeOWLFileImportsToObo(String outputFileName) throws Exception {
		if (owlIRI == null) {
			throw new NullPointerException("owlIRI is null");
		}
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(owlIRI);
		
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(graph.getSourceOntology());
		graph.setReasoner(reasoner);

		// Classify the ontology.
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
		
		OWLOntology mergedOntology = man.createOntology(new OWLOntologyID(IRI.create(outputFileName)));
		//Add all axioms from all source ontologies into one single merged ontology
		for (OWLOntology ont : graph.getAllOntologies()) {
			System.out.println("Got OWL ontology: "+ont.getOntologyID().getOntologyIRI());
			
			man.addAxioms(mergedOntology, ont.getAxioms());
		}
		OWLGraphWrapper graphMerged = new OWLGraphWrapper(mergedOntology);
		OWLGraphWrapperNameProvider nameProvider = new OWLGraphWrapperNameProvider(graphMerged);
		
		FileOutputStream bOut = new FileOutputStream(outputFileName);
		
		Owl2Obo bridge = new Owl2Obo();
		bridge.setDiscardUntranslatable(true);
		OBODoc doc;
		BufferedWriter bw = null;
		try {
			doc = bridge.convert(mergedOntology);
			OBOFormatWriter oboWriter = new OBOFormatWriter();
			oboWriter.setCheckStructure(false);
			
			bw = new BufferedWriter(new OutputStreamWriter(bOut));
			oboWriter.write(doc, bw, new OWLGraphWrapperNameProvider(graphMerged));
			bw.flush();
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		pw.saveOWL(mergedOntology, new OBOOntologyFormat(), outputFileName, graphMerged);
		

	}
	
	public static final void main(String[] args) throws Exception {
//		CreateSingleOboFile singleOboFiler = new CreateSingleOboFile("http://purl.obolibrary.org/obo/MFOEM.owl");
		
//		singleOboFiler.mergeOWLFileImportsToObo("C:/Work/Source/Java/bioontology-emotion/output/MFOEM-merged.obo");
		
		CreateSingleOboFile singleOboFiler = new CreateSingleOboFile("http://purl.obolibrary.org/obo/MFOEM/internal/neuroanno.owl");
		singleOboFiler.mergeOWLFileImportsToObo("C:/Work/Source/Java/bioontology-emotion/output/EM.obo");
	}

}
