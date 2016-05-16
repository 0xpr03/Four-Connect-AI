package test;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_PLAYER;
import gamelogic.GController;
import gamelogic.AI.WebPlayer;

public class webPlayerTest {
	private static Logger logger = LogManager.getLogger();
	
	private static JSONParser parser;
	private static String JSON = "{\"error\":false,\"moves\":[1,2,3,4]}";
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		parser = new JSONParser();
		GController.init();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@SuppressWarnings("unchecked")
	@Test
	public void jsonContainsTest() throws ParseException {
		HashMap<String, Object> map = (HashMap<String, Object>) parser.parse(JSON);
		assertTrue("Contains error",map.containsKey("error"));
		assertTrue("Contains moves",map.containsKey("moves"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void jsonValueTest() throws ParseException {
		HashMap<String, Object> map = (HashMap<String, Object>) parser.parse(JSON);
		assertTrue("Error is false & boolean",!(boolean) map.get("error"));
		long[] moves = {1,2,3,4};
		JSONArray array = new JSONArray();
		for(long i : moves){
			array.add(i);
		}
		assertEquals("Move is 4 & long",array,(JSONArray) map.get("moves"));
		JSONArray values = (JSONArray) map.get("moves");
		assertEquals("Move[3] = 4",4,(long) values.get(3));
	}
	
	@Test
	public void WPTest(){
		WebPlayer wb = new WebPlayer();
		GController.initGame(E_GAME_MODE.TESTING, Level.WARN, 4, 4);
		GController.startGame();
		wb.start(E_PLAYER.PLAYER_A);
		wb.preProcess();
		wb.getMove();
		
		try {
			logger.info("Getting into overtime..");
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
