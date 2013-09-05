package ch.bioontology.emotion.utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import au.com.bytecode.opencsv.CSVWriter;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import owltools.io.ParserWrapper.OWLGraphWrapperNameProvider;

public class CreateSingleOboFile {

	private String owlIRI; 
	private Map<String,String> oldIdToNewId = new HashMap<String, String>();
	private Map<String,Integer> prefixToAdditionValue = new HashMap<String,Integer>();
	private String replacementPrefix = "MF";

	public CreateSingleOboFile(String owlIRI) {
		this.owlIRI = owlIRI;

		prefixToAdditionValue.put("MF", 0);
		prefixToAdditionValue.put("MFOEM", 100000);
		prefixToAdditionValue.put("GO", 200000);
		prefixToAdditionValue.put("NBO", 300000);
		prefixToAdditionValue.put("OGMS", 400000);
		//		prefixToAdditionValue.put("BFO", 500000);
		//		prefixToAdditionValue.put("IAO", 600000);

	}

	public void mergeOWLFileImportsToObo(String outputFileName) throws Exception {
		if (owlIRI == null) {
			throw new NullPointerException("owlIRI is null");
		}
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLDataFactory factory = OWLManager.getOWLDataFactory();

		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(owlIRI);

		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		OWLReasoner reasoner = reasonerFactory.createReasoner(graph.getSourceOntology());
		graph.setReasoner(reasoner);

		// Classify the ontology.
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

		OWLOntology mergedOntology = man.createOntology(new OWLOntologyID(IRI.create(outputFileName)));

		for (OWLOntology ont : graph.getAllOntologies()) {
			if (!ont.getOntologyID().getOntologyIRI().toString().toLowerCase().contains("iao")
					&& !ont.getOntologyID().getOntologyIRI().toString().toLowerCase().contains("bfo")
					&& !ont.getOntologyID().getOntologyIRI().toString().toLowerCase().contains("ontology-metadata")) {

				for (OWLAxiom ax : ont.getAxioms()) {
					System.out.println("Got axiom: "+ax+" in ontology: "+ont.getOntologyID().getOntologyIRI()) ;

					processOWLAxiomForConversion(graph, ax, mergedOntology, factory, man);

				}
			}
		}
		OWLGraphWrapper graphMerged = new OWLGraphWrapper(mergedOntology);

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

		//Also save OWL
		String outputOwlFileName = outputFileName.substring(0,outputFileName.lastIndexOf(".")+1)+"owl";
		pw.saveOWL(mergedOntology, new File(outputOwlFileName).toURI().toString(), graphMerged);

		System.out.println("Finished saving to "+outputFileName+" and "+outputOwlFileName);

		//TODO: save the ID replacements to use them in the reasoner-annotation-creation step


	}

