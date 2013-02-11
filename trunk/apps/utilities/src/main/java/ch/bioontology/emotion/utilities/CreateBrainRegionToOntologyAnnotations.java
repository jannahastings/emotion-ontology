package ch.bioontology.emotion.utilities;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import au.com.bytecode.opencsv.CSVReader;

public class CreateBrainRegionToOntologyAnnotations {
//	private static final String ID_PREFIX = "MFOEM:";
	private static final int ID_LENGTH = 6;
	
	private Map<String,String> namesToIds = new HashMap<String, String>(); 
	private Map<String,List<String>> papersToIds = new HashMap<String,List<String>>();
	private List<String> annotationFileRows = new ArrayList<String>();

	public void parseUberonSubsetFile(String ontologyIRI) throws Exception {
		ParserWrapper pw = new ParserWrapper();
		OWLGraphWrapper graph = pw.parseToOWLGraph(ontologyIRI);
		OWLOntology ont = graph.getSourceOntology();
		
		for (OWLObject obj: graph.getAllOWLObjects()){
			if (obj instanceof OWLClass) {
				String name = graph.getLabel(obj);
				String id = graph.getIdentifier(obj);
				
				namesToIds.put(name, id);
				
				for (String synonym : graph.getSynonymStrings(obj)) {
					namesToIds.put(synonym, id);
				}
			}
			
		}
		System.out.println("Loaded "+namesToIds.keySet().size()+" anatomical regions");
	}
	
	public void loadPapersAndOntologyTermsFromFile(String filename) throws Exception {
		CSVReader reader = new CSVReader(new FileReader(filename)); 
			
		List<String[]> lines = reader.readAll();
		reader.close();
		
		lines.remove(0); //header
		for (String[] line : lines) {
			
			String pubMedId = line[0];
			if (!papersToIds.containsKey(pubMedId)) {
				papersToIds.put(pubMedId, new ArrayList<String>());
			}
			for (int i=1; i<line.length; i++) {
				String val = line[i];
				if (!val.isEmpty()) {
					String ontologyId = zeropad(val,ID_LENGTH);
					papersToIds.get(pubMedId).add(ontologyId);
				}
			}
			System.out.println("Got "+papersToIds.get(pubMedId).size()+ " ontology annotations for pubmed "+pubMedId);
		}
	}
	
	
	public void lookupCandidateAnnotations() throws Exception {
		if (namesToIds.size()==0 ||papersToIds.size()==0)
			throw new RuntimeException("Empty lists, cannot lookup candidate annotations. Load data from files first. ");
		
		//first get abstract text for paper. 
		for (String pubMedId : papersToIds.keySet()) {
			
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
			for (String name : namesToIds.keySet()) {

				if (title.contains(name)
						|| abstractText.contains(name)) {
					System.out.println("Adding annotation for pmid "+pubMedId+" and region "+name);
					
					for (String ontologyId : papersToIds.get(pubMedId)) {
						annotationFileRows.add(namesToIds.get(name)+" = "+ontologyId);
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
		
		writer.write("(uberon-emotion)(curator=janna)(species=ALL)\n");
		
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
		
		annoCreator.parseUberonSubsetFile("output/uberon-subset.owl");
		annoCreator.loadPapersAndOntologyTermsFromFile("input/paper-annotations.csv");
		annoCreator.lookupCandidateAnnotations();
		annoCreator.writeToOutputFile("output/uberemo.anno");
	}

	
}
