package test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Random;
import java.util.ResourceBundle.Control;

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
	
	@Test
	public void test_X_WIN(){
		logger.entry();
		test_init(E_GAME_MODE.KI_INTERNAL);
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
		test_init(E_GAME_MODE.KI_INTERNAL);
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
		test_init(E_GAME_MODE.KI_INTERNAL);
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
		test_init(E_GAME_MODE.KI_INTERNAL);
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
	public void test_fuzzer(){
		long time = System.currentTimeMillis();
		int games = 100;
		long moves = 0;
		int win_a = 0;
		int win_b = 0;
		int draw = 0;
		int lowest_moves_draw = 39;
		E_GAME_MODE gamemode = E_GAME_MODE.SINGLE_PLAYER;
		StringBuilder sb = new StringBuilder();
		for(int x = 0; x < games; x++){
			test_init(gamemode);
			Random rand = new Random(System.nanoTime());
			while(Controller.getGameState() == E_GAME_STATE.PLAYER_A || Controller.getGameState() == E_GAME_STATE.PLAYER_B){
				//Controller.printGameState();
				Controller.insertStone(rand.nextInt(7));
				moves++;
			}
			if (Controller.getGameState() == E_GAME_STATE.WIN_A ){
				win_a++;
			}else if (Controller.getGameState() == E_GAME_STATE.WIN_B ){
				win_b++;
			}else if(Controller.getGameState() == E_GAME_STATE.DRAW){
				if(Controller.getMoves() < lowest_moves_draw){
					sb.append(Controller.getprintedGameState());
					sb.append("\n");
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
	}
}
