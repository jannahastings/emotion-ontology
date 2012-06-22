package ch.bioontology.emonto.parser;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.bioontology.emonto.server.owlparser.EmotionOntologyOWLParser;

public class TestEmotionOntologyOWLParser {

	EmotionOntologyOWLParser parser = new EmotionOntologyOWLParser();
	
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		parser.parseOntology();
		assertTrue(parser.getEmOntology()!=null);
	}

}
