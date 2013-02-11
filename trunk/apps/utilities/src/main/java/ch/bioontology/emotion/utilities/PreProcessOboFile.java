package ch.bioontology.emotion.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;

/**
 * Pre-process an OBO file for use in the ontology enrichment tool
 * 
 * Execute with Parameters:
 * 1) obo file SOURCE
 * 2) obo file OUTPUT
 * 3) boolean carryDownInferredRelations
 * 4) boolean writeSeparateAnnotationFile
 * 5) root ontology id(s) to extract subtrees from (semicolon-separated)
 * 6) metadata properties to carry over to new ontology (semicolon-separated)
 * 7) [optional] relationship(s) to infer over (semicolon-separated) for annotation file and inherited inferences
 * 
 * @author tudose, hastings
 *
 */
public class PreProcessOboFile {

	public static void main(String args[]){

		if (args.length < 6 ) {
			System.err.println("Cannot proceed, wrong arguments. Required:\n" +
					" 1) file source, \n" +
					" 2) file output, \n" +
					" 3) boolean carryDownInferredRelations \n"+
					" 4) boolean writeSeparateAnnotationFile \n"+
					" 5) root ontology id(s) to extract subtrees from (semicolon-separated) \n"+ 
					" 6) metadata properties to carry over to new ontology (semicolon-separated) \n"+
					" 7) (optional) relationship(s) to carry inferences with and/or create separate annotations with (semicolon-separated)");
		}

		String sourceFile = args[0];
		String outputOboFile = args[1];
		boolean carryDownInferredRelations = Boolean.valueOf(args[2]);
		boolean writeSeparateAnnotationFile = Boolean.valueOf(args[3]);
		String rootIds = args[4];
		String metadatas = args[5];
		String relationships = ( args.length>6? args[6] : null);

		System.out.println("Got parameter sourceFile: "+sourceFile);
		System.out.println("Got parameter outputFile: "+outputOboFile);
		System.out.println("Got parameter carryDownInferredRelations: "+carryDownInferredRelations);
		System.out.println("Got parameter writeSeparateAnnotationFile: "+writeSeparateAnnotationFile);
		System.out.println("Got parameter rootIds: "+rootIds);
		System.out.println("Got parameter metadatas "+metadatas);
		System.out.println("Got parameter relationships: "+relationships);

		List<String> rootsList = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(rootIds, ";");
		while (tokenizer.hasMoreTokens()) {
			rootsList.add(tokenizer.nextToken());
		}

		List<String> relationList = new ArrayList<String>();
		if (relationships != null) {
			tokenizer = new StringTokenizer(relationships, ";");
			while (tokenizer.hasMoreTokens()) {
				relationList.add(tokenizer.nextToken());
			}		
		}

		List<String> metadataList = new ArrayList<String>();
		tokenizer = new StringTokenizer(metadatas, ";");
		while (tokenizer.hasMoreTokens()) {
			metadataList.add(tokenizer.nextToken());
		}		


		getTransitiveClosure(sourceFile, outputOboFile, carryDownInferredRelations, writeSeparateAnnotationFile,  
				rootsList, metadataList, relationList);

	}

	public static void getTransitiveClosure(String ontologyIRI, String newOntIRI,  
			boolean carryDownInferredRelations, boolean writeSeparateAnnotationFile, 
			List<String> chebiIDsForSubtrees,  
			List<String> metadatas, List<String> propertiesToInferUpon){
		/**
		 * This method loads an ontology, runs the reasoner and builds a new ontology with:
		 * 	- all subclassOf relations
		 * 	- for each class inherited properties are "re-assigned" directly
		 * 	- for the properties specified in the list, the class of the object property is followed up
		 * 	- all relations are restricted to classes belonging to the subtrees of the classes passed as parameters in chebiIDsForSubtrees; any other class is simply ignored
		 * Janna's description of the task: "compute the transitive closure of the has-role (and other, but mostly has-role) relationship over the is-a relationship." 
		 * 
		 *EXAMPLE
		 *Input: 
		 * 	Amphetamines hasRole CNSStimulant
		 * 	Mephedrome isA Amphetamines
		 *Result:
		 *  Amphetamines hasRole CNSStimulant
		 * 	Mephedrome isA Amphetamines
		 *	Mephedrome hasRole CNSStimulant
		 * 
		 */

		// Output file for ANNOTATIONS
		BufferedWriter cout = null; 
		try{
			if (writeSeparateAnnotationFile) {
				cout = new BufferedWriter(new FileWriter(newOntIRI.replace(".obo", ".txt")));
				cout.write("(species=ALL)(type=CHEBIROLES)(curator=ChEBI)\n");
				cout.flush();
			}

			ParserWrapper pw = new ParserWrapper();
			OWLGraphWrapper graph = pw.parseToOWLGraph(ontologyIRI);
			OWLOntology ont = graph.getSourceOntology();
			// Create the REASONER			
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
			graph.setReasoner(reasoner);

			// Classify the ontology.
			reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
			reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
			// Fresh empty ontology
			OWLOntology infOnt = man.createOntology(IRI.create(newOntIRI));

			long start = System.currentTimeMillis();
			Set<OWLClass> classSubSet = reasoner.getSubClasses(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(0)), false).getFlattened();
			classSubSet.add(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(0)));
						
			ArrayList<OWLProperty> propertiesOfInterest = new ArrayList<OWLProperty>();
			for (String iri: propertiesToInferUpon){
				propertiesOfInterest.add(graph.getOWLObjectProperty(iri));
			}

