package runnables;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Simple class to get statistics about the DB
 * Execution time can be up to some hours
 * @author Aron Heinecke
 *
 */
public class DBAnalyzer {
	private static Logger logger = LogManager.getLogger("DB");
	private static String address = "localhost";
	private static String user = "ai";
	private static String pw = "66z1ayi9vweIDdWa1n0Z";
	private static String db = "ai";
	private static int port = 3306;
	
	private static Connection connection;
	
	public static void main(String[] args){
		logger.info("Starting analyzer on {}",System.currentTimeMillis());
		connect();
		try {
			if(connection != null){
				if(!connection.isClosed()){
					long time = System.currentTimeMillis();
					run_analysis();
					logger.info("Took {} seconds",(System.currentTimeMillis() - time) / 100);
				}
			}
		} catch (SQLException e1) {
			logger.error("{}",e1);
		}
		try {
			connection.close();
		} catch (SQLException e) {
			logger.error("{}",e);
		}
		logger.info("Finished analysis");
	}
	
	private static void run_analysis() throws SQLException{
		long unused = -1;
		long amount = -1;
		long irregulars = -1;
		long wins_a = -1;
		long wins_b = -1;
		long draws_a = -1;
		long draws_b = -1;
		{
			ResultSet rs = execute("SELECT COUNT(fid) FROM moves WHERE `used`=0;");
			if(rs != null){
				if(rs.next()){
					unused = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(fid) FROM moves;");
			if(rs != null){
				if(rs.next()){
					amount = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(fid) FROM moves WHERE `used`=0 AND (`win`=1 OR `draw`=1 OR `loose`=1);");
			if(rs != null){
				if(rs.next()){
					irregulars = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(`fid`) FROM moves WHERE `win`=1 AND `player_a`=1;");
			if(rs != null){
				if(rs.next()){
					wins_a = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(`fid`) FROM moves WHERE `win`=1 AND `player_a`=0;");
			if(rs != null){
				if(rs.next()){
					wins_b = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(fid) FROM moves WHERE `draw`=1 AND `player_a`=1;");
			if(rs != null){
				if(rs.next()){
					draws_a = rs.getLong(1);
				}
			}
		}
		{
			ResultSet rs = execute("SELECT COUNT(fid) FROM moves WHERE `draw`=1 AND `player_a`=0;");
			if(rs != null){
				if(rs.next()){
					draws_b = rs.getLong(1);
				}
			}
		}
		logger.info("Unused: {} of {} errorous entries: {}",unused,amount,irregulars);
		logger.info("Wins A {} Wins B {} Draws A {} Draws B {}",wins_a, wins_b, draws_a, draws_b);
	}
	
	private static ResultSet execute(String sql){
		try{
			Statement stm = connection.createStatement();
			ResultSet rs = stm.executeQuery(sql);
			return rs;
		}catch(Exception e){
			logger.error(e);
			return null;
		}
	}
	
	private static void connect(){
		logger.info("Connecting to mariaDB");
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
}
