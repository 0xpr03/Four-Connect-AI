package gamelogic;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
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
			
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
				matchHistory = new ArrayList<E_FIELD_STATE[][]>();
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
			case MULTIPLAYER:
			case SINGLE_PLAYER:
			case KI_INTERNAL:
			case KI_TRAINING:
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
			
			if(found_place != -1) {
				MOVES++;
				WinStore ws = checkWin(column,found_place);
				if(ws == null){ // no win
					if(checkDraw()){ // is draw
						handelDraw();
					}else{ // no draw, no win, next move
						if(matchHistory != null)
							matchHistory.add(FIELD);
						STATE = STATE == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.PLAYER_B : E_GAME_STATE.PLAYER_A;
					}
				}else{
					handleWin(ws);
				}
			}else{
				logger.info("No more column space to insert any stones!");
			}
			
			//TODO: call graphics && let it callback the next run
		}
		return found_place != -1;
	}
	
	protected synchronized void checkHistory(){
		logger.entry();
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
	protected void handle_KI_moveless(){
		logger.entry();
		int size = matchHistory.size() -1;
		if(size >= 1){
			matchHistory.remove(size);
			size--;
		}
		FIELD = matchHistory.get(size);
		MOVES -= 2;
		if(STATE == E_GAME_STATE.PLAYER_A){
			STATE = E_GAME_STATE.PLAYER_B;
		}else if(STATE == E_GAME_STATE.PLAYER_B){
			STATE = E_GAME_STATE.PLAYER_A;
		}else{
			return;
		}
		
		logger.exit();
	}
	
	/**
	 * 
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_A(){
		if(!AI_a.getMove()){
			handle_KI_moveless();
		}
	}
	
	/**
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_B(){
		if(!AI_b.getMove()){
			handle_KI_moveless();
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
		int size = matchHistory.size() -1;
		if(size >= 1){
			matchHistory.remove(matchHistory.size()-1);
			size --;
		}
		MOVES--;
		FIELD = matchHistory.get(size);
		STATE = LAST_PLAYER;
	}
	
	protected synchronized void informAIs(){
		logger.entry();
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
			switch(LAST_PLAYER){
			case PLAYER_A:
				AI_a.gameEvent();
				break;
			case PLAYER_B:
				AI_b.gameEvent();
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
