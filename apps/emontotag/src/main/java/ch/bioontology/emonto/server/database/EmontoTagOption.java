package ch.bioontology.emonto.server.database;

/**
 * A TagOption is an item for which an emotion ontology tag will be created. 
 * In the case of the ICBO planned experiment, a TagOption is a talk or software demonstration.
 *  
 * @author hastings
 *
 */
public class EmontoTagOption {
	private String id; 
	private String name;
	private String description;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	} 
	
}
