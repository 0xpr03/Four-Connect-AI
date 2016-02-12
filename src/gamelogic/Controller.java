package gamelogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.AI.AI;

/**
 * Controller for the four connect game
 * This class is NOT supposed to be created and used object.
 * Use GController as main instance!!
 * @author Aron Heinecke
 * @param <E>
 */
public final class Controller<E extends AI> extends ControllerBase<E> {
	
	private Logger logger = LogManager.getLogger();
	
	public Controller(E kbs_trainer, E kbs_trainer2) {
		super(kbs_trainer, kbs_trainer2);
	}
	
	public Controller() {
		super();
	}

	// for ki training
	private List<E_FIELD_STATE[][]> matchHistory;
	private E_GAME_STATE LAST_PLAYER;
	
	/**
	 * Initialize a new Game
	 * @param gamemode on multiplayer / singleplayer the starting player will be selected randomly
	 * @param loglevel define a loglevel used for this game
	 * everthing else it'll be player a
	 */
	public synchronized void initGame(E_GAME_MODE gamemode, Level loglevel) {
		synchronized(lock){
			Configurator.setLevel(LogManager.getLogger(ControllerBase.class).getName(), loglevel);
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
			
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
				matchHistory = new ArrayList<E_FIELD_STATE[][]>((X_MAX * Y_MAX) - 5);
			}
			
			if (GAMEMODE == E_GAME_MODE.TESTING){
				logger.warn("Gamemode set to TESTING. All manipulations are enabled in this mode!");
			}
			MOVES = 0;
		}
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
		synchronized (lock) {
			if(GAMEMODE == E_GAME_MODE.NONE){
				logger.error("Uninitialized game start!");
				return;
			}
			STATE = E_GAME_STATE.START;
			switch(GAMEMODE){
			case KI_TRAINING:
				addHistory(); // add empty field
			case MULTIPLAYER:
			case SINGLE_PLAYER:
			case KI_INTERNAL:
			case FUZZING:
				STATE = getRandomBoolean() ? E_GAME_STATE.PLAYER_A : E_GAME_STATE.PLAYER_B;
				break;
			default:
				STATE = E_GAME_STATE.PLAYER_A;
				break;
			}
			start_AI();
		}
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
		synchronized (lock) {
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
		}
			if(found_place != -1) {
				MOVES++;
				WinStore ws = checkWin(column,found_place);
				if(ws == null){ // no win
					if(checkDraw()){ // is draw
						handelDraw();
					}else{ // no draw, no win, next move
						if(matchHistory != null){
							addHistory();
						}
						STATE = STATE == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.PLAYER_B : E_GAME_STATE.PLAYER_A;
					}
				}else{
					handleWin(ws);
				}
			}else{
				logger.info("No more column space to insert any stones!");
			}
			
