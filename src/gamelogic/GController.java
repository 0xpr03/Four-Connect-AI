package gamelogic;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.logging.log4j.Level;

import gamelogic.ControllerBase.E_FIELD_STATE;
import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.AI.KBS_player;
import gamelogic.AI.MemCache;
import gamelogic.AI.WebPlayer;
import gamelogic.AI.mariaDB;

/**
 * The MAIN controller of the game four connect
 * This is to be used from all classes
 * @author Aron Heinecke
 *
 */
public class GController {
	
	private static Controller controller;
	
	public static void init(String address, int port, String user, String pw, String db){
		MemCache<ByteBuffer,Long> cache = new MemCache<ByteBuffer, Long>(300, 300, 12000,2000,11000);
		controller = new Controller(new KBS_player<mariaDB>(new mariaDB(address,3306,user,pw,db,cache)), new KBS_player<mariaDB>(new mariaDB(address,3306,user,pw,db,cache)));
	}
	
	/**
	 * @see gamelogic.Controller#stopGame()
	 */
	public static void stopGame() {
		controller.stopGame();
	}

	public static void init(Controller cont){
		controller = cont;
	}
	
	public static void init(){
		controller = new Controller(new WebPlayer(), null);
	}
	
	/**
	 * 
	 * @see gamelogic.ControllerBase#restart()
	 */
	public static void restart() {
		controller.restart();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getPossibilities()
	 */
	public static List<Integer> getPossibilities() {
		return controller.getPossibilities();
	}

	/**
	 * 
	 * @see gamelogic.ControllerBase#moveAI_A()
	 */
	public static void moveAI_A() {
		controller.moveAI_A();
	}

	/**
	 * 
	 * @see gamelogic.ControllerBase#moveAI_B()
	 */
	public static void moveAI_B() {
		controller.moveAI_B();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getGamemode()
	 */
	public static E_GAME_MODE getGamemode() {
		return controller.getGamemode();
	}

	/**
	 * 
	 * @see gamelogic.ControllerBase#shutdown()
	 */
	public static void shutdown() {
		controller.shutdown();
	}

	/**
	 * @see gamelogic.ControllerBase#capitulate(gamelogic.ControllerBase.E_PLAYER)
	 */
	public static void capitulate() {
		controller.capitulate();
	}

	/**
	 * @param gamemode
	 * @param loglevel
	 * @see gamelogic.ControllerBase#initGame(gamelogic.ControllerBase.E_GAME_MODE, org.apache.logging.log4j.Level)
	 */
	public static void initGame(E_GAME_MODE gamemode, Level loglevel, int x_max,int y_max) {
		controller.initGame(gamemode, loglevel, x_max, y_max);
	}

	/**
	 * @param gamemode
	 * @see gamelogic.ControllerBase#initGame(gamelogic.ControllerBase.E_GAME_MODE)
	 */
	public static void initGame(E_GAME_MODE gamemode, int x_max, int y_max) {
		controller.initGame(gamemode, x_max, y_max);
	}

	/**
	 * 
	 * @see gamelogic.ControllerBase#startGame()
	 */
	public static void startGame() {
		controller.startGame();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getRandomBoolean()
	 */
	public static boolean getRandomBoolean() {
		return controller.getRandomBoolean();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getFieldState()
	 */
	public static E_FIELD_STATE[][] getFieldState() {
		return controller.getFieldState();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getGameState()
	 */
	public static E_GAME_STATE getGameState() {
		return controller.getGameState();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getMoves()
	 */
	public static int getMoves() {
		return controller.getMoves();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getprintedGameState()
	 */
	public static String getprintedGameState() {
		return controller.getprintedGameState();
	}

	/**
	 * 
	 * @see gamelogic.ControllerBase#printGameState()
	 */
	public static void printGameState() {
		controller.printGameState();
	}

	/**
	 * @param field
	 * @return
	 * @see gamelogic.ControllerBase#D_setField(gamelogic.ControllerBase.E_FIELD_STATE[][])
	 */
	public static boolean D_setField(E_FIELD_STATE[][] field) {
		return controller.D_setField(field);
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#D_analyzeField()
	 */
	public static boolean D_analyzeField() {
		return controller.D_analyzeField();
	}

	/**
	 * @param state
	 * @return
	 * @see gamelogic.ControllerBase#setState(gamelogic.ControllerBase.E_GAME_STATE)
	 */
	public static boolean setState(E_GAME_STATE state) {
		return controller.setState(state);
	}

	/**
	 * @param input
	 * @return
	 * @see gamelogic.ControllerBase#D_parseField(java.lang.String)
	 */
	public static E_FIELD_STATE[][] D_parseField(String input) {
		return controller.D_parseField(input);
	}

	/**
	 * @param column
	 * @return
	 * @see gamelogic.ControllerBase#insertStone(int)
	 */
	public static boolean insertStone(int column) {
		return controller.insertStone(column);
	}
	
	/**
	 * @return
	 * @see gamelogic.ControllerBase#getX_MAX()
	 */
	public static int getX_MAX() {
		return controller.getX_MAX();
	}

	/**
	 * @return
	 * @see gamelogic.ControllerBase#getY_MAX()
	 */
	public static int getY_MAX() {
		return controller.getY_MAX();
	}
	
	/**
	 * @return
	 * @see gamelogic.ControllerBase#getLAST_GAME()
	 */
	public static GameStore getLAST_GAME() {
		return controller.getLAST_GAME();
	}
}
