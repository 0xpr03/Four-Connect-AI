package test;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.newdawn.slick.state.GameState;

import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;

/**
 * AI tester & trainer
 * @author Aron Heinecke
 *
 */
public class AITrainer {
	
	private static Logger logger = LogManager.getLogger();
	private static long lastmatch;
	private static long start_time;
	
	private static long moves = 0;
	
	public static void main(String[] args){ // external logger, length, starting player [a,b]
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
		E_GAME_STATE player = E_GAME_STATE.PLAYER_B;
		if(args.length > 2){
			switch(args[2]){
			case "a":
				player = E_GAME_STATE.PLAYER_A;
				break;
			case "b":
				player = E_GAME_STATE.PLAYER_B;
				break;
			default:
				logger.warn("Unknown input for starting player: {}",args[2]);
			}
		}
		run(level_db,level_ai,games,player);
		
		GController.shutdown();
		logger.exit();
	}
	
	private static void run(Level level_db, Level level_ai, int games,E_GAME_STATE starting_pl){
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
		
		start_time=System.currentTimeMillis();
		GController.initGame(E_GAME_MODE.KI_TRAINING,logcontroller);
		GController.startGame();
		GController.setState(starting_pl);
		logger.info("Starting player: {}",starting_pl);
		while(gameRunning()){
			//logger.info(GController.getprintedGameState());
			moves++;
			lastmatch = System.currentTimeMillis();
			switch(GController.getGameState()){
			case PLAYER_A:
				GController.moveAI_A();
				break;
			case PLAYER_B:
				GController.moveAI_B();
				break;
			default:
				logger.info(GController.getGameState());
				break;
			}
		}
		logger.info(GController.getprintedGameState());
	}
	
	private static void registerExitFunction() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("Last move {}ms ago",System.currentTimeMillis() - lastmatch);
				try{
					logger.info("Current state: {}",GController.getGameState());
				}catch(Exception e){
					logger.error("Trying to get state: {}",e);
				}
				logger.info("Took {}ms and {} moves including re-moves",System.currentTimeMillis()-start_time,moves);
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
