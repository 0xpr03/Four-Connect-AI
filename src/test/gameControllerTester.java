package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gamelogic.Controller;
import gamelogic.Controller.E_GAME_MODE;
import gamelogic.Controller.E_GAME_STATE;

public class gameControllerTester {
	
	private Logger logger = LogManager.getLogger();
	
	@BeforeClass
	public static void setupController() throws Exception {
		
	}

	@AfterClass
	public static void destroy() throws Exception {
		//GAME_MODE
	}
	
	public void test_init(E_GAME_MODE gamemode){
		Controller.initGame(gamemode);
		Controller.startGame();
		assertTrue("controller start state",E_GAME_STATE.START != Controller.getGameState());
		assertTrue("controller start state",E_GAME_STATE.NONE != Controller.getGameState());
	}
	
	private void init_test(){
		Controller.initGame(E_GAME_MODE.TESTING);
		Controller.startGame();
		Controller.setState(E_GAME_STATE.PLAYER_A);
		assertEquals("controller start state",E_GAME_STATE.PLAYER_A, Controller.getGameState());
	}
	
	@Test
	public void test_X_WIN(){
		logger.entry();
		init_test();
		Controller.insertStone(0);
		Controller.insertStone(6);
		Controller.insertStone(1);
		Controller.insertStone(6);
		Controller.insertStone(2);
		Controller.insertStone(6);
		Controller.insertStone(3);
		assertEquals("already won", false,Controller.insertStone(0));
		
		Controller.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, Controller.getGameState());
	}
	
	@Test
	public void test_Y_WIN(){
		logger.entry();
		init_test();
		Controller.insertStone(0);
		Controller.insertStone(1);
		Controller.insertStone(0);
		Controller.insertStone(2);
		Controller.insertStone(0);
		Controller.insertStone(6);
		Controller.insertStone(0);
		assertEquals("already won", false,Controller.insertStone(0));
		assertEquals("already won", false,Controller.insertStone(0));
		
		Controller.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, Controller.getGameState());
	}
	
	@Test
	public void test_XYPM_WIN(){
		logger.entry();
		init_test();
		Controller.insertStone(0);// A
		Controller.insertStone(1);// B
		Controller.insertStone(1);// A
		Controller.insertStone(3);// B
		Controller.insertStone(2);// A
		Controller.insertStone(2);// B
		Controller.insertStone(2);// A
		Controller.insertStone(0);// B
		Controller.insertStone(3);// A
		Controller.insertStone(3);// B
		Controller.insertStone(3);// A
		assertEquals("already won", false,Controller.insertStone(0));
		
		Controller.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, Controller.getGameState());
	}
	
	@Test
	public void test_XPWYPM_WIN(){
		logger.entry();
		init_test();
		Controller.insertStone(5);// A
		Controller.insertStone(2);// B
		Controller.insertStone(3);// A
		Controller.insertStone(3);// B
		Controller.insertStone(3);// A
		Controller.insertStone(4);// B
		Controller.insertStone(2);// A
		Controller.insertStone(2);// B
		Controller.insertStone(2);// A
		Controller.insertStone(0);// B
		Controller.insertStone(4);// A
		Controller.printGameState();
		assertEquals("already won", false,Controller.insertStone(0));
		
		Controller.printGameState();
		assertEquals("state win x", E_GAME_STATE.WIN_A, Controller.getGameState());
	}
	
	@Test
	public void test_draw(){
		{
		String input = "OXO-XXO\nXXX-OOO\nOOO-XXX\nXXX-OXO\nOXO-OOX\nXOO-XXX\nXOO-OOX";
		Controller.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		Controller.startGame();
		Controller.D_setField(Controller.D_parseField(input));
		assertEquals("no draw check",false,Controller.D_analyzeField());
		}
		{
		String input = "X-OOX--\nXXXOX--\nOOOXOOO\nOXOOOXX\nOXOXXOO\nXXXOXXX\nOOOXXOX";
		Controller.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		Controller.startGame();
		Controller.D_setField(Controller.D_parseField(input));
		assertEquals("no draw check",false,Controller.D_analyzeField());
		}
		{
		String input = "--OOX--\nX-XOXX-\nOOXXOOO\nOXOXOXX\nOXOXXOO\nXXXOXXX\nOOOXXOX";
		Controller.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		Controller.startGame();
		Controller.D_setField(Controller.D_parseField(input));
		assertEquals("no draw check",false,Controller.D_analyzeField());
		}
		{
		String input = "OX--OX-\nOO--OO-\nXXXOXXX\nOOOXXOO\nXXXOOOX\nXOOOXXX\nXOXOOOX";
		Controller.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		Controller.startGame();
		Controller.D_setField(Controller.D_parseField(input));
		assertEquals("no draw check",false,Controller.D_analyzeField());
		}
		{
		String input = "-------\n--OXO-X\nXXOOOXX\nOOXXXOO\nXOOOXXX\nOXXXOXX\nOOXOXOO";
		Controller.initGame(E_GAME_MODE.TESTING, Level.TRACE);
		Controller.startGame();
		Controller.D_setField(Controller.D_parseField(input));
		assertEquals("no draw check",false,Controller.D_analyzeField());
		}
	}
	
	@Test
	public void test_fuzzer(){
		final int games = 100000000;
		final int amount_samples = 100;
		long moves = 0;
		int win_a = 0;
		int win_b = 0;
		int draw = 0;
		int lowest_moves_draw = 41;
		int lowest_draw = 41;
		int lowest_moves_win = 44;
		int lowest_win = 44;
		final E_GAME_MODE gamemode = E_GAME_MODE.FUZZING;
		StringBuilder sb = new StringBuilder();
		StringBuilder spot_samples = new StringBuilder();
		String lowest_field_draw = "";
		String lowest_field_win = "";
		final int spotdivider = games/amount_samples;
		logger.info("Starting fuzzing test, this will take some time..");
		long time = System.currentTimeMillis();
		for(int x = 0; x < games; x++){
			Controller.initGame(gamemode, Level.WARN);
			Controller.startGame();
			Random rand = new Random(System.nanoTime());
			while(Controller.getGameState() == E_GAME_STATE.PLAYER_A || Controller.getGameState() == E_GAME_STATE.PLAYER_B){
				//Controller.printGameState();
				Controller.insertStone(rand.nextInt(7));
				moves++;
			}
			if (x % spotdivider == 0){
				spot_samples.append(Controller.getprintedGameState());
				spot_samples.append("\n");
			}
			
			if (Controller.getGameState() == E_GAME_STATE.WIN_A ){
				win_a++;
				if(Controller.getMoves() < lowest_win){
					lowest_win = Controller.getMoves();
					lowest_field_win = Controller.getprintedGameState();
				}
			}else if (Controller.getGameState() == E_GAME_STATE.WIN_B ){
				win_b++;
				if(Controller.getMoves() < lowest_win){
					lowest_win = Controller.getMoves();
					lowest_field_win = Controller.getprintedGameState();
				}
			}else if(Controller.getGameState() == E_GAME_STATE.DRAW){
				if(Controller.getMoves() < lowest_moves_draw){
					sb.append(Controller.getprintedGameState());
					sb.append("\n");
					if(Controller.getMoves() < lowest_draw){
						lowest_draw = Controller.getMoves();
						lowest_field_draw = Controller.getprintedGameState();
					}
				}
				draw++;
			}else{
				logger.error("Unknown state: {}",Controller.getGameState());
				System.exit(1);
			}
		}
		logger.info("Computed {} games, in {} ms, {} moves",games,System.currentTimeMillis()-time, moves);
		logger.info("Gamemode simulated: {}",gamemode);
		logger.info("Wins A:{} B:{} Draws:{}",win_a,win_b,draw);
		logger.info("Drawgames with moves < {}: \n{}",lowest_moves_draw,sb.toString());
		logger.info("Lowest move count for a draw: {}\n{}",lowest_draw,lowest_field_draw);
		logger.info("Lowest move count for a win: {}\n{}",lowest_win,lowest_field_win);
		logger.info("Spot samples: \n{}",spot_samples.toString());
	}
}
