package gamelogic.AI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import gamelogic.ControllerBase.E_PLAYER;
import gamelogic.GController;

/**
 * KI player retrieving it's data over the web<br>
 * This player relies on the call of preProcess before each getMove
 * @author Aron Heinecke
 */
public class WebPlayer implements AI {
	private static String UA = "KBS WebPlayer";
	private static String HOST = "proctet.net";
	private static String URL = "http://localhost/q4_ai/ai.php";
	
	private static int SCHEDULE_TIME = 1000;
	
	private HttpClientBuilder hcbuilder;
	private HttpClient client;
	private Logger logger = LogManager.getLogger();
	private lib lib;
	private HttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
	private E_PLAYER player;
	private JSONParser parser;
	private Move prefetched_move = null;
	private boolean no_move = false;
	private boolean got_answer = false;
	private static Thread t = null;
	private TimerTask taskDoMove;
	private Timer maintimer;

	
	public WebPlayer(){
		logger.entry();
		hcbuilder = HttpClientBuilder.create();
		hcbuilder.setConnectionManager(connManager);
		hcbuilder.disableAutomaticRetries();
		client = hcbuilder.build();
		lib = new lib();
		maintimer = new Timer(true);
		parser = new JSONParser();
		initThreads();
	}
	
	/**
	 * Select a move and set as prefetched move
	 * @param sel
	 */
	private void selectMove(SelectResult sel){
		List<Move> possible_moves = new ArrayList<Move>(GController.getX_MAX());
		if(!sel.getWins().isEmpty()){
			List<Move> possibilities = new ArrayList<Move>();
			for(Move moveelem : sel.getWins()){
				if(!moveelem.isDraw()){
					possibilities.add(moveelem);
				}
			}
			if(!possibilities.isEmpty()){
				possible_moves = possibilities;
			}else{
				possible_moves = sel.getWins();
			}
		}else if(!sel.getDraws().isEmpty()) {
			List<Move> possibilities = new ArrayList<Move>();
			for(Move moveelem : sel.getDraws()){
				if(!moveelem.isLoose()){
					possibilities.add(moveelem);
				}
			}
			if(!possibilities.isEmpty()){
				possible_moves = possibilities;
			}else{
				possible_moves = sel.getDraws();
			}
		}else{
			if(sel.getUnused().isEmpty()){
				possible_moves = sel.getUnused();
				logger.debug("No known sel");
			}else{
				logger.error("Only looses left!");
				possible_moves = sel.getLooses();
			}
		}
		
		prefetched_move = possible_moves.get(ThreadLocalRandom.current().nextInt(0, possible_moves.size()));
	}
	
	/**
	 * Init preprocess thread & re-check timer
	 * @author Aron Heinecke
	 */
	private void initThreads(){
		taskDoMove = new TimerTask() {
			@Override
			public void run() {
				logger.entry();
				maintimer.cancel();
				getMove();
				logger.exit();
			}
		};
		
		t = new Thread(){
			@SuppressWarnings("unchecked")
			@Override
			public void run(){
			byte[] fieldhash = lib.field2sha(GController.getFieldState());
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("field", lib.bytesToHex(fieldhash)));
			urlParameters.add(new BasicNameValuePair("x", String.valueOf(GController.getX_MAX())));
			urlParameters.add(new BasicNameValuePair("y", String.valueOf(GController.getY_MAX())));
			urlParameters.add(new BasicNameValuePair("player_a", String.valueOf(player == E_PLAYER.PLAYER_A)));
			
			String output = "";
			try{
				output = doPost(URL, HOST, urlParameters);
				
				HashMap<String, Object> map = (HashMap<String, Object>) parser.parse(output);
				if ( ((String)map.get("error")).equals("false")){
					JSONArray moves = (JSONArray) map.get("moves");
					SelectResult sel = new SelectResult();
					for(Object obj :moves){
						HashMap<String, Object> entry = (HashMap<String, Object>) obj;
						Move m = new Move(-1, (int)((long)entry.get("move")),(boolean) entry.get("used"),
								(boolean)entry.get("loose"),(boolean) entry.get("draw"), (boolean)entry.get("win"),(boolean) entry.get("player_a"));
						sel.add(m);
					}
					selectMove(sel);
					no_move = false;
				}else{
					logger.error("Server response: {}",(String)map.get("error"));
					no_move = true;
				}
				
				got_answer = true;
				return;
			}catch (ParseException e){
				logger.error("Parse exception: {} on \n{}",e,output);
			}catch (ClassCastException  e){
				logger.error("Invalid input, cast exception: {}",e);
			} catch (IOException e) {
				logger.error("Unable to retrievie KI data: {}",e);
			}
			no_move = true;
			got_answer = false;
			}
		};
	}
	
	/**
	 * Use prefetched move
	 */
	@Override
	public boolean getMove() {
		logger.entry();
		if(t.isAlive()){
			logger.warn("Prefetch still running!");
			maintimer.schedule(taskDoMove, SCHEDULE_TIME,SCHEDULE_TIME); // run it later on
			return true;
		}else{
			if(got_answer && !no_move && prefetched_move != null){
				GController.insertStone(prefetched_move.getMove());
			}else{
				logger.error("Got move: {}, no move: {}",got_answer, no_move);
			}
			no_move = false;
			got_answer = false;
			prefetched_move = null;
		}
		return false;
	}

	@Override
	public void gameEvent(boolean rollback) {
		prefetched_move = null;
		got_answer = false;
	}

	@Override
	public void shutdown() {
		taskDoMove.cancel();
		maintimer.cancel();
		connManager.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		this.player = player;
		this.got_answer = false;
		this.no_move = false;
		this.prefetched_move = null;
	}

	@Override
	public void preProcess() {
		if(!t.isAlive()){
			t.start();
		}else{
			logger.error("Prefetch thread still running!");
		}
	}

	@Override
	public void goBackHistory(boolean allowEmpty) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasMoreMoves() {
		return false;
	}

	@Override
	public void getOutcome() {
		// TODO Auto-generated method stub
		
	}
	
	
	private String doPost(String url, String host, List<NameValuePair> urlParameters) throws IOException{
		//create client & post
		HttpPost post = new HttpPost(url);

		// add header
		post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		post.setHeader("Accept-Encoding", "gzip, deflate");
		post.setHeader("Accept-Language", "en-us;q=0.5,en;q=0.3");
		post.setHeader("Connection", "keep-alive");
		post.setHeader("Host", host);
		post.setHeader("User-Agent", UA);
		
		//create gzip encoder
		UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(urlParameters);
		urlEncodedFormEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_ENCODING, "UTF_8"));
		post.setEntity(urlEncodedFormEntity);

		//Create own context which stores the cookies
		HttpClientContext context = HttpClientContext.create();
		
		HttpResponse response = client.execute(post, context);
		
		// input-stream with gzip-accept
		InputStream input = response.getEntity().getContent();
		InputStreamReader isr = new InputStreamReader(input);
		BufferedReader rd = new BufferedReader(isr);
		
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		input.close();
		isr.close();
		rd.close();
		
		if(response.getStatusLine().getStatusCode() != 200){
			logger.error("Server returned code {}",response.getStatusLine().getStatusCode());
		}

		return result.toString();
	}
}
