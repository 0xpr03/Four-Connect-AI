package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gamelogic.Controller;
import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;
import gamelogic.AI.Move;
import gamelogic.AI.SelectResult;
import gamelogic.AI.lib;
import gamelogic.AI.mariaDB;
import gamelogic.AI.mariaDB_simple;

public class gameControllerTester {
	
	private Logger logger = LogManager.getLogger();
	
	@BeforeClass
	public static void setupController() throws Exception {
		GController.init();
	}

	@AfterClass
	public static void destroy() throws Exception {
		//GAME_MODE
	}
	
	public void test_init(E_GAME_MODE gamemode){
		GController.initGame(gamemode);
		GController.startGame();
		assertTrue("controller start state",E_GAME_STATE.START != GController.getGameState());
		assertTrue("controller start state",E_GAME_STATE.NONE != GController.getGameState());
	}
	
	private void init_test(){
		GController.initGame(E_GAME_MODE.TESTING,Level.TRACE);
		GController.startGame();
		GController.setState(E_GAME_STATE.PLAYER_A);
		logger.info(GController.getGameState());
		assertEquals("controller start state",E_GAME_STATE.PLAYER_A, GController.getGameState());
	}
	
	@Test
	public void get_hash(){
		lib lib = new lib();
		String input = "----\n----\nOXOX\nXXOO";
		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		GController.startGame();
		GController.D_setField(GController.D_parseField(input));
		Move move = new Move(lib.field2sha(GController.getFieldState()),1L, 1, true);
		logger.info("Hash: {}",move.toString());
		assertTrue(E_GAME_STATE.NONE != GController.getGameState());
	}
	
