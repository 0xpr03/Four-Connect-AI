package test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.Controller.E_GAME_MODE;

import gamelogic.GController;

/**
 * AI tester & trainer
 * @author Aron Heinecke
 *
 */
public class AITest {
	
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args){
		Level level_db = Level.WARN;
		Level level_ai = Level.TRACE;
		int games = 100000000;
		
		logger.entry();
		registerExitFunction();
		if(games > 1000){
			level_db = Level.WARN;
			level_ai = Level.WARN;
		}
		Configurator.setLevel("DB", level_db);
		Configurator.setLevel("AI", level_ai);
		for(int x = 0; x <= 100000000; x++){
			GController.initGame(E_GAME_MODE.KI_INTERNAL,Level.INFO);
			GController.startGame();
			while(gameRunning()){

			}
			//logger.info(GController.getprintedGameState());
		}
		logger.info(GController.getprintedGameState());
		logger.exit();
	}
	
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
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
			return false;
		default:
			return true;
		}
	}
}