	/**
	 * Detect classes, overwrite identifiers with new value and save the results in the new merged ontology
	 * @param graph
	 * @param ax
	 */
	private void processOWLAxiomForConversion(OWLGraphWrapper graph, OWLAxiom ax, 
			OWLOntology merged, OWLDataFactory factory, OWLOntologyManager manager) {
		if (ax instanceof OWLSubClassOfAxiom) {
			OWLSubClassOfAxiom s = (OWLSubClassOfAxiom) ax;

			OWLClassExpression clsSub = s.getSubClass();
			OWLClassExpression clsSuper = s.getSuperClass();

			String idSub = graph.getIdentifier(clsSub);
			String idSuper = graph.getIdentifier(clsSuper);

			if (idSub == null || idSuper == null) {
				System.out.println("Skipping axiom with null identifiers.");
				return; 
			}

			if ((idSub.startsWith("BFO") || idSub.startsWith("IAO")) && 
					(idSuper.startsWith("BFO") || idSuper.startsWith("IAO"))
					) {
				System.out.println("Skipping axiom with BFO/IAO identifiers.");
				return; 
			}

			//still have to check if ONE of them does, then the OTHER should be asserted in a Declaration, I think
			if (idSub.startsWith("BFO") || idSub.startsWith("IAO")) {
				String newIdSuper = getNewIdIfNeededAndSave(idSuper);

				IRI newIRI = graph.getIRIByIdentifier(newIdSuper);
				OWLClass newCls = factory.getOWLClass(newIRI);

				OWLDeclarationAxiom newD = factory.getOWLDeclarationAxiom(newCls);
				manager.applyChange(new AddAxiom(merged, newD));
				return;
			}

			if (idSuper.startsWith("BFO") || idSuper.startsWith("IAO")) {
				String newIdSub = getNewIdIfNeededAndSave(idSub);

				IRI newIRI = graph.getIRIByIdentifier(newIdSub);
				OWLClass newCls = factory.getOWLClass(newIRI);

				OWLDeclarationAxiom newD = factory.getOWLDeclarationAxiom(newCls);
				manager.applyChange(new AddAxiom(merged, newD));

				return; 
			}			

			String newIdSub = getNewIdIfNeededAndSave(idSub);
			String newIdSuper = getNewIdIfNeededAndSave(idSuper);

			IRI newClsSubIRI = graph.getIRIByIdentifier(newIdSub);
			IRI newClsSuperIRI = graph.getIRIByIdentifier(newIdSuper);

			OWLClass newClsSub = factory.getOWLClass(newClsSubIRI);
			OWLClass newClsSuper = factory.getOWLClass(newClsSuperIRI);

			OWLSubClassOfAxiom newS = factory.getOWLSubClassOfAxiom(newClsSub, newClsSuper);
			manager.applyChange(new AddAxiom(merged, newS));

		} else if (ax instanceof OWLDeclarationAxiom) {
			OWLDeclarationAxiom d = (OWLDeclarationAxiom) ax;

			OWLEntity declaredEntity = d.getEntity();
			if (declaredEntity instanceof OWLClass) {
				OWLClass declaredClass = (OWLClass) declaredEntity;
				String id = graph.getIdentifier(declaredClass);

				if (id == null ) {
					System.out.println("Skipping axiom with null identifiers.");
					return; 
				}
				if (id.startsWith("BFO") || id.startsWith("IAO")) {
					System.out.println("Skipping axiom with BFO/IAO identifiers.");
					return; 
				}				

				String newId = getNewIdIfNeededAndSave(id);

				IRI newIRI = graph.getIRIByIdentifier(newId);
				OWLClass newCls = factory.getOWLClass(newIRI);

				OWLDeclarationAxiom newD = factory.getOWLDeclarationAxiom(newCls);
				manager.applyChange(new AddAxiom(merged, newD));

			} //ignore other declarations, for now

		} else if (ax instanceof OWLAnnotationAssertionAxiom) {
			OWLAnnotationAssertionAxiom a = (OWLAnnotationAssertionAxiom) ax;

			OWLAnnotationSubject annotSubj = a.getSubject();
			OWLAnnotationProperty prop = a.getProperty();
			if (prop.toString().contains("inSubset")) {
				System.out.println("Skipping inSubset annotations");
				return;
			}

			String id = graph.getIdentifier(annotSubj);

			System.out.println("Got identifier for annotation subject: "+id);

			if (id == null) {
				System.out.println("Skipping axiom with null identifiers.");
				return; 
			}
			if (!id.contains(":")) {
				System.out.println("Skipping badly-formed id (not OBO compliant).");
				return; 
			}
			if (id.startsWith("BFO") || id.startsWith("IAO") || id.startsWith("RO")) {
				System.out.println("Skipping axiom with BFO/IAO/RO identifiers.");
				return; 
			}

			String newId = getNewIdIfNeededAndSave(id);

			IRI newIRI = graph.getIRIByIdentifier(newId);

			System.out.println("Class: "+newId+" getting annotation on property "+a.getProperty()+" to value: "+a.getValue());
			OWLAnnotationAssertionAxiom newA = factory.getOWLAnnotationAssertionAxiom(newIRI, factory.getOWLAnnotation(a.getProperty(), a.getValue()));
			manager.applyChange(new AddAxiom(merged, newA));

		}


	}

	private String getNewIdIfNeededAndSave(String oldId) {
		if (oldIdToNewId.containsKey(oldId)) {
			return oldIdToNewId.get(oldId);
		}

		System.out.println("Getting new ID for "+oldId);
		String prefixPart = oldId.substring(0, oldId.indexOf(":"));
		String numericPart = oldId.substring(oldId.indexOf(":")+1);
		Integer actualNumber = Integer.valueOf(numericPart);
		Integer addToNumber = prefixToAdditionValue.get(prefixPart);
		Integer newIdNumber = actualNumber + addToNumber;
		StringBuffer newIdNumString = new StringBuffer(String.valueOf(newIdNumber));
		int countDigits = newIdNumString.length();
		int neededZeros = 7-countDigits;
		char[] zeros = new char[neededZeros];
		Arrays.fill(zeros, '0');
		String fullIdNumber = new String(zeros)+newIdNumString;
		String newId = replacementPrefix+":"+fullIdNumber;

		oldIdToNewId.put(oldId, newId);
		System.out.println("Got new ID: "+newId+" for old ID: "+oldId);
		return newId;
	}


	public void writeReplacementIdsToFile(String fileName) throws Exception {
		CSVWriter writer = new CSVWriter(new FileWriter(new File(fileName)));
		
		writer.writeNext(new String[] {"OLD ID","NEW ID"});
		
		for (String oldId : oldIdToNewId.keySet() ) {
			writer.writeNext(new String[] {oldId, oldIdToNewId.get(oldId)});
		}
		
		writer.close();
	}
	
	public static final void main(String[] args)  {
		try {
			CreateSingleOboFile singleOboFiler = new CreateSingleOboFile("http://purl.obolibrary.org/obo/MFOEM.owl");

			singleOboFiler.mergeOWLFileImportsToObo("C:/Work/Source/Java/bioontology-emotion/output/MFOEM-merged.obo");
			
			singleOboFiler.writeReplacementIdsToFile("C:/Work/Source/Java/bioontology-emotion/output/MFOEM-merge-IDs.csv");

			//		CreateSingleOboFile singleOboFiler = new CreateSingleOboFile("http://purl.obolibrary.org/obo/MFOEM/internal/neuroanno.owl");
			//		singleOboFiler.mergeOWLFileImportsToObo("C:/Work/Source/Java/bioontology-emotion/output/EM.obo");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

}
