package ch.bioontology.emotion.utilities;

import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;

import owltools.graph.OWLGraphWrapper;
import owltools.io.ParserWrapper;
import au.com.bytecode.opencsv.CSVReader;

public class ParseEmotionData {

	private static final String ONTOLOGY_IRI = "http://purl.obolibrary.org/obo/MFOEM/internal/neuroanno-gen.owl";
	private static final String ID_PREFIX = "MFOEM:";
	private static final int ID_LENGTH = 6;
	private static final String OUTPUT_FILE = "output/neuroanno-gen.owl";

	public void createEMClassesFromCSV(String csvFileName) throws Exception {
		CSVReader csvReader = new CSVReader(new FileReader(csvFileName));

		OWLGraphWrapper graph = new OWLGraphWrapper(ONTOLOGY_IRI);

		OWLDataFactory df = graph.getDataFactory();
		Set<OWLAxiom> axs = new HashSet<OWLAxiom>();

		List<String[]> lines = csvReader.readAll();
		lines.remove(0); //header
		for (String[] line : lines) {
			System.out.println("Processing line for id: "+line[0]);
			if (line.length<2) continue;

			String id = ID_PREFIX+zeropad(line[0],ID_LENGTH);
			String description = line[1];

			if (description != null && description.length()>0) {

				IRI classIri = graph.getIRIByIdentifier(id);
				OWLClass cls = df.getOWLClass(classIri);
				axs.add(df.getOWLDeclarationAxiom(cls));
				axs.add(df.getOWLAnnotationAssertionAxiom(df.getRDFSLabel(), 
						classIri, df.getOWLLiteral(description)));
			}
		}

		for (OWLAxiom ax : axs) {
			graph.getManager().applyChange(new AddAxiom(graph.getSourceOntology(), ax));
		}

		ParserWrapper parserWrapper = new ParserWrapper();

		parserWrapper.saveOWL(graph.getSourceOntology(), new RDFXMLOntologyFormat(), new File(OUTPUT_FILE).toURI().toString(), graph);

	}


	private String zeropad(String input, int length) {
		StringBuffer result = new StringBuffer(input);
		while (result.length()<length) {
			result.insert(0,"0");
		}
		return result.toString();
	}

	public static final void main(String[] args) throws Exception {
		ParseEmotionData parser = new ParseEmotionData();
		parser.createEMClassesFromCSV("input/emotion-annotation-types.csv");
	}
}
