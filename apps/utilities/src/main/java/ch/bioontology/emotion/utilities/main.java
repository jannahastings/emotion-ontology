package ch.bioontology.emotion.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream.GetField;
import java.util.*;

import org.coode.oppl.SubClassVariableScope;
import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.coode.parsers.IRISymbol;
import org.coode.parsers.common.SystemErrorEcho;
import org.semanticweb.elk.owlapi.ElkReasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.*;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectSomeValuesFromImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

public class main {

	public static void main(String args[]){
		// build & write obo file for chemical entioties only
		ArrayList<String> rootsList = new ArrayList<String>();
		ArrayList<String> relationList = new ArrayList<String>();
/*		rootsList.add("CHEBI:24431"); // chemical entity
		getTransitiveClosure("C:/Users/tudose/Documents/Docs/chebi.obo", "c:/ontologies/chebiInferred_chemEnt.obo", rootsList, relationList);		// write roles-only file
		rootsList = new ArrayList<String>();
		*/
		rootsList.add("CHEBI:50906"); // role
//		getTransitiveClosure("C:/Users/tudose/Documents/Docs/chebi.obo", "c:/ontologies/chebiInferred_roles.obo", rootsList, relationList);
		// File with both chemical entities, roles and has-role relation.
		rootsList.add("CHEBI:24431");// chemical entity
		relationList.add("http://purl.obolibrary.org/obo/chebi#has_role"); //has-role
		getTransitiveClosure("C:/Users/tudose/Documents/Docs/chebi.obo", "c:/ontologies/chebiInferred_chemEnt_roles.obo", rootsList, relationList);
	
	}
	
	
	public static void loadWithProtegeAPI(){
		
	}
	
	public static void getTransitiveClosure(String ontologyIRI, String newOntIRI, ArrayList<String> chebiIDsForSubtrees,
			ArrayList<String> propertiesToInfereUpon){
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
		try{
			ParserWrapper pw = new ParserWrapper();
			OWLGraphWrapper graph = pw.parseToOWLGraph(ontologyIRI);
			OWLOntology ont = graph.getSourceOntology();
			// Create the REASONER			
			OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
			graph.setReasoner(reasoner);
			BufferedWriter cout = new BufferedWriter(new FileWriter(newOntIRI.replace(".obo", ".txt")));
		    // Classify the ontology.
		    reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		    reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
		    // To generate an inferred ontology we use implementations of
		    // inferred axiom generators
		    List<InferredAxiomGenerator<? extends OWLAxiom>> gens = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
		    gens.add(new InferredSubClassAxiomGenerator());
		    gens.add(new InferredEquivalentClassAxiomGenerator());
		    // Put the inferred axioms into a fresh empty ontology.
		    OWLOntology infOnt = man.createOntology();
		    InferredOntologyGenerator iog = new InferredOntologyGenerator(reasoner, gens);
		    long start = System.currentTimeMillis();
		    Set<OWLClass> classSubSet = reasoner.getSubClasses(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(0)), false).getFlattened();
		    classSubSet.add(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(0)));
//		    Set<OWLClass> classSubSet = reasoner.getSubClasses(graph.getOWLClass("http://www.semanticweb.org/tudose/ontologies/2012/10/untitled-ontology-19#chemicalEntity"),
//		    		false).getFlattened();
//		    classSubSet.add(graph.getOWLClass("http://www.semanticweb.org/tudose/ontologies/2012/10/untitled-ontology-19#chemicalEntity"));
		    ArrayList<OWLProperty> propertiesOfInterest = new ArrayList<OWLProperty>();
		    for (String iri: propertiesToInfereUpon){
		    	propertiesOfInterest.add(graph.getOWLObjectProperty(iri));
		    }
		    
