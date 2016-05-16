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
 * This player relies on the call of preProcess before each getMove to prefetch it's next move.
 * 
 * @author Aron Heinecke
 */
public class WebPlayer implements AI {
	private static String UA = "KBS WebPlayer";
	private static String HOST = "prog.db.proctet.net";
	private static String URL = "http://prog.db.proctet.net/ai.php";

	private static int SCHEDULE_TIME = 1000;

	private HttpClientBuilder hcbuilder;
	private HttpClient client;
	private Logger logger = LogManager.getLogger();
	private lib lib;
	private HttpClientConnectionManager connManager;
	private E_PLAYER player;
	private JSONParser parser;
	private Move prefetched_move = null;
	private boolean no_move = false;
	private boolean got_answer = false;
	private static Thread t = null;
	private TimerTask taskDoMove;
	private Timer maintimer;

	public WebPlayer() {
		logger.entry();
		parser = new JSONParser();
		hcbuilder = HttpClientBuilder.create();
		hcbuilder.disableAutomaticRetries();
	}
	
	/**
	 * Select a move and set as prefetched move
	 * 
	 * @param sel
	 */
	private void selectMove(SelectResult sel) {
		logger.entry();
		List<Move> possible_moves = null;
		if (!sel.getWins().isEmpty()) {
			possible_moves = sel.getWins();
		} else if (!sel.getDraws().isEmpty()) {
			possible_moves = sel.getDraws();
		} else {
			if (!sel.getUnused().isEmpty()) {
				possible_moves = sel.getUnused();
				logger.debug("No known sel");
			} else {
				logger.error("Only looses left!");
				possible_moves = sel.getLooses();
			}
		}
		if(possible_moves == null){
			logger.error("No moves!");
			return;
		}
		logger.debug("possible moves: {}", possible_moves.size());
		prefetched_move = possible_moves.get(ThreadLocalRandom.current().nextInt(0, possible_moves.size()));
	}

