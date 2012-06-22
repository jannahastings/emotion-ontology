package ch.bioontology.emonto.server.database;

public class EmontoTagUser {
	private String username;
	private String password;
	
	public EmontoTagUser() {
	}
	
	public EmontoTagUser(String username, String password) {
		this.username = username;
		this.password = password;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
	

}