			//TODO: call graphics && let it callback the next run
		return found_place != -1;
	}
	
	private E_FIELD_STATE[][] copyField(E_FIELD_STATE[][] origin){
		E_FIELD_STATE[][] copy = new E_FIELD_STATE[X_MAX][Y_MAX];
		for(int x = 0; x < X_MAX; x++){
			copy[x] = Arrays.copyOf(origin[x],Y_MAX);
		}
		return copy;
	}
	
	protected synchronized void checkHistory(){
		logger.entry();
		logger.debug(()->super.getprintedGameState());
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING)
			LAST_PLAYER = STATE;
	}
	
	protected synchronized void handelDraw(){
		checkHistory();
		STATE = E_GAME_STATE.DRAW;
		informAIs();
	}
	
	/**
	 * Curent KI is finished with a all moves
	 * Go back & lat KI before move
	 */
	private void handle_KI_moveless(){
		logger.entry();
		logger.info(()->super.getprintedGameState());
		go_back_history(true);
		MOVES -= 2;
		
		E_GAME_STATE state_cache = STATE;
		//TODO: change to represent current gamestate
		logger.warn("Using default win state to inform!");
		STATE = E_GAME_STATE.WIN_A;
		AI_a.gameEvent();
		AI_b.gameEvent();
		
		if(state_cache == E_GAME_STATE.PLAYER_A){
			AI_a.goBackHistory();
			STATE = E_GAME_STATE.PLAYER_B;
		}else if(state_cache == E_GAME_STATE.PLAYER_B){
			AI_b.goBackHistory();
			STATE = E_GAME_STATE.PLAYER_A;
		}else{
			return;
		}
		logger.info(()->super.getprintedGameState());
		logger.exit();
	}
	
	/**
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_A(){
		if(!AI_a.getMove()){
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING)
				handle_KI_moveless();
		}
	}
	
	/**
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_B(){
		if(!AI_b.getMove()){
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING)
				handle_KI_moveless();
		}
	}
	
	private void addHistory(){
		synchronized (lock) {
			if(!matchHistory.add(copyField(FIELD))){
				logger.error("Couldn't insert into list");
				logger.debug(getPrintedHistory());
			}
		}
	}
	
	/**
	 * Handle wins, calling animation functions etc
	 * @param ws
	 * @author Aron Heinecke
	 */
	protected synchronized void handleWin(WinStore ws){
		if(ws.isCapitulation()){
			logger.debug("Capitulation");
		}else{
			logger.debug("Point A:{}|{} B:{}|{}",ws.getPoint_a().getX(),ws.getPoint_a().getY(),ws.getPoint_b().getX(),ws.getPoint_b().getY());
		}
		logger.debug("State: {}",ws.getState());
		
		checkHistory();
		
		STATE = ws.getState();
		LASTWIN = ws;
		informAIs();
		//TODO: run handle code for winner display etc
	}
	
	protected synchronized void moveAgain(){
		logger.entry();
		logger.info(()->super.getprintedGameState());
		logger.info(getPrintedHistory());
		go_back_history(false);
		STATE = LAST_PLAYER;
		logger.info(()->super.getprintedGameState());
	}
	
	private String getPrintedHistory(){
		StringBuilder sb = new StringBuilder();
		sb.append("Match History:");
		sb.append(matchHistory.size());
		sb.append(" \n");
		for(E_FIELD_STATE[][] entry : matchHistory){
			sb.append("Entry:\n");
			for (int y = (Y_MAX-1); y >= 0; y--){
				sb.append(y+"\t");
				for (int x = 0; x < X_MAX; x++){
					sb.append("|"+printConvGamest(entry[x][y]));
				}
				sb.append("|\n");
			}
		}
		return sb.toString();
	}
	
	/**
	 * Set to true to go back history - 1
	 * @param second
	 */
	private void go_back_history(boolean second){
		logger.entry();
		int size = matchHistory.size() - 1;
		if(size >= 1){
			if(second){
				matchHistory.remove(size);
				size--;
				MOVES--;
			}
			MOVES--;
			FIELD = copyField(matchHistory.get(size));
		}else{
			logger.error("toot little to go back");
		}
	}
	
	protected synchronized void informAIs(){
		logger.entry();
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
			switch(LAST_PLAYER){
			case PLAYER_A:
				AI_a.gameEvent();
				AI_a.goBackHistory();
				break;
			case PLAYER_B:
				AI_b.gameEvent();
				AI_b.goBackHistory();
				break;
			default:
				logger.warn("no player!");
				break;
			}
			moveAgain();
		}else if(GAMEMODE == E_GAME_MODE.KI_INTERNAL){
			AI_a.gameEvent();
			AI_b.gameEvent();
		}else if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
			AI_a.gameEvent();
		}
	}
	
	/**
	 * Sets the current game to restart mode
	 * This is used only for KI internals where dataraces are comming
	 */
	public void restart(){
		logger.info("Restarting");
		STATE = E_GAME_STATE.RESTART;
	}
	
	/**
	 * Set draw for game, for KI internal/training only
	 */
	public void setDraw(){
		if(GAMEMODE == E_GAME_MODE.KI_INTERNAL || GAMEMODE == E_GAME_MODE.KI_TRAINING){
			handelDraw();
		}else{
			logger.error("Not allowed in this mode!");
		}
	}
	
	public synchronized void capitulate(E_PLAYER player){
		synchronized (lock) {
			WinStore ws = new WinStore(STATE);
			handleWin(ws);
		}
	}
}
