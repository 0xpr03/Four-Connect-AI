package test;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.GController;
import gamelogic.Controller.E_GAME_MODE;
import gamelogic.Controller.E_GAME_STATE;

public class AITest_Interact {
	
	private static Logger logger = LogManager.getLogger();
	
	private static void init(){
		GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai");
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
		if(System.console() == null){
			System.err.println("No console!!");
			System.exit(1);
		}
		while(gameRunning()){
			logger.info("game running");
			if(GController.getGameState() == E_GAME_STATE.PLAYER_A){
				System.out.println(GController.getprintedGameState());
				System.out.println("Please select a column, 0-6");
				String input = System.console().readLine();
				try{
					int in = Integer.parseInt(input);
					if(!GController.insertStone(in)){
						System.out.println("Not in range!");
					}
				}catch(NumberFormatException e){
					System.out.println("Not a number!");
				}
			}
		}
		System.out.println(GController.getprintedGameState());
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("{}",GController.getGameState());
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
