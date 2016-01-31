package gamelogic;

import org.apache.logging.log4j.Level;

import gamelogic.Controller.E_FIELD_STATE;
import gamelogic.Controller.E_GAME_MODE;
import gamelogic.Controller.E_GAME_STATE;

/**
 * The MAIN controller of the game four connect
 * This is to be used from all classes
 * @author Aron Heinecke
 *
 */
public class GController {
	private final static Controller controller = new Controller();

	public static void initGame(E_GAME_MODE gamemode, Level loglevel) {
		controller.initGame(gamemode, loglevel);
	}

	public static void initGame(E_GAME_MODE gamemode) {
		controller.initGame(gamemode);
	}

	public static void startGame() {
		controller.startGame();
	}

	public static boolean getRandomBoolean() {
		return controller.getRandomBoolean();
	}

	public static E_FIELD_STATE[][] getFieldState() {
		return controller.getFieldState();
	}

	public static E_GAME_STATE getGameState() {
		return controller.getGameState();
	}

	public static int getMoves() {
		return controller.getMoves();
	}

	public static String getprintedGameState() {
		return controller.getprintedGameState();
	}

	public static void printGameState() {
		controller.printGameState();
	}

	public static boolean checkWin(int posx, int posy) {
		return controller.checkWin(posx, posy);
	}

	public static boolean D_setField(E_FIELD_STATE[][] field) {
		return controller.D_setField(field);
	}

	public static boolean D_analyzeField() {
		return controller.D_analyzeField();
	}

	public static boolean setState(E_GAME_STATE state) {
		return controller.setState(state);
	}

	public static E_FIELD_STATE[][] D_parseField(String input) {
		return controller.D_parseField(input);
	}

	public static boolean insertStone(int column) {
		return controller.insertStone(column);
	}
	
}
