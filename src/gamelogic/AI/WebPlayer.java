package gamelogic.AI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private static String URL = "dev.proctet.net/q4proj.php";
	
	private HttpClientBuilder hcbuilder;
	private HttpClient client;
	private Logger logger = LogManager.getLogger();
	private lib lib;
	private HttpClientConnectionManager connManager = new BasicHttpClientConnectionManager();
	private E_PLAYER player;
	private JSONParser parser;
	private int[] prefetched_moves = null;
	private boolean no_move = false;
	private boolean got_answer = false;
	private static Thread t = null;
	
	public WebPlayer(){
		logger.entry();
		hcbuilder = HttpClientBuilder.create();
		hcbuilder.setConnectionManager(connManager);
		hcbuilder.disableAutomaticRetries();
		client = hcbuilder.build();
		lib = new lib();
		parser = new JSONParser();
		logger.exit();
		
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
				if ( !(boolean)map.get("error")){
					JSONArray moves = (JSONArray) map.get("moves");
					prefetched_moves = new int[moves.size()];
					for(int i = 0; i < moves.size(); i++){
						prefetched_moves[i] = (int)((long)moves.get(i));
					}
					no_move = false;
				}else{
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
	
	
	@Override
	public boolean getMove() {
		if(t.isAlive()){
			logger.warn("Prefetch still running!");
			return true;
		}else{
			if(got_answer && !no_move){
				GController.insertStone(prefetched_moves[ThreadLocalRandom.current().nextInt(0, GController.getX_MAX())]);
			}else{
				logger.error("Got move: {}, no move: {}",got_answer, no_move);
			}
		}
		return false;
	}

	@Override
	public void gameEvent(boolean rollback) {
		
	}

	@Override
	public void shutdown() {
		connManager.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		this.player = player;
		this.got_answer = false;
		this.no_move = false;
		this.prefetched_moves = null;
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
