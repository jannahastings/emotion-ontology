package ch.bioontology.emonto.server.database;

import java.util.Date;

import ch.bioontology.emonto.shared.EmontoTerm;

public class EmontoTag {
	private EmontoTagUser user;
	private EmontoTagOption option;
	private EmontoTerm ontologyTerm;
	private Date timeStamp;
	
	public EmontoTagUser getUser() {
		return user;
	}
	
	public void setUser(EmontoTagUser user) {
		this.user = user;
	}
	
	public EmontoTagOption getOption() {
		return option;
	}
	
	public void setOption(EmontoTagOption option) {
		this.option = option;
	}
	
	public EmontoTerm getOntologyTerm() {
		return ontologyTerm;
	}
	
	public void setOntologyTerm(EmontoTerm ontologyTerm) {
		this.ontologyTerm = ontologyTerm;
	}
	
	public Date getTimeStamp() {
		return timeStamp;
	}
	
	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	

}
