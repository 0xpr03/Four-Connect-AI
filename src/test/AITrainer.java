package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.Controller;
import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;
import gamelogic.AI.KBS_trainer;
import gamelogic.AI.mariaDB;

/**
 * AI tester & trainer
 * @author Aron Heinecke
 *
 */
public class AITrainer {
	
	private static Logger logger = LogManager.getLogger();
	private static long lastmatch;
	private static long start_time;
	
	private static long restarts = 0;
	private static long moves = 0;
	
	public static void main(String[] args){ // external logger, starting player [a,b], first move
		if(args.length > 1){
			if(!args[0].equals("none")){
				LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
				File file = new File(args[0]);
				context.setConfigLocation(file.toURI());
			}
		}
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
		
		int first_move = 3;
		if(args.length > 3){
			try{
				first_move = Integer.valueOf(args[2]);
			}catch(NumberFormatException e){
				logger.error("Invalid first move!");
			}
		}
		initController("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai", player == E_GAME_STATE.PLAYER_A, first_move);
		run(player,first_move);
		
		GController.shutdown();
		logger.exit();
	}
	
	private static void run(E_GAME_STATE starting_pl, int first_move){
		logger.entry();
//		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
//		final Configuration config = ctx.getConfiguration();
//		Map<String, LoggerConfig> appenders = config.getLoggers();
//		if(appenders.containsKey("Root")){
//			List<AppenderRef> j = appenders.get("Console").getAppenderRefs();
//			for(AppenderRef x : j){
//				if(x.getRef().equals("Console"))
//					x.
//			}
//		}
		registerExitFunction();
		Level logcontroller = Level.WARN;
		Configurator.setLevel("DB", Level.INFO);
		Configurator.setLevel("AI", Level.INFO);
		Configurator.setLevel("Controller", Level.INFO);
		
		start_time=System.currentTimeMillis();
		initGame(logcontroller, starting_pl);
		logger.info("Starting player: {}",starting_pl);
		
		@SuppressWarnings("unused")
		boolean runthrough = false;
		while(gameRunning()){
			moves++;
//			try {
//				if(runthrough == false){
//					logger.info(GController.getprintedGameState());
//					String l = readLine("Insert 1 to continue");
//					if(l.equals("C")){
//						runthrough = true;
//					}
//					if(!l.equals("1")){
//						continue;
//					}
//				}
//			} catch (IOException e) {
//				logger.error("{}",e);
//				e.printStackTrace();
//			}
			lastmatch = System.currentTimeMillis();
			switch(GController.getGameState()){
			case PLAYER_A:
				GController.moveAI_A();
				break;
			case PLAYER_B:
				GController.moveAI_B();
				break;
			case RESTART:
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					logger.error("{}",e);
				}
				initGame(logcontroller, starting_pl);
				restarts++;
				break;
			default:
				logger.info(GController.getGameState());
				break;
			}
		}
		logger.info(GController.getprintedGameState());
	}
	
	private static void initGame(Level logcontroller, E_GAME_STATE starting_pl){
		GController.initGame(E_GAME_MODE.KI_TRAINING,logcontroller);
		GController.startGame();
		GController.setState(starting_pl);
	}
	
	private static void initController(String address, int port, String user, String pw, String db, boolean player_a, int first_move){
		KBS_trainer AIA = new KBS_trainer(new mariaDB(address, port, user, pw, db),player_a == true ? first_move : -1);
		KBS_trainer AIB = new KBS_trainer(new mariaDB(address, port, user, pw, db),player_a == false ? first_move : -1);
		Controller controller = new Controller(AIA, AIB);
		GController.init(controller);
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
				logger.info("Took {}ms and {} moves including {} restart and re-moves",System.currentTimeMillis()-start_time,moves,restarts);
				GController.shutdown();
				logger.exit();
			}
		});
	}
	
	@SuppressWarnings("unused")
	private static String readLine(String format, Object... args) throws IOException {
	    if (System.console() != null) {
	        return System.console().readLine(format, args);
	    }
	    System.out.print(String.format(format, args));
	    BufferedReader reader = new BufferedReader(new InputStreamReader(
	            System.in));
	    return reader.readLine();
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
