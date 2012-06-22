package ch.bioontology.emonto.shared;

import java.io.Serializable;

public class EmontoTerm implements Serializable {
	
	/**
	 * Generated serialVersionUID 
	 */
	private static final long serialVersionUID = 996949852111674991L;
	
	private String id;
	private String label;
	private String definition;
	
	public EmontoTerm() {
		
	}
	
	public EmontoTerm(String id, String label, String definition) {
		this.id = id;
		this.label = label;
		this.definition = definition;
	}
	
	public EmontoTerm(String id, String label) {
		this.id = id;
		this.label = label;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EmontoTerm other = (EmontoTerm) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		} else if (!definition.equals(other.definition))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		return true;
	}
	
	public String toString() {
		return id + " -- "+label;
	}

}
