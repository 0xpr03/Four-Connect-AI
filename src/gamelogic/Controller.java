package gamelogic;

import java.lang.reflect.Array;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.newdawn.slick.util.Log;

/**
 * Main controller for the game
 * @author Aron Heinecke
 *
 */
public final class Controller {
	public enum E_GAME_MODE {
		NONE, SINGLE_PLAYER, MULTIPLAYER, KI_INTERNAL, FUZZING, TESTING
	}
	
	public enum E_GAME_STATE {
		NONE, START, PLAYER_A, PLAYER_B, WIN_A, WIN_B, DRAW
	}
	
	public enum E_FIELD_STATE {
		NONE, STONE_A, STONE_B
	}
	
	private static Logger logger = LogManager.getLogger();
	private static E_GAME_MODE GAMEMODE = E_GAME_MODE.NONE;
	private static E_GAME_STATE STATE = E_GAME_STATE.NONE;
	private static final int MAX_ROUNDS = 49;
	private static int MOVES;
	private static WinStore LASTWIN;
	private static E_FIELD_STATE[][] FIELD; // X Y
	private static final int NEEDED_WIN_DIFFERENCE = 2; //declaration: > x = win
	private static final int XY_MAX = 7; //declaration: max + 1
	
	/**
	 * Initialize a new Game
	 * @param gamemode on multiplayer / singleplayer the starting player will be selected randomly
	 * @param loglevel define a loglevel used for this game
	 * everthing else it'll be player a
	 */
	public static void initGame(E_GAME_MODE gamemode, Level loglevel) {
		Configurator.setLevel(logger.getName(), loglevel);
		if (gamemode == E_GAME_MODE.NONE){
			logger.error("Wrong game mode! {}",gamemode);
		}
		STATE = E_GAME_STATE.NONE;
		FIELD = new E_FIELD_STATE[XY_MAX][XY_MAX];
		for(int i = 0; i < XY_MAX; i++){
			for (int j = 0; j < XY_MAX; j++){
				FIELD[i][j] = E_FIELD_STATE.NONE;
			}
		}
		GAMEMODE = gamemode;
		if (GAMEMODE == E_GAME_MODE.TESTING){
			logger.warn("Gamemode set to TESTING. All manipulations are enabled in this mode!");
		}
		MOVES = 0;
	}
	
	/**
	 * Initialize a new game
	 * @param gamemode
	 */
	public static void initGame(E_GAME_MODE gamemode){
		initGame(gamemode, LogManager.getRootLogger().getLevel());
	}
	
	/**
	 * Start a initialized game
	 */
	public static void startGame() {
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
	}
	
	public static boolean getRandomBoolean() {
	    Random random = new Random();
	    return random.nextBoolean();
	}
	
	/**
	 * Returns the current game field state.
	 * @return FIELD_STATE[XY_MAX][XY_MAX]
	 */
	public static E_FIELD_STATE[][] getFieldState(){
		return FIELD;
	}
	
	/**
	 * Returns the current state of the game
	 * @return GAME_STATE
	 */
	public static E_GAME_STATE getGameState() {
		return STATE;
	}
	
	public static int getMoves(){
		return MOVES;
	}
	
	/**
	 * Print the current state of the game field to the console
	 */
	public static String getprintedGameState() {
		StringBuffer sb = new StringBuffer();
		sb.append("Current gamestate:\n");
		for (int i = 6; i >= 0; i--){
			sb.append(i+"\t");
			for (int x = 0; x < XY_MAX; x++){
				sb.append("|"+printConvGamest(FIELD[x][i]));
			}
			sb.append("|\n");
		}
		sb.append("\t ");
		for (int i = 0; i < XY_MAX; i++){
			sb.append(i+" ");
		}
		return sb.toString();
	}
	
	public static void printGameState(){
		logger.debug(getprintedGameState());
	}
	