	/**
	 * Init preprocess thread & re-check timer
	 * 
	 * @author Aron Heinecke
	 */
	private void initThreads() {
		logger.entry();
		taskDoMove = new TimerTask() {
			@Override
			public void run() {
				logger.entry();
				maintimer.cancel();
				getMove();
				logger.exit();
			}
		};

		t = new Thread() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try{
					logger.entry();
					byte[] fieldhash = lib.field2sha(GController.getFieldState());
					List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
					logger.debug("Field: {} player_a: {}",lib.bytesToHex(fieldhash), player);
					urlParameters.add(new BasicNameValuePair("field", lib.bytesToHex(fieldhash)));
					urlParameters.add(new BasicNameValuePair("x", String.valueOf(GController.getX_MAX())));
					urlParameters.add(new BasicNameValuePair("y", String.valueOf(GController.getY_MAX())));
					urlParameters.add(new BasicNameValuePair("player_a", String.valueOf(player == E_PLAYER.PLAYER_A)));
	
					String output = "";
					for(int i = 0; i < 3; i++){
						try {
							output = doPost(URL, HOST, urlParameters);
		
							HashMap<String, Object> map = (HashMap<String, Object>) parser.parse(output);
							if (!((boolean) map.get("error"))) {
								JSONArray moves = (JSONArray) map.get("moves");
								SelectResult sel = new SelectResult();
								for (Object obj : moves) {
									HashMap<String, Object> entry = (HashMap<String, Object>) obj;
									sel.add(getMove(entry));
								}
								selectMove(sel);
								no_move = false;
							} else {
								logger.error("Server response: {}", (String) map.get("error"));
								no_move = true;
							}
							
							got_answer = true;
							return;
						} catch (ParseException e) {
							logger.error("Parse exception: {} on \n{}", e, output);
						} catch (ClassCastException e) {
							logger.error("Invalid input, cast exception: {}", e);
						} catch (IOException e) {
							logger.error("Unable to retrievie KI data: {}", e);
						}
						// wait x ms on error
						try{
							Thread.sleep(50);
						}catch(Exception e){
							logger.error(e);
						}
					}
					no_move = true;
					got_answer = false;
				}catch(Exception e){
					logger.fatal("{}",e);
				}
			}
		};
		t.setName("prefetch");
	}

	/**
	 * Get move object from json map
	 * @param map
	 * @return
	 */
	private Move getMove(HashMap<String, Object> map) {
		boolean loose = ((long) map.get("loose")) != 0;
		boolean draw = ((long) map.get("draw")) != 0;
		boolean win = ((long) map.get("win")) != 0;
		boolean player_a = ((String) map.get("player_a")).equals("true");
		boolean used = ((long) map.get("used")) != 0;
		return new Move(-1, (int) ((long) map.get("move")), used, loose, draw, win, player_a);
	}

	/**
	 * Use prefetched move
	 */
	@Override
	public boolean getMove() {
		logger.entry();
		if (t.isAlive()) {
			logger.warn("Prefetch still running!");
			maintimer.schedule(taskDoMove, SCHEDULE_TIME, SCHEDULE_TIME);
			return true;
		} else {
			if (got_answer && !no_move && prefetched_move != null) {
				logger.debug("Using move: {}",prefetched_move.toString());
				GController.insertStone(prefetched_move.getMove());
				
			} else {
				logger.error("Got move: {}, no move: {}, prefetched_move: {}", got_answer, no_move, prefetched_move);
			}
			no_move = false;
			got_answer = false;
			prefetched_move = null;
		}
		initThreads();
		return false;
	}

	@Override
	public void gameEvent(boolean rollback) {
		logger.entry();
		prefetched_move = null;
		got_answer = false;
		taskDoMove.cancel();
		if(t.isAlive())
			t.interrupt();
		connManager.shutdown();
	}

	@Override
	public void shutdown() {
		logger.entry();
		if(taskDoMove != null)
			taskDoMove.cancel();
		if(t != null)
		if(t.isAlive())
			t.interrupt();
		if(maintimer != null)
			maintimer.cancel();
		if(connManager != null)
			connManager.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry(player);
		connManager = new BasicHttpClientConnectionManager();
		hcbuilder.setConnectionManager(connManager);
		client = hcbuilder.build();
		lib = new lib();
		this.player = player;
		maintimer = new Timer(true);
		this.got_answer = false;
		this.no_move = false;
		this.prefetched_move = null;
		initThreads();
	}

	@Override
	public void preProcess() {
		logger.entry();
		if(t != null){
			if (!t.isAlive()) {
				t.start();
			} else {
				logger.error("Prefetch thread still running!");
			}
		}else{
			logger.fatal("No thread initiated !");
		}
	}

	@Override
	public void goBackHistory(boolean allowEmpty) {
	}

	@Override
	public boolean hasMoreMoves() {
		return false;
	}

	@Override
	public void getOutcome() {
	}

	/**
	 * Http1/2.0 post
	 * @param url post url
	 * @param host header specified host
	 * @param urlParameters params to post
	 */
	private String doPost(String url, String host, List<NameValuePair> urlParameters) throws IOException {
		// create client & post
		HttpPost post = new HttpPost(url);

		// add header
		post.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		post.setHeader("Accept-Encoding", "gzip, deflate");
		post.setHeader("Accept-Language", "en-us;q=0.5,en;q=0.3");
		post.setHeader("Connection", "keep-alive");
		post.setHeader("Host", host);
		post.setHeader("User-Agent", UA);

		// create gzip encoder
		UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(urlParameters);
		urlEncodedFormEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_ENCODING, "UTF_8"));
		post.setEntity(urlEncodedFormEntity);

		// Create own context which stores the cookies
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

		if (response.getStatusLine().getStatusCode() != 200) {
			logger.error("Server returned code {}", response.getStatusLine().getStatusCode());
		}

		return result.toString();
	}
}
