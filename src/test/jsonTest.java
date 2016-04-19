package test;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class jsonTest {
	private static JSONParser parser;
	private static String JSON = "{\"error\":false,\"moves\":[1,2,3,4]}";
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		parser = new JSONParser();
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
		assertEquals("Move[3] = 4",4,(int) values.get(3));
	}

}