	@Test
	public void test_X_WIN(){
		logger.entry();
		init_test();
		GController.insertStone(0);
		GController.insertStone(6);
		GController.insertStone(1);
		GController.insertStone(6);
		GController.insertStone(2);
		GController.insertStone(6);
		GController.insertStone(3);
		assertEquals("already won", false,GController.insertStone(0));
		
		GController.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, GController.getGameState());
	}
	
	@Test
	public void test_Y_WIN(){
		logger.entry();
		init_test();
		GController.insertStone(0);
		GController.insertStone(1);
		GController.insertStone(0);
		GController.insertStone(2);
		GController.insertStone(0);
		GController.insertStone(6);
		GController.insertStone(0);
		assertEquals("already won", false,GController.insertStone(0));
		assertEquals("already won", false,GController.insertStone(0));
		
		GController.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, GController.getGameState());
	}
	
	@Test
	public void test_XYPM_WIN(){
		logger.entry();
		init_test();
		GController.insertStone(0);// A
		GController.insertStone(1);// B
		GController.insertStone(1);// A
		GController.insertStone(3);// B
		GController.insertStone(2);// A
		GController.insertStone(2);// B
		GController.insertStone(2);// A
		GController.insertStone(0);// B
		GController.insertStone(3);// A
		GController.insertStone(3);// B
		GController.insertStone(3);// A
		assertEquals("already won", false,GController.insertStone(0));
		
		GController.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, GController.getGameState());
	}
	
	@Test
	public void test_XPWYPM_WIN(){
		logger.entry();
		init_test();
		GController.insertStone(5);// A
		GController.insertStone(2);// B
		GController.insertStone(3);// A
		GController.insertStone(3);// B
		GController.insertStone(3);// A
		GController.insertStone(4);// B
		GController.insertStone(2);// A
		GController.insertStone(2);// B
		GController.insertStone(2);// A
		GController.insertStone(0);// B
		GController.insertStone(4);// A
		GController.printGameState();
		assertEquals("already won", false,GController.insertStone(0));
		
		GController.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, GController.getGameState());
	}
	
	@Test
	public void test_win(){
		Configurator.setLevel("DB", Level.WARN);
		Configurator.setLevel("AI", Level.WARN);
		String input = "-OOXXOO\n-XXOOXX\n-OOXXOO\n-XXOOXX\n-OOOXOO\n-XXOXXX";
		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		GController.startGame();
		GController.D_setField(GController.D_parseField(input));
		assertEquals("no draw check",false,GController.D_analyzeField());
	}
	
	@Test
	public void test_AI(){ // 4*4
		if(GController.getY_MAX() == 4 && GController.getX_MAX() == 4){
			GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai", false);
			String[] fields_used = {"X-O-\nO-X-\nOOX-\nOXXO"};
			mariaDB mdb = new mariaDB("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai");
			GController.initGame(E_GAME_MODE.TESTING);
			GController.startGame();
			Move move = new Move(null,1L,1,false);
			lib lib = new lib();
			for(String s : fields_used){
				GController.D_setField(GController.D_parseField(s));
				logger.info("field hash {}",move.bytesToHex(lib.field2sha(GController.getFieldState())));
				boolean not_found = true;
				StringBuilder sb = new StringBuilder();
				SelectResult sr = mdb.testField(GController.getFieldState());
				if(!sr.isEmpty()){
					not_found = false;
					for(Move m : sr.getUnused()){
						sb.append(m.toString());
						sb.append("\n");
					}
				}
				if(not_found){
					logger.error("Field {} not found!",s);
				}else{
					logger.info("Found: \n{} for {}",sb.toString(),GController.getprintedGameState());
				}
			}
		}
	}
	
	@Test
	public void test_draw(){
//		{
//		String input = "XXX-OOO\nOOO-XXX\nXXX-OXO\nOXO-OOX\nXOO-XXX\nXOO-OOX";
//		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
//		GController.startGame();
//		GController.D_setField(GController.D_parseField(input));
//		assertEquals("no draw check",false,GController.D_analyzeField());
//		}
//		{
//		String input = "X-OOX--\nXXXOX--\nOOOXOOO\nOXOOOXX\nOXOXXOO\nXXXOXXX";
//		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
//		GController.startGame();
//		GController.D_setField(GController.D_parseField(input));
//		assertEquals("no draw check",false,GController.D_analyzeField());
//		}
//		{
//		String input = "--OOX--\nX-XOXX-\nOOXXOOO\nOXOXOXX\nOXOXXOO\nXXXOXXX";
//		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
//		GController.startGame();
//		GController.D_setField(GController.D_parseField(input));
//		assertEquals("no draw check",false,GController.D_analyzeField());
//		}
//		{
//		String input = "OX--OX-\nOO--OO-\nXXXOXXX\nOOOXXOO\nXXXOOOX\nXOOOXXX";
//		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
//		GController.startGame();
//		GController.D_setField(GController.D_parseField(input));
//		assertEquals("no draw check",false,GController.D_analyzeField());
//		}
//		{
//		String input = "-------\n--OXO-X\nXXOOOXX\nOOXXXOO\nXOOOXXX\nOXXXOXX";
//		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
//		GController.startGame();
//		GController.D_setField(GController.D_parseField(input));
//		assertEquals("no draw check",false,GController.D_analyzeField());
//		}
		{
		String input = "XOX-\nOXO-\nOOXX\nOXXO";
		GController.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		GController.startGame();
		GController.D_setField(GController.D_parseField(input));
		assertEquals("no draw check",false,GController.D_analyzeField());
		}
	}
	
	@Test
	public void test_fuzzer(){
		class tResult{
			public int draws;
			public String draws_string;
			public String tiniest_draw;
			public int tiniest_draw_moves;
			public int wins_a;
			public int wins_b;
			public long time;
			public long moves;
			public tResult(String draws_string, String tinies_draw, int tiniest_draw_moves, int wins_a, int wins_b,int draws, long moves, long time){
				this.draws = draws;
				this.tiniest_draw = tinies_draw;
				this.tiniest_draw_moves = tiniest_draw_moves;
				this.wins_a = wins_a;
				this.wins_b = wins_b;
				this.moves = moves;
				this.time = time;
				this.draws_string = draws_string;
			}
		}
		try{
		final long games = 1000;
		final int lowest_moves_draw = 35; // border, everything below will be logged
		
		final E_GAME_MODE gamemode = E_GAME_MODE.FUZZING;
		int processors = Runtime.getRuntime().availableProcessors();
		final long games_per_thread = games / processors;
		
		logger.info("Starting fuzzing test, this will take some time..");
		logger.info("Using {} threads, {} games per thread. ( ={} Games) ",processors,games_per_thread,games_per_thread*processors);
		ExecutorService workers = Executors.newCachedThreadPool();
		Collection<Callable<tResult>> tasks = new ArrayList<Callable<tResult>>();
		long time_real = System.currentTimeMillis();
		for (int x = 0; x < processors; x++) {
		  tasks.add(new Callable<tResult>()
		  {

		    public tResult call() throws Exception {
		    	long moves = 0;
				int wins_a = 0;
				int wins_b = 0;
				int draws = 0;
				int lowest_draw = 42;
				StringBuilder draws_string = new StringBuilder();
				String lowest_field_draw = "";
				long time = System.currentTimeMillis();
				for(int x = 0; x < games_per_thread; x++){
					@SuppressWarnings({ "rawtypes" })
					Controller controller = new Controller();
					controller.initGame(gamemode, Level.WARN);
					controller.startGame();
					Random rand = new Random(System.nanoTime());
					while(controller.getGameState() == E_GAME_STATE.PLAYER_A || controller.getGameState() == E_GAME_STATE.PLAYER_B){
						controller.insertStone(rand.nextInt(7));
						moves++;
					}
					
					if (controller.getGameState() == E_GAME_STATE.WIN_A ){
						wins_a++;
					}else if (controller.getGameState() == E_GAME_STATE.WIN_B ){
						wins_b++;
					}else if(controller.getGameState() == E_GAME_STATE.DRAW){
						if(controller.getMoves() < lowest_moves_draw){
							draws_string.append(controller.getprintedGameState());
							draws_string.append("\n");
							if(controller.getMoves() < lowest_draw){
								lowest_draw = controller.getMoves();
								lowest_field_draw = controller.getprintedGameState();
							}
						}
						draws++;
					}else{
						logger.error("Unknown state: {}",controller.getGameState());
						System.exit(1);
					}
				}
				return new tResult(draws_string.toString(), lowest_field_draw, lowest_draw, wins_a, wins_b, draws, moves, System.currentTimeMillis() - time);
		    }
		  });
		}
		
		long moves = 0;
		long wins_a = 0;
		long wins_b = 0;
		long draws = 0;
		long time_total = 0;
		int lowest_draw = 99;
		StringBuilder draws_string = new StringBuilder();
		String lowest_draw_str = "";
		List<Future<tResult>> results = workers.invokeAll(tasks);
		for (Future<tResult> f : results) {
			tResult res = f.get();
			moves += res.moves;
			wins_a += res.wins_a;
			wins_b += res.wins_b;
			time_total += res.time;
			draws += res.draws;
			draws_string.append(res.draws_string);
			if (lowest_draw > res.tiniest_draw_moves){
				lowest_draw = res.tiniest_draw_moves;
				lowest_draw_str = res.tiniest_draw;
			}
		}
		time_real = System.currentTimeMillis() - time_real;
		logger.info("Computed {} games, in {} ms, {} moves",games,time_real, moves);
		logger.info("Single threaded this would've taken {} ms",time_total);
		logger.info("Gamemode simulated: {}",gamemode);
		logger.info("Wins A:{} B:{} Draws:{}",wins_a,wins_b,draws);
		logger.info("Drawgames with moves < {}: \n{}",lowest_moves_draw,draws_string.toString());
		logger.info("Lowest move count for a draw: {}\n{}",lowest_draw,lowest_draw_str);
		}catch(InterruptedException | ExecutionException e){
			logger.fatal("Error: {}",e);
		}

	}
}
