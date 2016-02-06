package test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.Controller.E_GAME_MODE;

import gamelogic.GController;

public class AITest {
	
	private static Logger logger = LogManager.getLogger();
	
	public static void main(String[] args){
		logger.entry();
		registerExitFunction();
		Configurator.setLevel("DB", Level.WARN);
		for(int x = 0; x <= 10000; x++){
			GController.initGame(E_GAME_MODE.KI_INTERNAL,Level.WARN);
			GController.startGame();
			while(gameRunning()){
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					logger.error(e);
				}
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
