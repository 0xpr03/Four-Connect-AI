package gamelogic;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.AI.AI;
import gamelogic.Controller.E_FIELD_STATE;

/**
 * Main controller for the game four connect
 * This class is NOT supposed to be created and used object.
 * Use GController as main instance!!
 * @author Aron Heinecke
 * @param <E>
 *
 */
public final class Controller<E extends AI> {
	public enum E_GAME_MODE {
		NONE, SINGLE_PLAYER, MULTIPLAYER, KI_INTERNAL, FUZZING, TESTING
	}
	
	public enum E_PLAYER {
		PLAYER_A, PLAYER_B
	}
	
	public enum E_GAME_STATE {
		NONE, START, PLAYER_A, PLAYER_B, WIN_A, WIN_B, DRAW, RESTART
	}
	
	public enum E_FIELD_STATE {
		NONE, STONE_A, STONE_B
	}
	
	private Logger logger = LogManager.getLogger();
	private E_GAME_MODE GAMEMODE = E_GAME_MODE.NONE;
	private E_GAME_STATE STATE = E_GAME_STATE.NONE;
	private int MOVES;
	private WinStore LASTWIN;
	private E_FIELD_STATE[][] FIELD; // X Y
	private final int NEEDED_WIN_DIFFERENCE = 2; //declaration: > x = win
	private final Object lock = new Object();
	private E AI_a = null; // primary AI
	private E AI_b = null; // secondary for KI internal
	private int X_MAX = 4;
	private int Y_MAX = 4;
	
	public Controller(E ai_a, E ai_b){
		logger.entry();
		if(AI_a == null)
			AI_a = ai_a;
		if(AI_b == null)
			AI_b = ai_b;
		logger.debug("Init AI's: A:{} B:{}",AI_a != null, AI_b != null);
		
		if(X_MAX != 7)
			logger.warn("X_MAX = {}",X_MAX);
		if(Y_MAX != 6)
			logger.warn("Y_MAX = {}",Y_MAX);
		
		if(E_FIELD_STATE.NONE.ordinal() != 0 || E_FIELD_STATE.STONE_A.ordinal() != 1 || E_FIELD_STATE.STONE_B.ordinal() != 2){
			logger.fatal("Invalid field state ordinals! FIELD{NONE:{},STONE_A:{},STONE_B:{}}",E_FIELD_STATE.NONE.ordinal(),E_FIELD_STATE.STONE_A.ordinal(),E_FIELD_STATE.STONE_B.ordinal());
			shutdown();
		}
		
	}
	public Controller(){}

	/**
	 * Initialize a new Game
	 * @param gamemode on multiplayer / singleplayer the starting player will be selected randomly
	 * @param loglevel define a loglevel used for this game
	 * everthing else it'll be player a
	 */
	public synchronized void initGame(E_GAME_MODE gamemode, Level loglevel) {
		synchronized(lock){
			Configurator.setLevel(logger.getName(), loglevel);
			if (gamemode == E_GAME_MODE.NONE){
				logger.error("Wrong game mode! {}",gamemode);
			}
			LASTWIN = null;
			STATE = E_GAME_STATE.NONE;
			FIELD = new E_FIELD_STATE[X_MAX][Y_MAX];
			for(int i = 0; i < X_MAX; i++){
				for (int j = 0; j < Y_MAX; j++){
					FIELD[i][j] = E_FIELD_STATE.NONE;
				}
			}
			GAMEMODE = gamemode;
			if (GAMEMODE == E_GAME_MODE.TESTING){
				logger.warn("Gamemode set to TESTING. All manipulations are enabled in this mode!");
			}
			MOVES = 0;
		}
	}
	
	public void moveAI_A(){
		AI_a.getMove();
	}
	
	public void moveAI_B(){
		AI_b.getMove();
	}
	
	/**
	 * Sets the current game to restart mode
	 * This is used only for KI internals where dataraces are comming
	 */
	public void restart(){
		STATE = E_GAME_STATE.RESTART;
	}
	
	/**
	 * Initialize a new game
	 * @param gamemode
	 */
	public void initGame(E_GAME_MODE gamemode){
		initGame(gamemode, LogManager.getRootLogger().getLevel());
	}
	
