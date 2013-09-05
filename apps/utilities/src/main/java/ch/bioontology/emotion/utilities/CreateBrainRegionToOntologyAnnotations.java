package ch.bioontology.emotion.utilities;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import au.com.bytecode.opencsv.CSVReader;

public class CreateBrainRegionToOntologyAnnotations {
	private static final String EM_ID_PREFIX = "MFOEM:";
	private static final String GO_ID_PREFIX = "GO:";
	private static final String NBO_ID_PREFIX = "NBO:";
	private static final String MF_ID_PREFIX = "MF:";
	
	private static final int ID_LENGTH = 6;
	
	private Map<String,String> uberonNamesToIds = new HashMap<String, String>(); 
	private Map<String,List<String>> papersToAnnoIds = new HashMap<String,List<String>>();
	private Map<String,List<String>> annoIdsToOntologyIds = new HashMap<String,List<String>>();
	private Set<String> annotationFileRows = new HashSet<String>();
	private Map<String,String> replacementIds = new HashMap<String,String>();

	public void parseReplacementIdFile(String fileName) throws Exception {
		CSVReader reader = new CSVReader(new FileReader(new File(fileName)));

		String[] header = reader.readNext(); //discard header
		
		String[] line = reader.readNext();
		while (line != null) {
			String oldId = line[0];
			String newId = line[1];
			replacementIds.put(oldId, newId);
			line = reader.readNext();
		}
		
		reader.close();
		
	}
	
	public void parseUberonSubsetFile(String ontologyIRI) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(ontologyIRI);
		OWLOntology ont = graph.getSourceOntology();
		
