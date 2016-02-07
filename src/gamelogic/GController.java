package gamelogic;

import org.apache.logging.log4j.Level;

import gamelogic.Controller.E_FIELD_STATE;
import gamelogic.Controller.E_GAME_MODE;
import gamelogic.Controller.E_GAME_STATE;
import gamelogic.Controller.E_PLAYER;
import gamelogic.AI.KBS;
import gamelogic.AI.mariaDB;

/**
 * The MAIN controller of the game four connect
 * This is to be used from all classes
 * @author Aron Heinecke
 *
 */
public class GController {
	
	private static Controller<KBS<mariaDB>> controller = null;
	
	/**
	 * 
	 * @see gamelogic.Controller#restart()
	 */
	public static void restart() {
		controller.restart();
	}

	/**
	 * 
	 * @see gamelogic.Controller#moveAI_A()
	 */
	public static void moveAI_A() {
		controller.moveAI_A();
	}

	/**
	 * 
	 * @see gamelogic.Controller#moveAI_B()
	 */
	public static void moveAI_B() {
		controller.moveAI_B();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getGamemode()
	 */
	public static E_GAME_MODE getGamemode() {
		return controller.getGamemode();
	}

	public static void init(String address, int port, String user, String pw, String db){
		boolean learning = true;
		controller = new Controller<KBS<mariaDB>>(new KBS<mariaDB>(new mariaDB(address,3306,user,pw,db),learning), new KBS<mariaDB>(new mariaDB(address,3306,user,pw,db),learning));
	}

	/**
	 * 
	 * @see gamelogic.Controller#shutdown()
	 */
	public static void shutdown() {
		controller.shutdown();
	}

	/**
	 * @param player
	 * @see gamelogic.Controller#capitulate(gamelogic.Controller.E_PLAYER)
	 */
	public static void capitulate(E_PLAYER player) {
		controller.capitulate(player);
	}

	/**
	 * @param gamemode
	 * @param loglevel
	 * @see gamelogic.Controller#initGame(gamelogic.Controller.E_GAME_MODE, org.apache.logging.log4j.Level)
	 */
	public static void initGame(E_GAME_MODE gamemode, Level loglevel) {
		controller.initGame(gamemode, loglevel);
	}

	/**
	 * @param gamemode
	 * @see gamelogic.Controller#initGame(gamelogic.Controller.E_GAME_MODE)
	 */
	public static void initGame(E_GAME_MODE gamemode) {
		controller.initGame(gamemode);
	}

	/**
	 * 
	 * @see gamelogic.Controller#startGame()
	 */
	public static void startGame() {
		controller.startGame();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getRandomBoolean()
	 */
	public static boolean getRandomBoolean() {
		return controller.getRandomBoolean();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getFieldState()
	 */
	public static E_FIELD_STATE[][] getFieldState() {
		return controller.getFieldState();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getGameState()
	 */
	public static E_GAME_STATE getGameState() {
		return controller.getGameState();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getMoves()
	 */
	public static int getMoves() {
		return controller.getMoves();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getprintedGameState()
	 */
	public static String getprintedGameState() {
		return controller.getprintedGameState();
	}

	/**
	 * 
	 * @see gamelogic.Controller#printGameState()
	 */
	public static void printGameState() {
		controller.printGameState();
	}

	/**
	 * @param field
	 * @return
	 * @see gamelogic.Controller#D_setField(gamelogic.Controller.E_FIELD_STATE[][])
	 */
	public static boolean D_setField(E_FIELD_STATE[][] field) {
		return controller.D_setField(field);
	}

	/**
	 * @return
	 * @see gamelogic.Controller#D_analyzeField()
	 */
	public static boolean D_analyzeField() {
		return controller.D_analyzeField();
	}

	/**
	 * @param state
	 * @return
	 * @see gamelogic.Controller#setState(gamelogic.Controller.E_GAME_STATE)
	 */
	public static boolean setState(E_GAME_STATE state) {
		return controller.setState(state);
	}

	/**
	 * @param input
	 * @return
	 * @see gamelogic.Controller#D_parseField(java.lang.String)
	 */
	public static E_FIELD_STATE[][] D_parseField(String input) {
		return controller.D_parseField(input);
	}

	/**
	 * @param column
	 * @return
	 * @see gamelogic.Controller#insertStone(int)
	 */
	public static boolean insertStone(int column) {
		return controller.insertStone(column);
	}
	
	/**
	 * @return
	 * @see gamelogic.Controller#getX_MAX()
	 */
	public static int getX_MAX() {
		return controller.getX_MAX();
	}

	/**
	 * @return
	 * @see gamelogic.Controller#getY_MAX()
	 */
	public static int getY_MAX() {
		return controller.getY_MAX();
	}
}