		    for (int i=1; i < chebiIDsForSubtrees.size(); i++){
		    	classSubSet.addAll(reasoner.getSubClasses(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(i)),false).getFlattened());
		    	classSubSet.add(graph.getOWLClassByIdentifier(chebiIDsForSubtrees.get(i)));
		    }		
		    // I get all objects in the graph and then test which are OWLClass. It's quite redundant but I didn't find the method to get all Classes direclty.
		    Set<OWLObject> objects = graph.getAllOWLObjects();
		    // We only want to follow isA edges, so we pass an empty set to the getAncestors method
    		Set<OWLPropertyExpression> properties = new TreeSet<OWLPropertyExpression>();
    		OWLClass current;
    		Set<OWLObject> ancestors;
		    OWLDataFactory factory = man.getOWLDataFactory();
		    long classesVisited = 0 ;		    
		    for (OWLObject obj: objects){
		    	// We only need classes
		    	if (obj.getClass().toString().equals(OWLClassImpl.class.toString()) && classSubSet.contains((OWLClass) obj)){
		    		current = (OWLClass) obj;
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
				    			writeToRolesFile(cout, current, exists);
				    			System.out.println("Add axiom: " + current + "  " + exists);
				    			// check if we want to follow the hierarchy of the object class
				    			// follow up the hierarchy of the object class above and add parents too
					    		for (OWLClass referencedClass : classesInSignature){
					    			Set<OWLObject> parents = graph.getAncestors(referencedClass);
					    			for (OWLObject parent: parents){
					    				if (classSubSet.contains(parent)){
					   						//replace the object with it's ancestor in the relation assertion
					   						OWLObjectSomeValuesFrom inferred = factory.getOWLObjectSomeValuesFrom(exists.getProperty(), (OWLClassExpression) parent);
					   						System.out.println("Add parents: " + current + "  " + inferred);
					   						man.applyChange(new AddAxiom(infOnt, factory.getOWLSubClassOfAxiom(current, inferred)));
					   						// write relation to the extra file Janna needed for roles
					   						writeToRolesFile(cout, current, inferred);
				    					}
				    				}
					    		}
				    		}
				    	}
				    }
			    	// add subclassOf relations
				    for (OWLAxiom x : ont.getReferencingAxioms(current)){
				    	Set<OWLClass> classesInSignature = x.getClassesInSignature();
				    	System.out.println("---" + classesInSignature);
				    	boolean inTree = true;
				    	for (OWLClass referencedClass : classesInSignature){
				    		if(!classSubSet.contains(referencedClass))
				    			inTree = false;
				    	}
				    	Set<OWLObjectProperty> propertiesInSignature = x.getObjectPropertiesInSignature();
				    	System.out.println("+++" + propertiesInSignature);
				    	if (inTree && (propertiesOfInterest.containsAll(propertiesInSignature) || propertiesInSignature.size() == 0))
				    		man.applyChange(new AddAxiom(infOnt, x));
				    	System.out.println("finished adding subclass of relation");
				    }
				    // get annotations from the source ontology
				    Set<OWLAnnotation> annotations = current.getAnnotations(ont);
				    for (OWLAnnotation ann : annotations){
				    	// only add labels to the new ont
				    	if (ann.getProperty().toString().equalsIgnoreCase("rdfs:label")){
				    		man.applyChange(new AddAxiom(infOnt, factory.getOWLAnnotationAssertionAxiom(current.getIRI(), ann)));
				    	}
				    	System.out.println(ann.getProperty());
				    }
		    	}
		    }
		    System.out.println("Finished the ontology. \nWriting to file, please be patient...");
		    // Save the new ontology
		    man.saveOntology(infOnt, new OBOOntologyFormat(), IRI.create(new File(newOntIRI+".temp").toURI()));
		    System.out.println("Ontology saved. Need to do some post-processing corrections now.");
		    postProcess(newOntIRI+".temp", newOntIRI);
		    cout.close();
		    System.out.println("Finished all " + (System.currentTimeMillis() - start) + " milliseconds. ");		   
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
			cout.close();
			cin.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void writeToRolesFile(BufferedWriter cout, OWLClass subjectClass, OWLObjectSomeValuesFrom axiom){
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