		for (OWLObject obj: graph.getAllOWLObjects()){
			if (obj instanceof OWLClass) {
				String name = graph.getLabel(obj);
				String id = graph.getIdentifier(obj);
				
				uberonNamesToIds.put(name, id);
				
				for (String synonym : graph.getSynonymStrings(obj)) {
					uberonNamesToIds.put(synonym, id);
				}
			}
			
		}
		System.out.println("Loaded "+uberonNamesToIds.keySet().size()+" anatomical regions");
	}
	
	public void loadPapersAndOntologyTermsFromFile(String filename) throws Exception {
		CSVReader reader = new CSVReader(new FileReader(filename)); 
			
		List<String[]> lines = reader.readAll();
		reader.close();
		
		lines.remove(0); //header
		for (String[] line : lines) {
			
			String pubMedId = line[0];
			if (!papersToAnnoIds.containsKey(pubMedId)) {
				papersToAnnoIds.put(pubMedId, new ArrayList<String>());
			}
			for (int i=1; i<line.length; i++) {
				String val = line[i];
				if (!val.isEmpty()) {
					String ontologyId = EM_ID_PREFIX+zeropad(val,ID_LENGTH);
					papersToAnnoIds.get(pubMedId).add(ontologyId);
				}
			}
			System.out.println("Got "+papersToAnnoIds.get(pubMedId).size()+ " ontology annotations for pubmed "+pubMedId);
		}
	}
	
	//Reason over neuroanno.owl, in which the EM IDs that are parsed herein are classified. 
	//Extract the simple class hierarchy of the neuroanno classes. 
	//
	//need that class hierarchy to have the uberon annotations associated to it. 
	public void getClassificationParents(String emOntologyFile) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(emOntologyFile);
		OWLOntology ont = graph.getSourceOntology();
		OWLReasonerFactory reasonerFactory = new ReasonerFactory();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLReasoner reasoner = reasonerFactory.createReasoner(ont);
		graph.setReasoner(reasoner);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		
		for (String uberonId : papersToAnnoIds.keySet()) {
			for (String annoId : papersToAnnoIds.get(uberonId)) {
				OWLClass annoClass = graph.getOWLClassByIdentifier(annoId);
				System.out.println("Got annoClass: "+annoClass);
				if (annoClass == null) {
					System.out.println("Got no anno class for id "+annoId);
					continue;
				}
				NodeSet<OWLClass> parents = reasoner.getSuperClasses(annoClass, false);
				
				for (OWLClass parent : parents.getFlattened()) {
					String parentId = graph.getIdentifier(parent);
					System.out.println("Got parent: "+parent);
					System.out.println("Parent has ID: "+parentId);
					System.out.println("Parent has label: "+graph.getLabel(parent));
					if (shouldIncludeParent(parentId)) {
						if (!annoIdsToOntologyIds.containsKey(annoId)) {
							annoIdsToOntologyIds.put(annoId, new ArrayList<String>());
						}
						annoIdsToOntologyIds.get(annoId).add(graph.getIdentifier(parent));
					}
				}
				if (annoIdsToOntologyIds.containsKey(annoId)) 
					System.out.println("Got "+annoIdsToOntologyIds.get(annoId).size()+" parent classes for annotation id "+annoId);
			}
		}
	}
	
	private boolean shouldIncludeParent(String identifier) {
		boolean result = false;
		
		if (identifier.startsWith(EM_ID_PREFIX)) {
			String numericPart = identifier.substring(identifier.indexOf(":")+1);
			if (Integer.valueOf(numericPart) < 1000) {
				result = true;
			}
		} else if (identifier.startsWith(NBO_ID_PREFIX)
				|| identifier.startsWith(GO_ID_PREFIX)
				|| identifier.startsWith(MF_ID_PREFIX)) {
			result = true;
		}
		
		return result; 
	}
	
	
	public void lookupCandidateAnnotations() throws Exception {
		if (uberonNamesToIds.size()==0 ||papersToAnnoIds.size()==0)
			throw new RuntimeException("Empty lists, cannot lookup candidate annotations. Load data from files first. ");
		
		//first get abstract text for paper. 
		for (String pubMedId : papersToAnnoIds.keySet()) {
			
			EFetchPubmedServiceStub service = new EFetchPubmedServiceStub();
			EFetchPubmedServiceStub.EFetchRequest req = new EFetchPubmedServiceStub.EFetchRequest();

			req.setId(pubMedId);

			EFetchPubmedServiceStub.EFetchResult res = service.run_eFetch(req);
			
			String title = "", abstractText = "";

			if (res.getPubmedArticleSet().getPubmedArticleSetChoice().length>0) {
				if (res.getPubmedArticleSet().getPubmedArticleSetChoice()[0].getPubmedArticle()!= null) {
					PubmedArticleType article = res.getPubmedArticleSet().getPubmedArticleSetChoice()[0].getPubmedArticle();

					title = article.getMedlineCitation().getArticle().getArticleTitle().getString();

					if (article.getMedlineCitation().getArticle().getAbstract()!=null &&
							article.getMedlineCitation().getArticle().getAbstract().getAbstractText().length>0) {
						abstractText = article.getMedlineCitation().getArticle().getAbstract().getAbstractText()[0].getString();
					}

				} 
			}
		
			//check against all the Uberon labels if the abstract text contains them. 
			for (String name : uberonNamesToIds.keySet()) {

				if (title.contains(name)
						|| abstractText.contains(name)) {
					System.out.println("Adding annotation for pmid "+pubMedId+" and region "+name);
					
					for (String ontologyId : papersToAnnoIds.get(pubMedId)) {
						if (annoIdsToOntologyIds.containsKey(ontologyId)) {
							for (String parentId : annoIdsToOntologyIds.get(ontologyId)) {
								if (replacementIds.containsKey(parentId)) {
									parentId = replacementIds.get(parentId);
								}
								parentId = parentId.substring(parentId.indexOf(":")+1);
								annotationFileRows.add(uberonNamesToIds.get(name)+" = "+parentId);
							}
						}
					}
				}
				
			}

		}
	}
	
	
	public void writeToOutputFile(String outputFileName) throws Exception {
		if (annotationFileRows.size()==0 ) {
			System.err.println("Cannot proceed. Nothing to write to file. ");
			return;
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));
		
		writer.write("(species=ALL) (type=UBEREMO) (curator=JH)\n");
		
		for (String line : annotationFileRows) {
			writer.write(line+"\n");
		}
		writer.flush();
		writer.close();
	}
	
	private String zeropad(String input, int length) {
		StringBuffer result = new StringBuffer(input);
		while (result.length()<length) {
			result.insert(0,"0");
		}
		return result.toString();
	}
	
	
	public static final void main(String args[]) throws Exception {
		CreateBrainRegionToOntologyAnnotations annoCreator = new CreateBrainRegionToOntologyAnnotations();
		annoCreator.parseReplacementIdFile("output/MFOEM-merge-IDs.csv");
		annoCreator.parseUberonSubsetFile("output/uberon-subset.owl");
		annoCreator.loadPapersAndOntologyTermsFromFile("input/paper-annotations.csv");
		annoCreator.getClassificationParents("C:/Work/Ontologies/mfo-and-emo/emotion-ontology/ontology/internal/neuroanno.owl");
		annoCreator.lookupCandidateAnnotations();
		annoCreator.writeToOutputFile("output/uberemo.anno");
	}

	
}
