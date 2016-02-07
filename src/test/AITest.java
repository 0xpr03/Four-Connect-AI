package test;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
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
		if(args.length > 0){
			LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
			File file = new File(args[0]);
			context.setConfigLocation(file.toURI());
		}
		GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai");
		
		Level level_db = Level.WARN;
		Level level_ai = Level.WARN;
		int games = 50;
		
		
		if(args.length > 1){
			games = Integer.parseInt(args[1]);
		}
		run(level_db,level_ai,games);
		logger.exit();
	}
	
	private static void run(Level level_db, Level level_ai, int games){
		logger.entry();
		long win_a = 0;
		long win_b = 0;
		long draw = 0;
		registerExitFunction();
		if(games > 1000){
			level_db = Level.WARN;
			level_ai = Level.WARN;
		}
		Configurator.setLevel("DB", level_db);
		Configurator.setLevel("AI", level_ai);
		for(int x = 0; x < games; x++){
//			if(x % 1000 == 0){
//				logger.info(x);
//			}
			GController.initGame(E_GAME_MODE.KI_INTERNAL,Level.INFO);
			GController.startGame();
			while(gameRunning()){
				switch(GController.getGameState()){
				case PLAYER_A:
					GController.moveAI_A();
					break;
				case PLAYER_B:
					GController.moveAI_B();
					break;
				default:
				}
			}
			switch(GController.getGameState()){
			case DRAW:
				draw++;
				break;
			case WIN_A:
				win_a++;
				break;
			case WIN_B:
				win_b++;
				break;
			case RESTART:  // datarace, table locking etc
				x--;
				break;
			default:
				break;
			}
			//logger.info(()->GController.getGameState());
			//logger.info(()->GController.getprintedGameState());
		}
		logger.info(GController.getprintedGameState());
		logger.info("Wins A:{} B:6{} Draws:{}",win_a,win_b,draw);
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
