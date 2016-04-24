package runnables;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.Controller;
import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;
import gamelogic.AI.MemCache;
import gamelogic.AI.mariaDB;
import gamelogic.AI.learning.KBS_Channel;
import gamelogic.AI.learning.KBS_trainer;

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
	
	public static void main(String[] args){ // external logger, starting player [a,b], first move, [non-allowed moves]
		if(args.length > 0){
			if(!args[0].equals("none")){
				LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
				File file = new File(args[0]);
				context.setConfigLocation(file.toURI());
			}
		}
		E_GAME_STATE player = E_GAME_STATE.PLAYER_A;
		if(args.length > 1){
			switch(args[1]){
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
		
		int X_MAX = 5;
		int Y_MAX = 4;
		
		int first_move = -1;
		if(args.length > 1){
			X_MAX = 6;
			Y_MAX = 6;
			first_move = -1;
			logger.warn("Detected args, using {}*{} field!",X_MAX,Y_MAX);
		}
		if(args.length > 2){
			try{
				first_move = Integer.valueOf(args[2]);
				logger.info("Starting move: {}",first_move);
			}catch(NumberFormatException e){
				logger.error("Invalid first move!");
			}
		}
		ArrayList<Integer> forbiddenMoves = new ArrayList<Integer>();
		if(args.length > 3 ) {
			String[] forbidden_moves = args[3].split(",");
			for(String s : forbidden_moves){
				if(s.equals(""))
					continue;
				try{
					forbiddenMoves.add(Integer.valueOf(s));
				}catch(NumberFormatException e){
					logger.error("Invalid arg for forbidden move! {}",s);
				}
			}
		}
		
		initController("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai", player == E_GAME_STATE.PLAYER_A, first_move,forbiddenMoves);
		try{
			run(player,first_move,X_MAX, Y_MAX);
		} catch(Exception e){
			logger.fatal("Error on run: {}",e);
		}
		
		GController.shutdown();
		logger.exit();
	}
	
	private static void run(E_GAME_STATE starting_pl, int first_move, int x_max, int y_max){
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
		initGame(logcontroller, starting_pl, x_max, y_max);
		logger.info("Starting player: {}",starting_pl);
		
		@SuppressWarnings("unused")
		boolean runthrough = false;
		while(true){
			moves++;
//			try {
//				if(runthrough == false){
//					logger.info(GController.getprintedGameState());
//					String l = lib.readLine("Insert 1 to continue");
//					if(l.equals("C")){
//						System.out.println("Disabling manual mode!");
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
					Thread.sleep(200);
				} catch (InterruptedException e) {
					logger.error("{}",e);
				}
				initGame(logcontroller, starting_pl, x_max, y_max);
				restarts++;
				break;
			default:
				logger.info(GController.getGameState());
				break;
			}
		}
	}
	
	private static void initGame(Level logcontroller, E_GAME_STATE starting_pl,int x_max,int y_max){
		GController.initGame(E_GAME_MODE.KI_TRAINING,logcontroller,x_max, y_max);
		GController.startGame();
		GController.setState(starting_pl);
	}
	
	private static void initController(String address, int port, String user, String pw, String db, boolean player_a, int first_move, List<Integer> forbiddenMoves){
		MemCache<ByteBuffer,Long> cache = new MemCache<ByteBuffer, Long>(20, 30, 50000);
		KBS_Channel channel = new KBS_Channel();
		KBS_trainer AIA = new KBS_trainer(new mariaDB(address, port, user, pw, db,cache),player_a == true ? first_move : -1,forbiddenMoves,channel);
		KBS_trainer AIB = new KBS_trainer(new mariaDB(address, port, user, pw, db,cache),player_a == false ? first_move : -1,forbiddenMoves,channel);
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
					System.out.println("Gamestate: "+GController.getGameState());
					logger.info("Took {}ms and {} moves including re-moves and {} restarts",System.currentTimeMillis()-start_time,moves,restarts);
				}catch(Exception e){
					logger.error("Trying to get state: {}",e);
				}
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
