package ch.bioontology.emotion.utilities;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.AuthorType;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchPubmedServiceStub.PubmedArticleType;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import au.com.bytecode.opencsv.CSVWriter;

public class FindEmotionPapers {

	private List<PublicationDetail> papers = new ArrayList<PublicationDetail>();

	public void searchPubMedForPapers() {
		try	{

			EUtilsServiceStub searchService = new EUtilsServiceStub();
			EUtilsServiceStub.ESearchRequest searchRequest = new EUtilsServiceStub.ESearchRequest();

			searchRequest.setDb("pubmed");
			searchRequest.setMindate("1990");
			searchRequest.setRetMax("1000");
			searchRequest.setTerm("((emotion[title/abstract] OR amygdala[title/abstract] OR vmPFC[title/abstract] " +
					"OR ventromedial prefrontal cortex[title/abstract])  " +
					"AND (pet[title] OR fmri[title] OR functional magnetic resonance imaging[title] " +
					" OR functional neuroimaging[title])) NOT (review[title] OR meta-analysis[title])");

			EUtilsServiceStub.ESearchResult searchResult = searchService.run_eSearch(searchRequest);

			System.out.println("Search result count: "+searchResult.getCount());
			System.out.println("Search result IDList: "+Arrays.toString(searchResult.getIdList().getId()));

			for (String id : Arrays.asList(searchResult.getIdList().getId())) {
				EFetchPubmedServiceStub service = new EFetchPubmedServiceStub();
				EFetchPubmedServiceStub.EFetchRequest req = new EFetchPubmedServiceStub.EFetchRequest();

				req.setId(id);

				EFetchPubmedServiceStub.EFetchResult res = service.run_eFetch(req);

				if (res.getPubmedArticleSet().getPubmedArticleSetChoice().length>0) {
					if (res.getPubmedArticleSet().getPubmedArticleSetChoice()[0].getPubmedArticle()!= null) {
						PubmedArticleType article = res.getPubmedArticleSet().getPubmedArticleSetChoice()[0].getPubmedArticle();

						PublicationDetail paper = new PublicationDetail();

						paper.id = article.getMedlineCitation().getPMID().getString();
						paper.title = article.getMedlineCitation().getArticle().getArticleTitle().getString();
						if (article.getMedlineCitation().getArticle().getAuthorList()!=null) {
							AuthorType[] authors = article.getMedlineCitation().getArticle().getAuthorList().getAuthor();
							StringBuffer authorString = new StringBuffer();
							for (AuthorType author : Arrays.asList(authors)) {
								if (author.getAuthorTypeChoice_type0()!=null && author.getAuthorTypeChoice_type0().getAuthorTypeSequence_type0()!=null) {
									authorString.append(author.getAuthorTypeChoice_type0().getAuthorTypeSequence_type0().getForeName());
									authorString.append(" ");
									authorString.append(author.getAuthorTypeChoice_type0().getAuthorTypeSequence_type0().getLastName());
									authorString.append(", ");
								}
							}
							paper.authors = authorString.toString();
						}

						paper.journal = article.getMedlineCitation().getArticle().getJournal().getTitle();

						paper.date = article.getMedlineCitation().getDateCreated().getYear();
						if (article.getMedlineCitation().getArticle().getAbstract()!=null &&
								article.getMedlineCitation().getArticle().getAbstract().getAbstractText().length>0) {
							paper.abstractText = article.getMedlineCitation().getArticle().getAbstract().getAbstractText()[0].getString();
						}

						papers.add(paper);

					} 

				}
			}
			System.out.flush();

		} catch(Exception e) { 
			System.out.println(e.toString()); 
			e.printStackTrace();
		}
	}