	/**
	 * Start a initialized game
	 */
	public synchronized void startGame() {
		if(GAMEMODE == E_GAME_MODE.NONE){
			logger.error("Uninitialized game start!");
			return;
		}
		STATE = E_GAME_STATE.START;
		if (GAMEMODE == E_GAME_MODE.MULTIPLAYER || GAMEMODE == E_GAME_MODE.SINGLE_PLAYER || GAMEMODE == E_GAME_MODE.FUZZING) {
			STATE = getRandomBoolean() ? E_GAME_STATE.PLAYER_A : E_GAME_STATE.PLAYER_B;
		}else{
			STATE = E_GAME_STATE.PLAYER_A;
		}
		start_AI();
		new Thread() {
		    public void run() {
		        try {
		            run_AI();
		        } catch(Error e) {
		            logger.error(e);
		        }
		    }  
		}.start();
	}
	
	/**
	 * Let KI start a move, if gamemode & current player matches
	 */
	private void run_AI(){
		if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
			if(STATE == E_GAME_STATE.PLAYER_B){
//				AI_a.getMove();
			}
		}
	}
	
	/**
	 * Called on game start with AIs
	 * AI_a is Player B on singleplayer
	 * AI_a is player A on KI internal
	 */
	private void start_AI(){
		if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
			AI_a.start(E_PLAYER.PLAYER_B);
		}else if(GAMEMODE == E_GAME_MODE.KI_INTERNAL){
			AI_a.start(E_PLAYER.PLAYER_A);
			AI_b.start(E_PLAYER.PLAYER_B);
		}
	}
	
	/**
	 * Get random true/false
	 * @return
	 */
	public boolean getRandomBoolean() {
	    Random random = new Random();
	    return random.nextBoolean();
	}
	
	/**
	 * Returns the current game field state.
	 * @return FIELD_STATE[XY_MAX][XY_MAX]
	 */
	public synchronized E_FIELD_STATE[][] getFieldState(){
		return FIELD;
	}
	
	/**
	 * Get current gamemode
	 * @return
	 */
	public synchronized E_GAME_MODE getGamemode(){
		return GAMEMODE;
	}
	
	/**
	 * Returns the current state of the game
	 * @return GAME_STATE
	 */
	public E_GAME_STATE getGameState() {
		return STATE;
	}
	
	/**
	 * Return the amount of done moves for this game
	 * @return
	 */
	public int getMoves(){
		return MOVES;
	}
	
	/**
	 * Print the current state of the game field to the console
	 */
	public String getprintedGameState() {
		StringBuffer sb = new StringBuffer();
		sb.append("Current gamestate: ");
		sb.append(STATE);
		sb.append("\n");
		for (int Y = (Y_MAX-1); Y >= 0; Y--){
			sb.append(Y+"\t");
			for (int x = 0; x < X_MAX; x++){
				sb.append("|"+printConvGamest(FIELD[x][Y]));
			}
			sb.append("|\n");
		}
		sb.append("\t ");
		for (int X = 0; X < X_MAX; X++){
			sb.append(X+" ");
		}
		
		if(LASTWIN != null){
			sb.append("\nLast win from ");
			sb.append(LASTWIN.getState());
			if(LASTWIN.isCapitulation()){
				sb.append(" By capitulation");
			}else{
				Point a = LASTWIN.getPoint_a();
				Point b = LASTWIN.getPoint_b();
				sb.append(" at ");
				sb.append(a.getX());
				sb.append("|");
				sb.append(a.getY());
				sb.append(" ");
				sb.append(b.getX());
				sb.append("|");
				sb.append(b.getY());
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Print the current game field and state to the console
	 */
	public void printGameState(){
		logger.info(()->getprintedGameState());
	}
	
	private String printConvGamest(E_FIELD_STATE input){
		switch(input){
		case NONE:
			return "-";
		case STONE_A:
			return "X";
		case STONE_B:
			return "O";
		default:
			return "ERR";
		}
	}
	
	/**
	 * Check for win based on the current stone
	 * @return
	 */
	private synchronized boolean checkWin(final int posx, final int posy){
		logger.debug("Player: {}",STATE);
		E_FIELD_STATE wstate = STATE == E_GAME_STATE.PLAYER_A ?  E_FIELD_STATE.STONE_A : E_FIELD_STATE.STONE_B;
		logger.debug("wished state: {}",wstate);
		WinStore wst = checkWin_Y(posx, posy, wstate);
		if ( wst != null ){
			handleWin(wst);
			return true;
		}
		wst = checkWin_X(posx, posy, wstate);
		if ( wst != null ){
			handleWin(wst);
			return true;
		}
		wst = checkWin_XYP(posx, posy, wstate);
		if ( wst != null ){
			handleWin(wst);
			return true;
		}
		wst = checkWin_XYM(posx, posy, wstate);
		if ( wst != null ){
			handleWin(wst);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks for a draw
	 * @param wstate
	 * @return true if it's a draw
	 * @author Aron Heinecke
	 */
	private boolean checkDraw(){
		int y;
		for(int x = 0; x < X_MAX; x++){
			if(FIELD[x][0] == E_FIELD_STATE.NONE){ // empty column
				return false;
			}
			y = 0;
			while(y < Y_MAX){
				if (FIELD[x][y] == E_FIELD_STATE.NONE){
					break;
				}
				y++;
			}
			logger.debug("XY: {}|{}",x,y);
			// check for edges (1down, 1left/right)
			if(x > 1 && y < Y_MAX){
				if(FIELD[x-1][y] != E_FIELD_STATE.NONE){ // left of space = stone
					if (!checkDrawPos(x-1,y-1, true,false)){ // check 1left,1down for diag. wins
						return false;
					}
					if (!checkDrawPos(x-1,y, false,true)){ // check 1left for horiz/vert wins
						return false;
					}
				}
			}
			if(x < (X_MAX-1)  && y < Y_MAX){
				if(FIELD[x+1][y] != E_FIELD_STATE.NONE){ // right of space = stone
					if (!checkDrawPos(x+1,y-1, true,false)){ // check 1right,1down for diag. wins
						return false;
					}
					if (!checkDrawPos(x+1,y, false,true)){ // check 1right for horiz/vert wins
						return false;
					}
				}
			}
			
			// check for topmost stone:
			if (!checkDrawPos(x,y-1,false,false)){
				return false;
			}
			
			for(int y2 = y; y2 < Y_MAX; y2++){
			// check for 4-space free
				if(!checkDrawPos(x,y2,false,true)){
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Returns false if one possible win for posxy is found.
	 * @param x
	 * @param y
	 * @param test_diag_only if set to true, x/y coord. wins won't be checked
	 * @return true if no possible win for pos is found
	 * @author Aron Heinecke
	 */
	private boolean checkDrawPos(int x, int y, boolean test_diag_only, boolean test_horiz_vert_only){
		E_FIELD_STATE wstate = FIELD[x][y];
		{
			if(!test_diag_only){
				Point a = getXmax(wstate, x, y,true);
				Point b = getXmin(wstate,x,y,true);
				if(a.getX() - b.getX() > NEEDED_WIN_DIFFERENCE){
					return false;
				}
			}
		}
		{
			if(!test_diag_only){
				Point a = getYmax(wstate, x, y,true);
				Point b = getYmin(wstate,x,y,true);
				if(a.getY() - b.getY() > NEEDED_WIN_DIFFERENCE){
					return false;
				}
			}
		}
		{
			if(!test_horiz_vert_only){
				Point a = getXmaxYmax(wstate, x, y,true);
				Point b = getXminYmin(wstate,x,y,true);
				if(a.getX() - b.getX() > NEEDED_WIN_DIFFERENCE){
					return false;
				}
			}
		}
		{
			if(!test_horiz_vert_only){
				Point a = getXmaxYmin(wstate, x, y,true);
				Point b = getXminYmax(wstate,x,y,true);
				if(a.getX() - b.getX() > NEEDED_WIN_DIFFERENCE){
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * Handle wins, calling animation functions etc
	 * @param ws
	 * @author Aron Heinecke
	 */
	private synchronized void handleWin(WinStore ws){
		if(ws.isCapitulation()){
			logger.debug("Capitulation");
		}else{
			logger.debug("Point A:{}|{} B:{}|{}",ws.getPoint_a().getX(),ws.getPoint_a().getY(),ws.getPoint_b().getX(),ws.getPoint_b().getY());
		}
		logger.debug("State: {}",ws.getState());
		STATE = ws.getState();
		LASTWIN = ws;
		informAIs();
		//TODO: run handle code for winner display etc
	}
	
	private synchronized void informAIs(){
		if(GAMEMODE == E_GAME_MODE.KI_INTERNAL){
			AI_a.gameEvent();
			AI_b.gameEvent();
		}else if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
			AI_a.gameEvent();
		}
	}
	
	/**
	 * Check if player wins on y coordinate
	 * @param posx
	 * @param posy
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private WinStore checkWin_Y(final int posx,final int posy,final E_FIELD_STATE wstate) {
		logger.debug("posx:{} posy:{} wstate:{}",posx,posy,wstate);
		Point a = getYmax(wstate,posx,posy,false);
		Point b = getYmin(wstate,posx,posy,false);
		
		logger.debug("max_y:{} min_y:{}",a.getY(),b.getY());
		if ( ( a.getY() - b.getY() ) > NEEDED_WIN_DIFFERENCE ) {
			logger.debug("Winn Y");
			return new WinStore(a,b, STATE);
		}else{
			return null;
		}
	}
	
	private boolean matchField(E_FIELD_STATE field,E_FIELD_STATE wstate, boolean ignore_empty){
		if (ignore_empty)
			return field == wstate || field == E_FIELD_STATE.NONE;
		return field == wstate;
	}
	
	/**
	 * Get most y+ point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getYmax(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int max_y = posy;
		for(int y = posy; y < Y_MAX; y++){
			if ( matchField(FIELD[posx][y],wstate,ignore_none) ) {
				max_y = y;
			}else {
				break;
			}
		}
		return new Point(posx,max_y);
	}
	
	/**
	 * Get most y- point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getYmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int min_y = posy;
		for(int y = posy; y >= 0; y--){
			if ( matchField(FIELD[posx][y],wstate,ignore_none) ) {
				min_y = y;
			} else {
				break;
			}
		}
		return new Point(posx,min_y);
	}
	
	/**
	 * Check if player wins on x coordinate
	 * @param posx
	 * @param posy
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private WinStore checkWin_X(final int posx,final int posy,final E_FIELD_STATE wstate) {
		Point a = getXmax(wstate,posx, posy,false);
		Point b = getXmin(wstate,posx,posy,false);
		
		logger.debug("max_x:{} min_x:{}",a.getX(),b.getX());
		if ( ( a.getX() - b.getX())  > NEEDED_WIN_DIFFERENCE ) {
			logger.debug("Winn X");
			return new WinStore(a,b, STATE);
		}else{
			return null;
		}
	}
	
	/**
	 * Get most x+ point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXmax(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int max_x = posx;
		for(int x = posx; x < X_MAX; x++){
			if ( matchField(FIELD[x][posy],wstate,ignore_none) ) {
				max_x = x;
			}else {
				break;
			}
		}
		return new Point(max_x,posy);
	}
	
	/**
	 * Get most x- point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int min_x = posx;
		for(int x = posx; x >= 0; x--){
			if ( matchField(FIELD[x][posy],wstate,ignore_none) ) {
				min_x = x;
			} else {
				break;
			}
		}
		return new Point(min_x,posy);
	}
	
	/**
	 * Check if player wins across xy coordinate
	 * Going x- y- to x+ y+ from the current pos
	 * @param posx
	 * @param posy
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private WinStore checkWin_XYP(final int posx,final int posy,final E_FIELD_STATE wstate) {
		Point a = getXmaxYmax(wstate,posx,posy,false);
		Point b = getXminYmin(wstate,posx,posy,false);
		
		logger.debug("max_x:{} min_x:{}",a.getX(),b.getX());
		if ( ( a.getX() - b.getX())  > NEEDED_WIN_DIFFERENCE ) {
			logger.debug("Winn XYP");
			return new WinStore(a,b, STATE);
		}else{
			return null;
		}
	}
	
	/**
	 * Get most x+ y+ point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXmaxYmax(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int max_x = posx;
		int max_y = posy;
		while (max_x+1 < X_MAX && max_y+1 < Y_MAX){
			if (matchField(FIELD[max_x+1][max_y+1],wstate,ignore_none)){
				max_x++;
				max_y++;
			}else{
				break;
			}
		}
		return new Point(max_x,max_y);
	}
	
	/**
	 * Get most x- y- point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXminYmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int min_x = posx;
		int min_y = posy;
		while (min_x-1 >= 0 && min_y-1 >= 0){
			if (matchField(FIELD[min_x-1][min_y-1],wstate,ignore_none)){
				min_x--;
				min_y--;
			}else{
				break;
			}
		}
		return new Point(min_x,min_y);
	}
	
	
	
	/**
	 * Check if player wins across xy coordinate
	 * Going x- y+ to x+ y- from the current pos
	 * @param posx
	 * @param posy
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private WinStore checkWin_XYM(final int posx,final int posy,final E_FIELD_STATE wstate) {
		
		Point a = getXmaxYmin(wstate,posx,posy,false);
		logger.debug("P1: {}|{}",a.getX(),a.getY());
		
		Point b = getXminYmax(wstate,posx,posy,false);
		logger.debug("PNEEDED_WIN_DIFFERENCE: {}|{}",b.getX(),b.getY());
		
		//remove possible 0
		logger.debug("max_x:{} min_x:{}",a.getX(),b.getX());
		if ( ( a.getX() - b.getX())  > NEEDED_WIN_DIFFERENCE ) {
			logger.debug("Winn XYM");
			return new WinStore(a,b, STATE);
		}else{
			return null;
		}
	}
	
	/**
	 * Get most x+ y- point from the starting point
	 * @param wstate
	 * @param posx
	 * @param posy
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXmaxYmin(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int max_x = posx;
		int min_y = posy;
		while (max_x+1 < X_MAX && min_y-1 >= 0){
			if (matchField(FIELD[max_x+1][min_y-1],wstate,ignore_none)){
				max_x++;
				min_y--;
			}else{
				break;
			}
		}
		return new Point(max_x,min_y);
	}
	
	/**
	 * Get most x- y+ point from the starting point
	 * @param wstate
	 * @return
	 * @author Aron Heinecke
	 */
	private Point getXminYmax(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int min_x = posx;
		int max_y = posy;
		while (max_y+1 < Y_MAX && min_x-1 >= 0){
			if (matchField(FIELD[min_x-1][max_y+1],wstate,ignore_none)){
				min_x--;
				max_y++;
			}else{
				break;
			}
		}
		return new Point(min_x,max_y);
	}
	
	/**
	 * Set game field
	 * Only for debugging
	 * @param field
	 * @return true on success
	 * @author Aron Heinecke
	 */
	public boolean D_setField(E_FIELD_STATE[][] field){
		if(GAMEMODE == E_GAME_MODE.TESTING){
			FIELD = field;
			return true;
		}else{
			logger.error("Game field changes are not allowed in this gamemode!");
			return false;
		}
	}
	
	/**
	 * Analyze game
	 * Only for debugging
	 * @return true on draw
	 * @author Aron Heinecke
	 */
	public boolean D_analyzeField(){
		if(GAMEMODE == E_GAME_MODE.TESTING){
			printGameState();
			for(int x = 0; x < X_MAX; x++){
				for(int y = 0; y < Y_MAX; y++){
					if(checkWin(x, y)){
						logger.info("Found win at {}|{}",y,x);
						break;
					}
				}
			}
			return checkDraw();
		}else{
			logger.error("Not allowed in this gamemode!");
			return false;
		}
	}
	
	/**
	 * Set state of game
	 * @param state
	 * @return true on success
	 */
	public boolean setState(E_GAME_STATE state){
		if(GAMEMODE == E_GAME_MODE.TESTING){
			STATE = state;
			return true;
		}else{
			logger.error("Not allowed in this gamemode!");
			return false;
		}
	}
	
	/**
	 * Parse an string of AB-BA... (2d) into a FIELD
	 * @param input
	 * @return null on invalid input
	 * @author Aron Heinecke
	 */
	public E_FIELD_STATE[][] D_parseField(String input){
		String[] args = input.split("\n");
		logger.debug("Input: {}",args);
		logger.debug("0 column: {}",args[0]);
		E_FIELD_STATE[][] data = new E_FIELD_STATE[X_MAX][Y_MAX];
		for(int i = 0; i < X_MAX; i++){
			for (int j = 0; j < Y_MAX; j++){
				FIELD[i][j] = E_FIELD_STATE.NONE;
			}
		}
		
		if(Array.getLength(args) != Y_MAX){
			logger.error("Invalid parse input size! {}",Array.getLength(args));
			return null;
		}
		
		int y_col = (Y_MAX-1);
		for(int y = 0; y < Y_MAX; y++){
			String s = args[y];
			if(s.length() != X_MAX){
				logger.error("Invalid row input size! {}",s.length());
				return null;
			}
			for(int x = (X_MAX-1); x >= 0; x--){
				data[x][y_col] = parseChar(s.charAt(x));
			}
			y_col--;
		}
		return data;
	}
	
	private E_FIELD_STATE parseChar(char s){
		E_FIELD_STATE state;
		switch(s){
		case 'A':
		case 'a':
		case 'X':
			state = E_FIELD_STATE.STONE_A;
			break;
		case 'B':
		case 'b':
		case 'O':
			state = E_FIELD_STATE.STONE_B;
			break;
		case '-':
		case ' ':
		default:
			state = E_FIELD_STATE.NONE;
		}
		return state;
	}
	
	public synchronized void capitulate(E_PLAYER player){
		WinStore ws = new WinStore(STATE);
		handleWin(ws);
	}
	
	/**
	 * Insert stone
	 * @param column value between 0-6
	 * @return returns false on failure
	 * @author Aron Heinecke
	 */
	public synchronized boolean insertStone(final int column){
		logger.entry();
		int found_place = -1;
		//synchronized(lock){
			if (STATE != E_GAME_STATE.PLAYER_A && STATE != E_GAME_STATE.PLAYER_B){
				logger.warn("Ignoring stone insert due to state {}!",STATE);
				return false;
			}
			E_FIELD_STATE new_field_state = STATE == E_GAME_STATE.PLAYER_A ? E_FIELD_STATE.STONE_A : E_FIELD_STATE.STONE_B;
			
			for(int i = 0; i < Y_MAX; i++){
				if ( FIELD[column][i] == E_FIELD_STATE.NONE ) {
					FIELD[column][i] = new_field_state;
					found_place = i;
					break;
				}
			}
			
			if(found_place != -1) {
				MOVES++;
				if ( !checkWin(column,found_place) ) {
					if (checkDraw()){
						STATE = E_GAME_STATE.DRAW;
						informAIs();
					}else{
						STATE = STATE == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.PLAYER_B : E_GAME_STATE.PLAYER_A;
//						if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
//							if(STATE == E_GAME_STATE.PLAYER_B){
//								new Thread() {
//								    public void run() {
//								        try {
//								            run_AI();
//								        } catch(Error e) {
//								            logger.error(e);
//								        }
//								    }  
//								}.start();
//							}
//						}
					}
				}
			}else{
				logger.info("No more column space to insert any stones!");
			}
			
			//TODO: call graphics && let it callback the next run
		//}
		return found_place != -1;
	}
	
	/**
	 * Returns list of possible moves
	 * @return
	 */
	public List<Integer> getPossibilities(){
		logger.entry();
		List<Integer> possibilities = new ArrayList<Integer>();
		int ymax = GController.getY_MAX()-1;
		synchronized (lock){
			for(int x = 0; x < GController.getX_MAX(); x++){
				logger.debug("State for {} {}",x,FIELD[x][ymax]);
				if(FIELD[x][ymax] == E_FIELD_STATE.NONE){
					possibilities.add(x);
				}
			}
		}
		return possibilities;
	}
	
	public void shutdown(){
		synchronized (lock){
			STATE = E_GAME_STATE.NONE;
			GAMEMODE = E_GAME_MODE.NONE;
			if(AI_a != null)
				AI_a.shutdown();
			if(AI_b != null)
				AI_b.shutdown();
		}
	}
	
	/**
	 * @return the x_MAX
	 */
	public int getX_MAX() {
		return X_MAX;
	}

	/**
	 * @return the y_MAX
	 */
	public int getY_MAX() {
		return Y_MAX;
	}
}
