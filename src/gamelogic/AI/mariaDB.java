package gamelogic.AI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.Controller.E_FIELD_STATE;

/**
 * MariaDB DB handler for KBS
 * @author Aron Heinecke
 *
 */
public class mariaDB implements DB {
	
	private Logger logger = LogManager.getLogger("DB");
	private String address;
	private String user;
	private String pw;
	private String db;
	private lib lib = new lib();
	int port;
	private Connection connection;
	
	private Random rand = new Random(System.nanoTime());
	
	PreparedStatement stmInsert;
	PreparedStatement stmSelect;
	PreparedStatement stmUpdate;
	PreparedStatement stmDelAll;
	PreparedStatement stmDelLooses;
	
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
		
		try {
			stmInsert = connection.prepareStatement("INSERT INTO `moves` (`field`,`move`,`used`,`draw`,`loose`) VALUES (?,?,?,?,?);");
			stmSelect = connection.prepareStatement("SELECT `move`,`draw`,`loose`,`used` FROM `moves` USE INDEX (field) WHERE `field` = ?;");
			stmUpdate = connection.prepareStatement("UPDATE `moves` SET `used` = ?, `draw` = ?, `loose`= ? WHERE `field` = ? AND `move` = ?;");
			stmDelAll = connection.prepareStatement("DELETE FROM `moves` WHERE `field` = ?;");
			stmDelLooses = connection.prepareStatement("DELETE FROM `moves` WHERE `field` = ? AND `loose` = 1;");
		} catch (SQLException e) {
			logger.error("Statement preparation {}",e);
		}
	}
	
	@Override
	public List<Move> getMoves(E_FIELD_STATE[][] field_in) {
		logger.entry();
		try {
			byte[] field = lib.field2sha(field_in);
			stmSelect.setBytes(1, field);
			ResultSet rs = stmSelect.executeQuery();
			List<Move> moves = new ArrayList<Move>(7);
			while(rs.next()){
				moves.add(new Move(field,rs.getInt(1),rs.getBoolean(2),rs.getBoolean(3),rs.getBoolean(4)));
			}
			rs.close();
			return moves;
		} catch (SQLException e) {
			logger.error("getMoves {}",e);
			return null;
		}
	}

	@Override
	public Move insertMoves(E_FIELD_STATE[][] field, List<Integer> moves) {
		logger.entry();
		try {
			byte[] sha = lib.field2sha(field);
			stmInsert.setBytes(1, sha);
			stmInsert.setBoolean(3, false);
			stmInsert.setBoolean(4, false);
			stmInsert.setBoolean(5, false);
			for(int move : moves){
				stmInsert.setInt(2, move);
				stmInsert.executeUpdate();
			}
			//stmInsert.clearParameters();
			return new Move(sha,moves.get(rand.nextInt(moves.size())),false,false,false);
		} catch (SQLException e) {
			if(e.getCause().getClass().equals(SQLIntegrityConstraintViolationException.class) || e.getCause().getClass().equals(SQLTransactionRollbackException.class)){
				logger.info("Ignoring duplicate insertion exception");
			}else{
				logger.error("insertMoves {}",e);
			}
			return null;
		}
	}

	@Override
	public boolean setMove(Move move) {
		logger.entry();
		try {
			stmUpdate.setBoolean(1, move.isUsed());
			stmUpdate.setBoolean(2, move.isDraw());
			stmUpdate.setBoolean(3, move.isLoose());
			stmUpdate.setBytes(4, move.getField());
			stmUpdate.setInt(5, move.getMove());
			stmUpdate.executeUpdate();
			//stmUpdate.clearParameters();
			return true;
		} catch (SQLException e) {
			if(e.getCause().getClass() == SQLTransactionRollbackException.class){
				logger.info("Ignoring datarace exception");
			}else{
				logger.error("updateMove {}",e);
			}
			return false;
		}
	}

	@Override
	public void shutdown() {
		logger.entry();
		{
		try {
			stmInsert.cancel();
			stmInsert.close();
		} catch (SQLException e) {
			logger.error("stmInsert shutdown {}",e);
		}
		}
		{
		try {
			stmSelect.cancel();
			stmSelect.close();
		} catch (SQLException e) {
			logger.error("stmSelect shutdown {}",e);
		}
		}
		{
		try {
			stmUpdate.cancel();
			stmUpdate.close();
		} catch (SQLException e) {
			logger.error("stmUpdate shutdown {}",e);
		}
		}
		{
		try {
			stmDelAll.cancel();
			stmDelAll.close();
		} catch (SQLException e) {
			logger.error("stmDelAll shutdown {}",e);
		}
		}
		{
		try {
			stmDelLooses.cancel();
			stmDelLooses.close();
		} catch (SQLException e) {
			logger.error("stmDelLooses shutdown {}",e);
		}
		}
		try {
			connection.close();
		} catch (SQLException e) {
			logger.error("mariaDB shutdown {}",e);
		}
	}

	@Override
	public boolean deleteMoves(byte[] fieldHash) {
		logger.entry();
		try {
			stmDelAll.setBytes(1, fieldHash);
			stmDelAll.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error("stmDelAll {}",e);
			return false;
		}
	}

	@Override
	public boolean deleteLooses(byte[] childHash) {
		logger.entry();
		try {
			stmDelLooses.setBytes(1, childHash);
			stmDelLooses.executeUpdate();
			return true;
		} catch (SQLException e) {
			logger.error("stmDelLooses {}",e);
			return false;
		}
	}

}
