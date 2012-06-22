package ch.bioontology.emonto.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access to a standard MySQL database for the Emotion Ontology tagging project
 * @author hastings
 *
 */
public class EmontoTagDbAccessor {
	//Singleton class, static singleton instance
	private static EmontoTagDbAccessor msInstance = null;
	//Member data
	private Connection connection = null;
	private PreparedStatement psloginUser = null, //for password checking
							  psSaveUser = null, 
							  psGetTagOptions = null, 
							  psSaveTag = null; 

	//---------------- STATIC AND PRIVATE ACCESS METHODS, singleton class ------------------
	
	private EmontoTagDbAccessor() {
		createConnection();
	}

	/**
	 * Creates the connection if it doesn't yet exist
	 * @return
	 * @throws SQLException
	 */
	private void createConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        try {
            connection = DriverManager.getConnection
        		("jdbc:mysql://localhost:3306/emontotag", "taguser", "password");
            prepareStatements();
        } catch (Exception e) {
        	e.printStackTrace();
        }
        
    }
	
	/**
	 * Set up the prepared statements for accessing the data in the database
	 */
	private void prepareStatements() {
		
	}

	
	//---------------NON-STATIC METHODS -------------------
	
	public EmontoTagDbAccessor getInstance() {
		if (msInstance == null) {
			msInstance = new EmontoTagDbAccessor();
		}
		return msInstance;
	}
	
	/**
	 * Closes the connection
	 * @throws Exception
	 */
	public void closeConnection() throws Exception {
		if (connection != null) {
			connection.close();
		}
	}

	/**
	 * Checks the specified username and password against the database
	 * @param user
	 * @return
	 */
	public boolean checkLoginUser(EmontoTagUser user) {
		
		
		return false;
	}
	
	/**
	 * Save user details
	 * @param user
	 */
	public void saveUser(EmontoTagUser user) {
		
	}
	
	/**
	 * Get the options for tagging
	 * @return
	 */
	public List<EmontoTagOption> getTagOptions() {
		List<EmontoTagOption> results = new ArrayList<EmontoTagOption>();
		
		
		return results;
	}
	
	/**
	 * Save a tag to the database
	 * @param tag
	 */
	public void createTag(EmontoTag tag) {
		
	}
	
	
	
	//-----------------MAIN METHOD------------------------------------
	
	/**
	 * Main method for test purposes only
	 * @param args
	 */
	
	public static final void main(String[] args) {
		try {
			//Connect to database
			EmontoTagDbAccessor dbAccessor = new EmontoTagDbAccessor();
			//Run some test queries
			
			//Close the connection
			dbAccessor.closeConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
