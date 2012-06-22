package ch.bioontology.emonto.server.owlparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import org.semanticweb.owlapi.io.OWLOntologyCreationIOException;
import org.semanticweb.owlapi.io.OWLParser;
import org.semanticweb.owlapi.io.OWLParserException;
import org.semanticweb.owlapi.io.UnparsableOntologyException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.UnloadableImportException;

import owltools.io.ParserWrapper;
import owltools.mooncat.Mooncat;

public class EmotionOntologyOWLParser {

	private static final String EMONTO_URL = "http://emotion-ontology.googlecode.com/svn/trunk/ontology/MFOEM.owl";
	private static final String BACKUP_FILE = "C:/Work/Source/Java/emontobrowse/war/emontobrowse/ontology/MFOEM.owl";
	private boolean loadedFromWeb = true;
	private boolean loadedFromFile = false;

	private OWLOntology emOntology = null;
	private EmotionOntologyGraph emOntoGraph = null;
	
	public void parseOntology() {
		ParserWrapper parser = new ParserWrapper();
		
		try {
			emOntology = parser.parse(EMONTO_URL);
			emOntoGraph = new EmotionOntologyGraph(emOntology);
			
			emOntoGraph.addSupportOntologiesFromImportsClosure();
			Mooncat mooncat = new Mooncat(emOntoGraph);
			mooncat.mergeOntologies();
			
			loadedFromWeb = true; loadedFromFile = false;
			
		}
		catch (OWLOntologyCreationIOException e) {
			// IOExceptions during loading get wrapped in an OWLOntologyCreationIOException
			IOException ioException = e.getCause();
			if (ioException instanceof FileNotFoundException) {
				System.out.println("Could not load ontology. File not found: " + ioException.getMessage());
			}
			else if (ioException instanceof UnknownHostException) {
				System.out.println("Could not load ontology. Unknown host: " + ioException.getMessage());
			}
			else {
				System.out.println("Could not load ontology: " + ioException.getClass().getSimpleName() + " " + ioException.getMessage());
			}

			System.out.println("Trying local file! ");
			try {
				parser.getManager().setSilentMissingImportsHandling(true);
				emOntology = parser.parse(BACKUP_FILE); 
				emOntoGraph = new EmotionOntologyGraph(emOntology);
				loadedFromFile = true; loadedFromWeb = false;
			} catch (Exception e2) {
				e2.printStackTrace();
				System.out.println("FATAL parse backup file error, cannot continue.");
			}

		}
		catch (UnparsableOntologyException e) {
			// If there was a problem loading an ontology because there are syntax errors in the document (file) that
			// represents the ontology then an UnparsableOntologyException is thrown
			System.out.println("Could not parse the ontology: " + e.getMessage());
			// A map of errors can be obtained from the exception
			Map<OWLParser, OWLParserException> exceptions = e.getExceptions();
			// The map describes which parsers were tried and what the errors were
			for (OWLParser owlParser : exceptions.keySet()) {
				System.out.println("Tried to parse the ontology with the " + owlParser.getClass().getSimpleName() + " parser");
				System.out.println("Failed because: " + exceptions.get(owlParser).getMessage());
			}
		}
		catch (UnloadableImportException e) {
			// If our ontology contains imports and one or more of the imports could not be loaded then an
			// UnloadableImportException will be thrown (depending on the missing imports handling policy)
			System.out.println("Could not load import: " + e.getImportsDeclaration());
			// The reason for this is specified and an OWLOntologyCreationException
			OWLOntologyCreationException cause = e.getOntologyCreationException();
			System.out.println("Reason: " + cause.getMessage());
		}
		catch (OWLOntologyCreationException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
		} 
		catch (IOException e) {
			System.out.println("Could not load ontology: " + e.getMessage());
		}
	}
	
	public boolean isLoadedFromFile() {
		return loadedFromFile;
	}
	
	public boolean isLoadedFromWeb() {
		return loadedFromWeb;
	}

	public EmotionOntologyGraph getEmOntoGraph() {
		return emOntoGraph;
	}

	public void setEmOntoGraph(EmotionOntologyGraph emOntoGraph) {
		this.emOntoGraph = emOntoGraph;
	}

	public OWLOntology getEmOntology() {
		return emOntology;
	}

	public void setEmOntology(OWLOntology emOntology) {
		this.emOntology = emOntology;
	}
	
	/**
	 * Method for creating backups. Note that this cannot be run from within the GWT environment. 
	 */
	public void saveToFile() {
		if (emOntoGraph != null) { //sanity check
			try {
			
				OWLOntologyManager m = emOntoGraph.getSourceOntology().getOWLOntologyManager();
				m.saveOntology(emOntoGraph.getSourceOntology(), new FileOutputStream(new File(BACKUP_FILE)));
				
			} catch (Exception e) {
				System.out.println("Failed to back up loaded emotion ontology: "+e.getMessage());
				e.printStackTrace();
			}
		}
	}


}


