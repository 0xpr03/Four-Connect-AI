package test;

import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;
import gamelogic.AI.MemCache;
import gamelogic.AI.lib;

/**
 * Cache test for KBS<br>
 * Validates that inserted Byte arrays are staying the same, hash wise
 * @author Aron Heinecke
 */
public class cacheTest {

	Logger logger = LogManager.getLogger();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		GController.init();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		GController.initGame(E_GAME_MODE.TESTING,7,6);
		GController.startGame();
		lib lib = new lib();
		MemCache<ByteBuffer, Long> cache = new MemCache<ByteBuffer,Long>(2, 1, 10000);
		
		byte[] byte_1 = lib.field2sha(GController.getFieldState());
		cache.put(ByteBuffer.wrap(lib.field2sha(GController.getFieldState())), 1L);
		
		GController.insertStone(1);
		byte[] byte_2 = lib.field2sha(GController.getFieldState());
		cache.put(ByteBuffer.wrap(lib.field2sha(GController.getFieldState())), 2L);
		
		GController.insertStone(2);
		byte[] byte_3 = lib.field2sha(GController.getFieldState());
		cache.put(ByteBuffer.wrap(lib.field2sha(GController.getFieldState())), 3L);
		assertTrue("get default array",1L == cache.get(ByteBuffer.wrap(byte_1)));
		assertTrue("get string bytes", 2L == cache.get(ByteBuffer.wrap(byte_2)));
		assertTrue("get string bytes", 3L == cache.get(ByteBuffer.wrap(byte_3)));
	}
	
	@Test
	public void performanceTest(){
		Configurator.setLevel("Controller", Level.WARN);
		GController.initGame(E_GAME_MODE.MULTIPLAYER, 7,6);
		GController.startGame();
		MemCache<ByteBuffer, Long> cache = new MemCache<ByteBuffer,Long>(2, 1, 10000);
		lib lib = new lib();
		Random randomGenerator = new Random(System.nanoTime());
		final int games = 10000;
		long moves = 0;
		
		long time = System.currentTimeMillis();
		for(int i = 0; i < games; i++){
			GController.initGame(E_GAME_MODE.MULTIPLAYER, Level.WARN,7,6);
			GController.startGame();
			while(GController.getGameState() == E_GAME_STATE.PLAYER_A || GController.getGameState() == E_GAME_STATE.PLAYER_B){
				if(GController.insertStone(randomGenerator.nextInt(GController.getX_MAX()))){
					moves++;
					cache.put(ByteBuffer.wrap(lib.field2sha(GController.getFieldState())), moves);
				}
			}
		}
		logger.info("Took {}ms for {} games, {} moves",System.currentTimeMillis() - time, games, moves);
		logger.info("Last game move: {}",cache.get(ByteBuffer.wrap(lib.field2sha(GController.getFieldState()))));
	}

}
