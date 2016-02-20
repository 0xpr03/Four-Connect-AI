package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;

/**
 * Interactive test Player vs AI of type KBS
 * (most verbose output)
 * @author Aron Heinecke
 *
 */
public class AITest_Interact {
	
	private static Logger logger = LogManager.getLogger();
	
	private static void init(){
		GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai", true);
		Level level_db = Level.TRACE;
		Level level_ai = Level.TRACE;
		Configurator.setLevel("DB", level_db);
		Configurator.setLevel("AI", level_ai);
		registerExitFunction();
	}
	
	public static void main(String[] args){
		init();
		GController.initGame(E_GAME_MODE.SINGLE_PLAYER,Level.INFO);
		GController.startGame();
		logger.info("Gamemode: {}",GController.getGamemode());
		E_GAME_STATE state = E_GAME_STATE.NONE;
		while(gameRunning()){
			if(state != GController.getGameState()){
				logger.info("STATE: {}",GController.getGameState());
				state = GController.getGameState();
			}
			if(GController.getGameState() == E_GAME_STATE.PLAYER_A){
				logger.info("Possibilities: \n{}",GController.getPossibilities());
				logger.info("\n{}",GController.getprintedGameState());
				System.out.println("Please select a column, 0-"+GController.getX_MAX());
				String input;
				try{
					input = readLine("");
					int in = Integer.parseInt(input);
					logger.debug("Player using {}",in);
					if(!GController.insertStone(in)){
						System.out.println("Not in range!");
					}
				}catch(NumberFormatException e){
					System.out.println("Not a number!");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					logger.error(e1);
				}
			}else if(GController.getGameState() == E_GAME_STATE.PLAYER_B){
				GController.moveAI_A();
			}
		}
		System.out.println(GController.getprintedGameState());
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static String readLine(String format, Object... args) throws IOException {
	    if (System.console() != null) {
	        return System.console().readLine(format, args);
	    }
	    System.out.print(String.format(format, args));
	    BufferedReader reader = new BufferedReader(new InputStreamReader(
	            System.in));
	    return reader.readLine();
	}
	
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("State: {}",GController.getGameState());
				GController.shutdown();
			}
		});
	}
	
	/**
	 * Returns false if draw,win_a,win_b is the current gamestate
	 * true otherwise
	 * @return
	 */
	private static boolean gameRunning(){
		switch(GController.getGameState()){
		case DRAW:
		case WIN_A:
		case WIN_B:
		case RESTART:
			return false;
		default:
			return true;
		}
	}
	
}
