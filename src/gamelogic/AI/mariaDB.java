package gamelogic.AI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.ControllerBase.E_FIELD_STATE;
import gamelogic.GController;

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
	
	private long deletes = 0;
	private long inserts = 0;
	private long updates = 0;
	private long deletesAll = 0;
	
	private lib lib = new lib();
	int port;
	private Connection connection;
	
	PreparedStatement stmInsert;
	PreparedStatement stmSelect;
	PreparedStatement stmUpdate;
	PreparedStatement stmDelAll;
	PreparedStatement stmDelLooses;
	
	PreparedStatement stmInsertRelation;
	
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
			stmInsert = connection.prepareStatement("INSERT INTO `moves` (`field`,`move`,`player_a`,`used`) VALUES (?,?,?,?);");
			stmSelect = connection.prepareStatement("SELECT `move`,`used` FROM `moves` USE INDEX(field,player_a) WHERE `field` = ? AND `player_a` = ?;");
			stmUpdate = connection.prepareStatement("UPDATE `moves` SET `used` = ? WHERE `field` = ? AND `move` = ? AND `player_a` = ? ;");
			
			
			stmDelAll = connection.prepareStatement("DELETE FROM `moves` WHERE `field` = ?;");
			stmDelLooses = connection.prepareStatement("DELETE FROM `moves` WHERE `field` = ? AND `loose` = 1;");
			stmInsertRelation = connection.prepareStatement("INSERT INTO `relations` (`parent`,`move`,`child`) VALUES (?,?,?);");
		} catch (SQLException e) {
			logger.error("Statement preparation {}",e);
		}
	}
	
	@Override
	public SelectResult getMoves(E_FIELD_STATE[][] field_in,boolean player_a) {
		logger.entry();
		try {
			byte[] field = lib.field2sha(field_in);
			stmSelect.setBytes(1, field);
			stmSelect.setBoolean(2, player_a);
			ResultSet rs = stmSelect.executeQuery();
			SelectResult sel = new SelectResult();
			while(rs.next()){
				Move move = new Move(field,rs.getInt(1),false,false,rs.getBoolean(2),player_a);
				if(move.isUsed()){
					sel.addWin(move);
				}else{
					sel.addUnused(move);
				}
			}
			rs.close();
			return sel;
		} catch (SQLException e) {
			logger.error("getMoves {}",e);
			return null;
		}
	}

	@Override
	public SelectResult insertMoves(E_FIELD_STATE[][] field, List<Integer> moves,boolean player_a) {
		logger.entry();
		try {
			byte[] sha = lib.field2sha(field);
			
			stmInsert.setBytes(1, sha);
			stmInsert.setBoolean(3, player_a);
			stmInsert.setBoolean(4, false);
			SelectResult sel = new SelectResult();
			for(int move : moves){
				stmInsert.setInt(2, move);
				stmInsert.executeUpdate();
				sel.addUnused(new Move(sha, move, player_a));
			}
			inserts++;
			return sel;
		} catch (SQLException e) {
//			if(e.getCause().getClass().equals(SQLIntegrityConstraintViolationException.class) || e.getCause().getClass().equals(SQLTransactionRollbackException.class)){
//				logger.error("Ignoring duplicate insertion exception");
//			}else{
				logger.error("insertMoves {}",e);
//			}
			return null;
		}
	}

	@Override
	public boolean setMove(Move move) {
		logger.entry();
		try {
			if( (move.isDraw() || move.isLoose() ) && !move.isUsed()){ // test that no invalid move is inserted
				logger.warn("Probably invalid move! {}",()->move.toString());
			}
			stmUpdate.setBoolean(1, move.isUsed());
			stmUpdate.setBytes(2, move.getField());
			stmUpdate.setInt(3, move.getMove());
			stmUpdate.setBoolean(4, move.isPlayer_a());
			stmUpdate.executeUpdate();
			//stmUpdate.clearParameters();
			updates++;
			return true;
		} catch (SQLException e) {
//			if(e.getCause().getClass() == SQLTransactionRollbackException.class){
//				logger.debug("Ignoring datarace exception");
//			}else{
				logger.error("updateMove {}",e);
//			}
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
		logger.info("Stats: Deletes:{} DelAlls:{} Inserts:{} Updates:{}",deletes,deletesAll,inserts, updates);
	}
	
	public boolean insertRelation(Move parent, E_FIELD_STATE[][] child){
		return insertRelation(parent,lib.field2sha(child));
	}
	
	public boolean insertRelation(Move parent, byte[] child){
		try {
			stmInsertRelation.setBytes(1, parent.getField());
			stmInsertRelation.setInt(2, parent.getMove());
			stmInsertRelation.setBytes(3, child);
			stmInsertRelation.executeUpdate();
			return true;
		} catch (SQLException e) {
			//logger.error("stmInsertRelation {}",e);
			return false;
		}
	}

	@Override
	public boolean deleteMoves(byte[] fieldHash) {
		logger.entry();
		deletes++;
//		try {
//			stmDelAll.setBytes(1, fieldHash);
//			stmDelAll.executeUpdate();
//			deletes++;
//			return true;
//		} catch (SQLException e) {
//			logger.error("stmDelAll {}",e);
//			return false;
//		}
		return true;
	}

	@Override
	public boolean deleteLooses(byte[] childHash) {
		logger.entry();
		deletesAll++;
//		try {
////			stmDelLooses.setBytes(1, childHash);
////			stmDelLooses.executeUpdate();
//			deletesAll++;
//			return true;
//		} catch (SQLException e) {
//			if(e.getCause().getClass() == SQLTransactionRollbackException.class){
//				logger.debug("Ignoring datarace exception");
//			}else{
//				logger.error("stmDelLooses {}",e);
//			}
//			return false;
//		}
		return true;
	}
	public byte[] getHash(){
		 return lib.field2sha(GController.getFieldState());
	}
}