	private static String printConvGamest(E_FIELD_STATE input){
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
	public synchronized static boolean checkWin(final int posx, final int posy){
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
	private static boolean checkDraw(){
		int y;
		for(int x = 0; x < XY_MAX; x++){
			if(FIELD[x][0] == E_FIELD_STATE.NONE){ // empty column
				return false;
			}
			y = 0;
			while(y < XY_MAX){
				if (FIELD[x][y] == E_FIELD_STATE.NONE){
					break;
				}
				y++;
			}
			logger.debug("XY: {}|{}",x,y);
			// check for edges (1down, 1left/right)
			if(x > 1 && y < XY_MAX){
				if(FIELD[x-1][y] != E_FIELD_STATE.NONE){ // left of space = stone
					if (!checkDrawPos(x-1,y-1, true)){ // check 1left,1down for diag. wins
						return false;
					}
				}
			}
			if(x < (XY_MAX-1)  && y < XY_MAX){
				if(FIELD[x+1][y] != E_FIELD_STATE.NONE){ // right of space = stone
					if (!checkDrawPos(x+1,y-1, true)){ // check 1right,1down for diag. wins
						return false;
					}
				}
			}
			
			// check for topmost stone:
			if (!checkDrawPos(x,y-1,false)){
				return false;
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
	private static boolean checkDrawPos(int x, int y, boolean test_diag_only){
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
			Point a = getXmaxYmax(wstate, x, y,true);
			Point b = getXminYmin(wstate,x,y,true);
			if(a.getX() - b.getX() > NEEDED_WIN_DIFFERENCE){
				return false;
			}
		}
		{
			Point a = getXmaxYmin(wstate, x, y,true);
			Point b = getXminYmax(wstate,x,y,true);
			if(a.getX() - b.getX() > NEEDED_WIN_DIFFERENCE){
				return false;
			}
		}
		return true;
	}
	
	private synchronized static void handleWin(WinStore ws){
		logger.debug("Point A:{}|{} B:{}|{}",ws.getPoint_a().getX(),ws.getPoint_a().getY(),ws.getPoint_b().getX(),ws.getPoint_b().getY());
		logger.debug("State: {}",ws.getState());
		STATE = ws.getState();
		LASTWIN = ws;
		//TODO: run handle code for winner display etc
	}
	
	/**
	 * Check if player wins on y coordinate
	 * @param posx
	 * @param posy
	 * @param wstate
	 * @return
	 */
	private static WinStore checkWin_Y(final int posx,final int posy,final E_FIELD_STATE wstate) {
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
	
	private static boolean matchField(E_FIELD_STATE field,E_FIELD_STATE wstate, boolean ignore_empty){
		if (ignore_empty)
			return field == wstate || field == E_FIELD_STATE.NONE;
		return field == wstate;
	}
	
	/**
	 * Get most y+ point from the starting point
	 * @param wstate
	 * @return
	 */
	private static Point getYmax(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int max_y = posy;
		for(int y = posy; y < XY_MAX; y++){
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
	 */
	private static Point getYmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
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
	 */
	private static WinStore checkWin_X(final int posx,final int posy,final E_FIELD_STATE wstate) {
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
	 */
	private static Point getXmax(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
		int max_x = posx;
		for(int x = posx; x < XY_MAX; x++){
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
	 */
	private static Point getXmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
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
	 */
	private static WinStore checkWin_XYP(final int posx,final int posy,final E_FIELD_STATE wstate) {
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
	 */
	private static Point getXmaxYmax(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int max_x = posx;
		int max_y = posy;
		while (max_x+1 < XY_MAX && max_y+1 < XY_MAX){
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
	 */
	private static Point getXminYmin(final E_FIELD_STATE wstate,final int posx,final int posy, boolean ignore_none){
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
	 */
	private static WinStore checkWin_XYM(final int posx,final int posy,final E_FIELD_STATE wstate) {
		
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
	 */
	private static Point getXmaxYmin(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int max_x = posx;
		int min_y = posy;
		while (max_x+1 < XY_MAX && min_y-1 >= 0){
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
	 */
	private static Point getXminYmax(final E_FIELD_STATE wstate,final int posx,final int posy,boolean ignore_none){
		int min_x = posx;
		int max_y = posy;
		while (max_y+1 < XY_MAX && min_x-1 >= 0){
			if (matchField(FIELD[min_x-1][max_y+1],wstate,ignore_none)){
				min_x--;
				max_y++;
			}else{
				break;
			}
		}
		return new Point(min_x,max_y);
	}
	
	public static boolean D_setField(E_FIELD_STATE[][] field){
		if(GAMEMODE == E_GAME_MODE.TESTING){
			FIELD = field;
			return true;
		}else{
			logger.error("Game field changes are not allowed in this gamemode!");
			return false;
		}
	}
	
	public static boolean D_analyzeField(){
		if(GAMEMODE == E_GAME_MODE.TESTING){
			printGameState();
			return checkDraw();
		}else{
			logger.error("Not allowed in this gamemode!");
			return false;
		}
	}
	
	public static boolean setState(E_GAME_STATE state){
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
	 */
	public static E_FIELD_STATE[][] D_parseField(String input){
		String[] args = input.split("\n");
		logger.debug("Input: {}",args);
		logger.debug("0 column: {}",args[0]);
		E_FIELD_STATE[][] data = new E_FIELD_STATE[XY_MAX][XY_MAX];
		for(int i = 0; i < XY_MAX; i++){
			for (int j = 0; j < XY_MAX; j++){
				FIELD[i][j] = E_FIELD_STATE.NONE;
			}
		}
		
		if(Array.getLength(args) != XY_MAX){
			logger.error("INvalid parse input size! {}",Array.getLength(args));
			return null;
		}
		
		int y_col = (XY_MAX-1);
		for(int y = 0; y < XY_MAX; y++){
			String s = args[y];
			if(s.length() != XY_MAX){
				logger.error("Invalid row input size! {}",s.length());
				return null;
			}
			for(int x = (XY_MAX-1); x >= 0; x--){
				data[x][y_col] = parseChar(s.charAt(x));
			}
			y_col--;
		}
		return data;
	}
	
	private static E_FIELD_STATE parseChar(char s){
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
	
	/**
	 * Insert stone
	 * @param column value between 0-6
	 * @return returns false on failure
	 */
	public synchronized static boolean insertStone(final int column){
		if (STATE != E_GAME_STATE.PLAYER_A && STATE != E_GAME_STATE.PLAYER_B){
			logger.warn("Ignoring stone insert due to state {}!",STATE);
			return false;
		}
		E_FIELD_STATE new_field_state = STATE == E_GAME_STATE.PLAYER_A ? E_FIELD_STATE.STONE_A : E_FIELD_STATE.STONE_B;
		
		int found_place = -1;
		
		for(int i = 0; i < XY_MAX; i++){
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
				}else{
					STATE = STATE == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.PLAYER_B : E_GAME_STATE.PLAYER_A;
				}
			}
		}else{
			logger.info("No more column space to insert any stones!");
		}
		
		return found_place != -1;
	}
}
