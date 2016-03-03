package gamelogic.AI;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTransactionRollbackException;
import java.sql.Statement;
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
	
	private final Logger logger = LogManager.getLogger("DB");
	private final String address;
	private final String user;
	private final String pw;
	private final String db;
	
	private long deletes = 0;
	private long inserts = 0;
	private long updates = 0;
	private long deletesAll = 0;
	
	private final lib lib = new lib();
	private final int port;
	private Connection connection;
	private final MemCache<ByteBuffer,Long> cache;
	
	private final boolean USE_CACHE = true;
	
	PreparedStatement stmSelFID;
	PreparedStatement stmInsFID;
	
	PreparedStatement stmInsert;
	PreparedStatement stmSelect;
	PreparedStatement stmUpdate;
	
	PreparedStatement stmDelAll;
	PreparedStatement stmDelLooses;
	
	public mariaDB(String address, int port, String user, String pw, String db, MemCache<ByteBuffer, Long> cache){
		this.address = address;
		this.port = port;
		this.user = user;
		this.pw = pw;
		this.db = db;
		connect();
		this.cache = cache;// = new MemCache<ByteBuffer,Long>(20, 30, 50000);
	}
	
	private void connect(){
		logger.info("Connecting to mariaDB, using cache: {}",USE_CACHE);
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
			stmInsFID = connection.prepareStatement("INSERT INTO `fields` (`field`) VALUES (?);", Statement.RETURN_GENERATED_KEYS);
			stmSelFID = connection.prepareStatement("SELECT `fid` FROM `fields` USE INDEX(`field`) WHERE `field` = ?;");
			
			stmInsert = connection.prepareStatement("INSERT INTO `moves` (`fid`,`player_a`,`move`,`used`,`loose`,`draw`,`win`) VALUES (?,?,?,0,0,0,0);");
			stmSelect = connection.prepareStatement("SELECT `move`,`used`,`loose`,`draw`,`win` FROM `moves` USE INDEX(`fid`,`player_a`) WHERE `fid` = ? AND `player_a` = ?;");
			stmUpdate = connection.prepareStatement("UPDATE `moves` SET `used` = ?, `loose` = ?, `draw` = ?, `win` = ? WHERE `fid` = ? AND `move` = ? AND `player_a` = ? ;");
			
			stmDelAll = connection.prepareStatement("DELETE FROM `moves` WHERE `fid` = ? AND `player_a` = ?;");
			stmDelLooses = connection.prepareStatement("DELETE FROM `moves` WHERE `fid` = ? AND `loose` = 1 AND `player_a` = ?;");
		} catch (SQLException e) {
			logger.error("Statement preparation {}",e);
		}
	}
	
	@Override
	public SelectResult getMoves(E_FIELD_STATE[][] field_in,boolean player_a) {
		logger.entry(player_a);
		try {
			SelectResult sel = new SelectResult();
			long fID = getFieldID(lib.field2sha(field_in));
			if(fID == -2) // error check
				return null;
			
			if(fID != -1){
				stmSelect.setLong(1, fID);
				stmSelect.setBoolean(2, player_a);
				ResultSet rs = stmSelect.executeQuery();
				while(rs.next()){
					Move move = new Move(fID,rs.getInt(1),rs.getBoolean(2),rs.getBoolean(3),rs.getBoolean(4),rs.getBoolean(5),player_a);
					if(move.isLoose())
						sel.addLoose(move);
					if(move.isWin())
						sel.addWin(move);
					if(move.isDraw())
						sel.addDraw(move);
					if(!move.isUsed())
						sel.addUnused(move);
				}
			}
			return sel;
		} catch (SQLException e) {
			logger.error("getMoves {}",e);
			return null;
		}
	}
	
	/**
	 * Retrive the field ID
	 * @param fieldHash
	 * @return -1 if no element was found
	 * 	-2 on error
	 */
	private long getFieldID(byte[] fieldHash){
		logger.entry();
		try {
			if(USE_CACHE){
				Long id = cache.get(ByteBuffer.wrap(fieldHash));
				if(id != null){
					return id;
				}
			}
			
			stmSelFID.setBytes(1, fieldHash);
			ResultSet rs = stmSelFID.executeQuery();
			if(rs.next()){
				if(USE_CACHE)
					cache.put(ByteBuffer.wrap(fieldHash), rs.getLong(1));
				return rs.getLong(1);
			}else{
				return -1;
			}
		} catch (SQLException e) {
			logger.error("stmSelFID {}",e);
			return -2;
		}
	}
	
	/**
	 * Insert a new field ID
	 * @param fieldHash
	 * @return new fID
	 */
	private long insertFieldID(byte[] fieldHash){
		logger.entry();
		for(int x = 0; x < 3; x++){
			try {
				stmInsFID.setBytes(1, fieldHash);
				stmInsFID.executeUpdate();
				ResultSet rs = stmInsFID.getGeneratedKeys();
				if(rs.next()){
					if(USE_CACHE)
						cache.put(ByteBuffer.wrap(fieldHash), rs.getLong(1));
					return rs.getLong(1);
				}
			} catch (SQLException e){
				if(!e.getCause().getClass().equals(SQLIntegrityConstraintViolationException.class)){
					logger.error("stmInsFID {}",e);
					logger.error("fieldHash: {}",bytesToHex(fieldHash));
				}
			}
		}
		return -1;
	}
	
	/**
	 * Bytes to hex for sha ASCII representation
	 * @param bytes
	 * @return
	 */
	public String bytesToHex(byte[] bytes) {
		StringBuffer result = new StringBuffer();
		for (byte byt : bytes)
			result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
		return result.toString();
	}

	@Override
	public SelectResult insertMoves(E_FIELD_STATE[][] field, List<Integer> moves,boolean player_a) {
		logger.entry(player_a);
		try {
			byte[] sha = lib.field2sha(field);
			long fID = getFieldID(sha);
			if(fID == -2){ // no fid or error
				return null;
			}
			if(fID == -1){
				fID = insertFieldID(sha);
				if(fID == -1){
					return null;
				}
			}
			stmInsert.setLong(1, fID);
			stmInsert.setBoolean(2, player_a);
			SelectResult sel = new SelectResult();
			for(int move : moves){
				try{
					stmInsert.setInt(3, move);
					stmInsert.executeUpdate();
					if(sel != null)
						sel.addUnused(new Move(fID, move, player_a));
				}catch(SQLException e){
					if(e.getCause().getClass().equals(SQLIntegrityConstraintViolationException.class)){
						sel = null;
					}else{
						throw e;
					}
				}
			}
			inserts++;
			connection.clearWarnings();
			return sel;
		} catch (SQLException e) {
			if(e.getCause().getClass().equals(SQLIntegrityConstraintViolationException.class) || e.getCause().getClass().equals(SQLTransactionRollbackException.class)){
				logger.debug("Ignoring duplicate insertion exception");
			}else{
				logger.error("insertMoves {}",e);
				logger.error("field:{} pla:{} moves:{}",this.bytesToHex(lib.field2sha(field)),player_a,moves);
			}
		}
		return null;
	}

	@Override
	public boolean setMove(Move move) {
		logger.entry();
		for(int i = 0; i < 3; i++){
			try {
				if( (move.isDraw() || move.isLoose() ) && !move.isUsed()){ // test that no invalid move is inserted
					logger.warn("Probably invalid move! {}",()->move.toString());
				}
				stmUpdate.setBoolean(1, move.isUsed());
				stmUpdate.setBoolean(2, move.isLoose());
				stmUpdate.setBoolean(3, move.isDraw());
				stmUpdate.setBoolean(4, move.isWin());
				stmUpdate.setLong(5, move.getFID());
				stmUpdate.setInt(6, move.getMove());
				stmUpdate.setBoolean(7, move.isPlayer_a());
				stmUpdate.executeUpdate();
				//stmUpdate.clearParameters();
				updates++;
				return true;
			} catch (SQLException e) {
				if(e.getCause().getClass() == SQLTransactionRollbackException.class){
					logger.debug("Ignoring datarace exception");
				}else{
					logger.error("updateMove {}",e);
				}
			}
		}
		return false;
	}

	@Override
	public void shutdown() {
		Logger exitLogger = LogManager.getLogger();
		exitLogger.entry();
		{
			try {
				stmInsFID.cancel();
				stmInsFID.close();
			} catch (SQLException e) {
				exitLogger.error("stmInsFID shutdown {}", e);
			}
		}
		{
			try {
				stmSelFID.cancel();
				stmSelFID.close();
			} catch (SQLException e) {
				exitLogger.error("stmSelFID shutdown {}", e);
			}
		}
		{
			try {
				stmInsert.cancel();
				stmInsert.close();
			} catch (SQLException e) {
				exitLogger.error("stmInsert shutdown {}", e);
			}
		}
		{
			try {
				stmSelect.cancel();
				stmSelect.close();
			} catch (SQLException e) {
				exitLogger.error("stmSelect shutdown {}", e);
			}
		}
		{
			try {
				stmUpdate.cancel();
				stmUpdate.close();
			} catch (SQLException e) {
				exitLogger.error("stmUpdate shutdown {}", e);
			}
		}
		{
			try {
				stmDelAll.cancel();
				stmDelAll.close();
			} catch (SQLException e) {
				exitLogger.error("stmDelAll shutdown {}", e);
			}
		}
		{
			try {
				stmDelLooses.cancel();
				stmDelLooses.close();
			} catch (SQLException e) {
				exitLogger.error("stmDelLooses shutdown {}", e);
			}
		}
		try {
			connection.close();
		} catch (SQLException e) {
			exitLogger.error("mariaDB shutdown {}", e);
		}
		exitLogger.info("Stats: Deletes:{} DelAlls:{} Inserts:{} Updates:{}",deletes,deletesAll,inserts, updates);
	}

	@Override
	public boolean deleteMoves(long fid, boolean player_a) {
		logger.entry();
		deletes++;
		try {
			stmDelAll.setLong(1, fid);
			stmDelAll.setBoolean(2, player_a);
			stmDelAll.executeUpdate();
			deletes++;
			return true;
		} catch (SQLException e) {
			logger.error("stmDelAll {}",e);
		}
		return false;
	}

	@Override
	public boolean deleteLooses(long fid, boolean player_a) {
		logger.entry();
		deletesAll++;
		try {
			stmDelLooses.setLong(1, fid);
			stmDelLooses.setBoolean(2, player_a);
			stmDelLooses.executeUpdate();
			deletesAll++;
			return true;
		} catch (SQLException e) {
			logger.error("stmDelLooses {}",e);
		}
		return true;
	}
	
	public SelectResult testField(E_FIELD_STATE[][] field_in) {
		logger.entry();
		try {
			SelectResult sel = new SelectResult();
			byte[] field = lib.field2sha(field_in);
			long fID = getFieldID(field);
			if(fID == -2) // error check
				return null;
			
			if(fID != -1){
				stmSelect.setLong(1, fID);
				{
					stmSelect.setBoolean(2, false);
					ResultSet rs = stmSelect.executeQuery();
					while(rs.next()){
						Move move = new Move(fID,rs.getInt(1),rs.getBoolean(2),rs.getBoolean(3),rs.getBoolean(4),rs.getBoolean(5),false);
						sel.addUnused(move);
					}
					rs.close();
				}
				{
					stmSelect.setBoolean(2, true);
					ResultSet rs = stmSelect.executeQuery();
					while(rs.next()){
						Move move = new Move(fID,rs.getInt(1),rs.getBoolean(2),rs.getBoolean(3),rs.getBoolean(4),rs.getBoolean(5),true);
						sel.addUnused(move);
					}
					rs.close();
				}
			}
			return sel;
		} catch (SQLException e) {
			logger.error("getMoves {}",e);
			return null;
		}
	}
	
	public byte[] getHash(){
		 return lib.field2sha(GController.getFieldState());
	}
}