			for (int i=1; i < chebiIDsForSubtrees.size(); i++){
				classSubSet.addAll(reasoner.getSubClasses(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(i)),false).getFlattened());
				classSubSet.add(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(i)));
			}		
			
			// Get all objects in the graph and then test which are OWLClass. 
			Set<OWLObject> objects = graph.getAllOWLObjects();
			// We only want to follow isA edges, so we pass an empty set to the getAncestors method
			Set<OWLPropertyExpression> properties = new TreeSet<OWLPropertyExpression>();
			OWLClass current;
			Set<OWLObject> ancestors;
			OWLDataFactory factory = man.getOWLDataFactory();

			for (OWLObject obj: objects){
				// We only need classes
				if (obj.getClass().toString().equals(OWLClassImpl.class.toString()) && classSubSet.contains((OWLClass) obj)){
					current = (OWLClass) obj;

					if (carryDownInferredRelations) {
						ancestors = graph.getAncestors(current, properties);
						for (OWLObject anc: ancestors){
							// add inherited relations
							if ( anc.getClass().toString().equalsIgnoreCase(OWLObjectSomeValuesFromImpl.class.toString())){
								OWLObjectSomeValuesFromImpl exists = (OWLObjectSomeValuesFromImpl) anc;
								// test if the classes used in of these relations is also in the right subtree
								Set<OWLClass> classesInSignature = exists.getClassesInSignature();
								boolean inTree = true;
								for (OWLClass referencedClass : classesInSignature){
									if(!classSubSet.contains(referencedClass))
										inTree = false;
								}
								if (inTree && (propertiesOfInterest.contains(exists.getProperty()))){
									// Class is in the right subtree , so add the relation
									man.applyChange(new AddAxiom(infOnt, factory.getOWLSubClassOfAxiom(current, exists)));
									if (writeSeparateAnnotationFile) writeToAnnotationFile(cout, current, exists);

								}
							}
						}
					}
					// add subclassOf relations
					for (OWLAxiom x : ont.getReferencingAxioms(current)){
						Set<OWLClass> classesInSignature = x.getClassesInSignature();
						boolean inTree = true;
						for (OWLClass referencedClass : classesInSignature){
							if(!classSubSet.contains(referencedClass))
								inTree = false;
						}
						Set<OWLObjectProperty> propertiesInSignature = x.getObjectPropertiesInSignature();
						if (inTree && (propertiesOfInterest.containsAll(propertiesInSignature) || propertiesInSignature.size() == 0))
							man.applyChange(new AddAxiom(infOnt, x));
					}
					// get annotations from the source ontology
					Set<OWLAnnotation> annotations = current.getAnnotations(ont);
					for (OWLAnnotation ann : annotations){
						// only add specified annotations to the new ont
						for (String annoType : metadatas) {
							if (annoType.equals("ALL")
									|| ann.getProperty().toString().toLowerCase().contains(annoType.toLowerCase())
									|| ann.getValue().toString().toLowerCase().contains(annoType.toLowerCase())){
								man.applyChange(new AddAxiom(infOnt, factory.getOWLAnnotationAssertionAxiom(current.getIRI(), ann)));
							}
						}
					}
				}
			}
			System.out.println("Finished the ontology. \nWriting to file, please be patient...");
			// Save the new ontology
			if (newOntIRI.endsWith("obo")) {
				man.saveOntology(infOnt, new OBOOntologyFormat(), IRI.create(new File(newOntIRI+".temp").toURI()));
				System.out.println("Ontology saved. Need to do some post-processing corrections now.");
				postProcess(newOntIRI+".temp", newOntIRI);
			} else if (newOntIRI.endsWith("owl")) {
				man.saveOntology(infOnt, new RDFXMLOntologyFormat(), IRI.create(new File(newOntIRI).toURI()));
			} else {
				System.err.println("Cannot save ontology: unrecognised file extension: "+newOntIRI);
			}
			if (writeSeparateAnnotationFile) cout.close();
			System.out.println("Finished all " + (System.currentTimeMillis() - start) + " milliseconds. ");
			System.exit(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	

	public static void postProcess(String oboFile, String outputFile){
		/**
		 * We need to change some things in the file we created with getTransitiveClosure(...)
		 * - IDs are changed into CHEBI_123 and don't match the original notation any more (CHEBI:123)
		 * - The "is-a Thing" assertions must be deleted too
		 */
		try{
			BufferedReader cin = new BufferedReader (new FileReader(new File (oboFile)));
			BufferedWriter cout = new BufferedWriter (new FileWriter(outputFile));
			String line;
			while ((line = cin.readLine()) != null){
				if (!line.contains("is_a: Thing"))
					cout.write(line.replaceAll("CHEBI_", "CHEBI:") + "\n");
			}
			cout.flush();
			cout.close();
			cin.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeToAnnotationFile(BufferedWriter cout, OWLClass subjectClass, OWLObjectSomeValuesFrom axiom){
		try{
			Set<OWLClass> object = axiom.getClassesInSignature();
			if (object.size() > 1)
				System.out.println("__________________________________________________________");
			String objectID = "";
			for (OWLClass dirObj : object)
				objectID = dirObj.toStringID();
			objectID = objectID.replace("http://purl.obolibrary.org/obo/CHEBI_", "");
			String subjectID = subjectClass.toStringID();
			subjectID = subjectID.replace("http://purl.obolibrary.org/obo/CHEBI_", "CHEBI:");
			cout.write(subjectID+"="+objectID+"\n");
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
