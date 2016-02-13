package test;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.ControllerBase.E_GAME_MODE;
import net.java.games.input.Controller;
import gamelogic.GController;

/**
 * AI tester & trainer
 * @author Aron Heinecke
 *
 */
public class AITrainer {
	
	private static Logger logger = LogManager.getLogger();
	private static long lastmatch;
	
	private static long win_a = 0;
	private static long win_b = 0;
	private static long draw = 0;
	private static long restarts = 0;
	
	public static void main(String[] args){ // external logger, length
		if(args.length > 1){
			if(!args[0].equals("none")){
				LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
				File file = new File(args[0]);
				context.setConfigLocation(file.toURI());
			}
		}
		GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai");
		
		Level level_db = Level.INFO;
		Level level_ai = Level.INFO;
		int games = 1;
		
		if(args.length > 1){
			games = Integer.parseInt(args[1]);
		}else if(args.length > 0){
			games = Integer.parseInt(args[0]);
		}
		logger.info("Games {}",games);
		if(args.length > 1 &&  (GController.getX_MAX() != 7 || GController.getY_MAX() != 6)){
			// protection from running invalid field evaluations on the server
			logger.error("Stopping, field size modified!");
		}
		run(level_db,level_ai,games);
		
		GController.shutdown();
		logger.exit();
	}
	
	private static void run(Level level_db, Level level_ai, int games){
		logger.entry();
		
		registerExitFunction();
		Level logcontroller = Level.INFO;
		if(games > 50){
			level_db = Level.INFO;
			level_ai = Level.WARN;
			logcontroller = Level.WARN;
		}
		Configurator.setLevel("DB", level_db);
		Configurator.setLevel("AI", level_ai);
		Configurator.setLevel("Controller", Level.WARN);
		for(int x = 0; x < games; x++){
			GController.initGame(E_GAME_MODE.KI_TRAINING,logcontroller);
			GController.startGame();
			while(gameRunning()){
				//logger.info(GController.getprintedGameState());
				switch(GController.getGameState()){
				case PLAYER_A:
					GController.moveAI_A();
					break;
				case PLAYER_B:
					GController.moveAI_B();
					break;
				default:
					logger.info(GController.getGameState());
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
				restarts++;
				break;
			default:
				break;
			}
			lastmatch = System.currentTimeMillis();
		}
		logger.info(GController.getprintedGameState());
		logger.info("Wins A:{} B:{} Draws:{} Restarts:{}",win_a,win_b,draw,restarts);
	}
	
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("Last match {}ms ago",System.currentTimeMillis() - lastmatch);
				try{
					logger.info("Current state: {}",GController.getGameState());
				}catch(Exception e){
					logger.error("Trying to get state: {}",e);
				}
				logger.info("Wins A:{} B:{} Draws:{} Restarts:{}",win_a,win_b,draw,restarts);
				GController.shutdown();
				logger.exit();
			}
		});
	}
	
	/**
	 * Returns false if draw,win_a,win_b is the current gamestate
	 * true otherwise
	 * @return
	 */
	private static boolean gameRunning(){
//		switch(GController.getGameState()){
//		case DRAW:
//		case WIN_A:
//		case WIN_B:
//		case RESTART:
//			return false;
//		default:
			return true;
//		}
	}
}
