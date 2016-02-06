package gamelogic.AI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.Controller.E_FIELD_STATE;
import gamelogic.GController;

public class mariaDB implements DB {
	
	private Logger logger = LogManager.getLogger();
	private String address;
	private String user;
	private String pw;
	private String db;
	private lib lib = new lib();
	int port;
	private Connection connection;
	
	public mariaDB(String address, int port, String user, String pw, String db){
		this.address = address;
		this.port = port;
		this.user = user;
		this.pw = pw;
		this.db = db;
		connect();
	}
	
	private void connect(){
		logger.info("Connecting to mariDB");
		String base = "jdbc:mariadb://";
		base = base+address+":"+port;
		base += "/"+db;
		base +="?tcpKeepAlive=true";
		boolean success = false;
		try{
			connection = DriverManager.getConnection(base, user, pw);
			success = true;
		}catch(SQLNonTransientConnectionException e){
			logger.error("No connection to DB! {}",e);
		}catch(SQLException e){
			logger.error("DBError {}",e);
		}finally{
			if(!success && connection != null){
				try{connection.close();}catch(SQLException e){}
			}
		}
	}
	
	@Override
	public List<Move> getMoves(E_FIELD_STATE[][] field) {
		return null;
	}

	@Override
	public void insertMoves(E_FIELD_STATE field, List<Integer> moves) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setMove(Move move) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		try {
			connection.close();
		} catch (SQLException e) {
			logger.error("mariaDB shutdown {}",e);
		}
	}

}