	public void writeResultsToFile() throws Exception  {
		CSVWriter writer = new CSVWriter(new FileWriter("output/emotion-papers.csv"), ',');
		
		writer.writeNext(getHeader());

		for (PublicationDetail paper : papers) {
			String[] entries = paper.getDetails();
			writer.writeNext(entries);
		}
		writer.close();

		for (PublicationDetail paper : papers) {
			if (paper.fullText != null) {
				FileWriter fullWriter = new FileWriter("output/papertext/emotion-text-"+paper.id+".txt");
				fullWriter.write(paper.title);
				fullWriter.write("\n");
				fullWriter.write(paper.authors);
				fullWriter.write("\n");
				fullWriter.write(paper.journal+", ");
				fullWriter.write(paper.date);
				fullWriter.write("\n\n");
				if (paper.abstractText!=null) {
					fullWriter.write(paper.abstractText);
					fullWriter.write("\n\n");
				}
				fullWriter.write(paper.fullText);

				fullWriter.close();
			} else if (paper.abstractText != null) {
				FileWriter abstractWriter = new FileWriter("output/papertext/emotion-abstract-"+paper.id+".txt");
				abstractWriter.write(paper.title);
				abstractWriter.write("\n");
				abstractWriter.write(paper.authors);
				abstractWriter.write("\n");
				abstractWriter.write(paper.journal+", ");
				abstractWriter.write(paper.date);
				abstractWriter.write("\n\n");
				abstractWriter.write(paper.abstractText);

				abstractWriter.close();
			} //else we have nothing
		}

	}


	public void parseFullText() {

		for (PublicationDetail paper : papers) {
			try {
				//First get the PMC ID for the PM ID
				URL pmcIdUrl = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/elink.fcgi?dbfrom=pubmed&db=pmc&id="+paper.id);
				URLConnection yc = pmcIdUrl.openConnection();
				BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
				StringBuffer pmcIdXml = new StringBuffer();
				String inputLine;
				while ((inputLine = in.readLine()) != null) 
					pmcIdXml.append(inputLine);
				in.close();

				String pmcId = parseXmlToPmcString(pmcIdXml.toString());
				if (pmcId != null) {
					paper.pmcId = pmcId;

					URL fullTextUrl = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pmc&id="+pmcId);
					URLConnection yc2 = fullTextUrl.openConnection();
					BufferedReader in2 = new BufferedReader(new InputStreamReader(yc2.getInputStream()));
					StringBuffer fullTextXml = new StringBuffer();
					while ((inputLine = in2.readLine()) != null) 
						fullTextXml.append(inputLine);
					in2.close();

					String fullText = getFullTextFromXml(fullTextXml.toString());
					paper.fullText = fullText;

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	private String getFullTextFromXml(String xml) {

		if (xml.indexOf("<body>")>0 && xml.indexOf("</body>")>0) {
			int startIndex = xml.indexOf("<body>")+6;
			int endIndex = xml.indexOf("</body>");
			
			String fullTextXml = xml.substring(startIndex,endIndex);
			
			System.out.println("Loaded full text starting with: "+fullTextXml.substring(0, 300));
			
			return fullTextXml;
		}
		return null;
	}	

	public static final void main(String args[]) throws Exception {
		FindEmotionPapers paperFinder = new FindEmotionPapers() ;

		paperFinder.searchPubMedForPapers();
//		paperFinder.parseFullText();

//		paperFinder.writeResultsToFile();

	}


	private String parseXmlToPmcString(String xml) {
		String pmcId = null;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(xml));
			Document doc = docBuilder.parse (is);

			// normalize text representation
			doc.getDocumentElement ().normalize ();
			NodeList eLinkResults = doc.getElementsByTagName("eLinkResult");

			for(int s=0; s<eLinkResults.getLength() ; s++){


				Node linkSet = eLinkResults.item(s);

				if(linkSet.getNodeType() == Node.ELEMENT_NODE){

					Element linkSetElement = (Element)linkSet;

					NodeList linkSetDb = linkSetElement.getElementsByTagName("LinkSetDb");
					if (linkSetDb.item(0) != null) {
						Element linkSetDbElement = (Element) linkSetDb.item(0);
						Element linkElement = (Element) linkSetDbElement.getElementsByTagName("Link").item(0);
						Element idElement = (Element) linkElement.getElementsByTagName("Id").item(0);
						pmcId = idElement.getTextContent();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return pmcId;
	}

	public String[] getHeader() {
		return new String[] {"id", "pmcId", "title", "authors", "journal", "date"};
	}
	
	class PublicationDetail {
		String id; 
		String pmcId;
		String title;
		String authors;
		String journal;
		String date;
		String abstractText;
		String methodText;
		String fullText;
		
		

		public String[] getDetails() {
			return new String[] {id, pmcId, title, authors, journal, date};
		}
	}
}
