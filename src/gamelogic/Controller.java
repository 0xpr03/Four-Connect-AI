package gamelogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.AI.AI;
import main.MainGameLoop;
import main.MainGameLoop.Color;

/**
 * Controller for the four connect game<br>
 * Handles all game related things such as who is supposed to move,
 * who won / draw etc<br>
 * The Controller is also able to play number-crunching games for a
 * knowledge based system in learning mode. (KBS2)
 * This includes running through all possible game states by rolling
 * back the game field to an earlier state.
 * This class is NOT supposed to be created and used as object except
 * for testing purposes. The global calls are done via GController.
 * @author Aron Heinecke
 * @param <E>
 */
public final class Controller extends ControllerBase {
	
	private Logger logger = LogManager.getLogger("Controller");
	
	/**
	 * Create a new Controller with AI's
	 * @param kbs_trainer default AI for singleplayer
	 * @param kbs_trainer2 AI for training & tests of AI vs AI
	 */
	public Controller(AI kbs_trainer, AI kbs_trainer2) {
		super(kbs_trainer, kbs_trainer2);
	}
	
	/**
	 * Create a new, AI less, Controller
	 */
	public Controller() {
		super();
	}

	// for ki training
	private List<E_FIELD_STATE[][]> matchHistory;
	private E_GAME_STATE LAST_PLAYER;
	
	/**
	 * If player x does a move, leading to a game end, we've to go back two times
	 * But if we're resetting without this last move, we only must go back one times
	 * ALLOW_BACK_BOTH is true, if we're allowing to go back on both ai's
	 */
	private boolean ALLOW_BACK_BOTH = false;
	
	/**
	 * Initialize a new Game
	 * @param gamemode on multiplayer / singleplayer the starting player will be selected randomly
	 * @param loglevel define a loglevel used for this game
	 * everthing else it'll be player a
	 * @param x_max x size of the field
	 * @param y_max y size of the field
	 */
	public synchronized void initGame(E_GAME_MODE gamemode, Level loglevel, int x_max, int y_max) {
		logger.entry(gamemode, loglevel, x_max, y_max);
		if(loglevel != null)
			Configurator.setLevel(LogManager.getLogger(ControllerBase.class).getName(), loglevel);
		synchronized(lock){
			if (gamemode == E_GAME_MODE.NONE){
				logger.error("Wrong game mode! {}",gamemode);
			}
			LAST_GAME = null;
			STATE = E_GAME_STATE.NONE;
			super.X_MAX = x_max;
			super.Y_MAX = y_max;
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
			
			if (GAMEMODE == E_GAME_MODE.TESTING || GAMEMODE == E_GAME_MODE.KI_TRAINING){
				logger.warn("All manipulations are enabled in this mode! {}",GAMEMODE);
			}
			MOVES = 0;
		}
	}
	
	/**
	 * Initialize a new game
	 * @param gamemode
	 */
	public void initGame(E_GAME_MODE gamemode,int x_max,int y_max){
		initGame(gamemode, LogManager.getRootLogger().getLevel(), x_max, y_max);
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
				ALLOW_BACK_BOTH = false;
				logger.debug("Using hard coded state!");
				STATE = E_GAME_STATE.PLAYER_A;
				start_AI();
				break;
			case SINGLE_PLAYER:
//				STATE = E_GAME_STATE.PLAYER_B;
//				break;
			case MULTIPLAYER:
			case KI_INTERNAL:
			case FUZZING:
				STATE = Math.random() < 0.5 ? E_GAME_STATE.PLAYER_A : E_GAME_STATE.PLAYER_B;
				start_AI();
				break;
			default:
				STATE = E_GAME_STATE.PLAYER_A;
				break;
			}
		}
	}

	/**
	 * Insert stone
	 * @param column value between 0-6
	 * @return returns false on failure
	 * @author Aron Heinecke
	 */
	public synchronized boolean insertStone(final int column){
		logger.entry(STATE);
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
			GameStore ws = checkWin(column,found_place);
			if(STATE == E_GAME_STATE.PLAYER_B && GAMEMODE == E_GAME_MODE.SINGLE_PLAYER)
				MainGameLoop.setStone(column, Color.YELLOW);
			if(ws == null){ // no win
				if(checkDraw()){ // is draw
					ALLOW_BACK_BOTH = true; // we're on a state where the last move leaded to a gameEvent sit.
					handelDraw();
				}else{ // no draw, no win, next move
					if(matchHistory != null){
						addHistory();
					}
					STATE = STATE == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.PLAYER_B : E_GAME_STATE.PLAYER_A;
					if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER || GAMEMODE == E_GAME_MODE.TESTING){
						doPrefetch();
					}
				}
			}else{
				ALLOW_BACK_BOTH = true; // we're on a state where the last move leaded to a gameEvent sit.
				handleWin(ws);
			}
		}else{
			logger.info("No more column space to insert any stones!");
		}
			
			//TODO: call graphics && let it callback the next run
		return found_place != -1;
	}
	
	private void doPrefetch(){
		switch(STATE){
		case PLAYER_B:
			AI_a.preProcess();
			break;
		default:
			break;
		
		}
	}
	
	/**
	 * Copies the field, creating a full copy
	 * @param origin
	 * @return
	 */
	private E_FIELD_STATE[][] copyField(E_FIELD_STATE[][] origin){
		E_FIELD_STATE[][] copy = new E_FIELD_STATE[X_MAX][Y_MAX];
		for(int x = 0; x < X_MAX; x++){
			copy[x] = Arrays.copyOf(origin[x],Y_MAX);
		}
		return copy;
	}
	
	/**
	 * Sets the LAST_PLAYER to the current state
	 * called before win/draw/ states are set
	 */
	protected synchronized void checkHistory(){
		logger.entry();
		logger.debug(()->super.getprintedGameState());
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING)
			LAST_PLAYER = STATE;
	}
	
	/**
	 * Handle draws, calling everything needed
	 */
	protected synchronized void handelDraw(){
		checkHistory();
		STATE = E_GAME_STATE.DRAW;
		logger.debug("State: {}",STATE);
		
		super.LAST_GAME = new GameStore();
		informAIs(true);
		
		if(GAMEMODE == E_GAME_MODE.MULTIPLAYER || GAMEMODE == E_GAME_MODE.SINGLE_PLAYER)
			MainGameLoop.renderGameEnd(LAST_GAME);
	}
	
	/**
	 * Handle wins, calling animation functions etc
	 * @param ws
	 * @author Aron Heinecke
	 */
	protected synchronized void handleWin(GameStore ws){
		if(ws.isCapitulation()){
			logger.debug("Capitulation");
		}else{
			logger.debug("Point A:{}|{} B:{}|{}",()->ws.getPoint_a().getX(),()->ws.getPoint_a().getY(),()->ws.getPoint_b().getX(),()->ws.getPoint_b().getY());
		}
		logger.debug("State: {}",()->ws.getState());
		
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
			checkHistory();
		}
		
		STATE = ws.getState();
		LAST_GAME = ws;
		informAIs(true);
		
		if(GAMEMODE == E_GAME_MODE.MULTIPLAYER || GAMEMODE == E_GAME_MODE.SINGLE_PLAYER)
			MainGameLoop.renderGameEnd(LAST_GAME);
	}

	/**
	 * Curent KI is finished with a all moves
	 * Go back & lat KI before move
	 */
	private void handle_KI_moveless(){
		logger.entry(ALLOW_BACK_BOTH);
		logger.debug(()->super.getprintedGameState());
		
		E_GAME_STATE state_cache = STATE;
		// if rollback without move, there's no last win
		E_GAME_STATE last_win = super.LAST_GAME == null ? E_GAME_STATE.NONE : super.LAST_GAME.getState();
		go_back_history(true);
		if(MOVES == 0){
			logger.fatal("Shutting down, moves are {} \n{}",MOVES,GController.getprintedGameState());
			System.exit(0);
		}
		
		STATE = last_win;
		if(ALLOW_BACK_BOTH){
			logger.debug("state; {}",STATE);
			MOVES -= 2;
			// if last player, then no rollback info
			
			switch(state_cache){
			case PLAYER_A:
				AI_a.gameEvent(false);
				AI_a.getOutcome();
				AI_b.gameEvent(true);
				AI_a.goBackHistory(MOVES < 3);
				AI_b.goBackHistory(MOVES < 3);
				break;
			case PLAYER_B:
				AI_b.gameEvent(false);
				AI_b.getOutcome();
				AI_a.gameEvent(true);
				AI_b.goBackHistory(MOVES < 3);
				AI_a.goBackHistory(MOVES < 3);
				break;
			default:
				logger.error("Not supported case! {}",GController.getprintedGameState());
				break;
			}
			
			ALLOW_BACK_BOTH = false;
		}else{
			MOVES--;
			switch(state_cache){
			case PLAYER_A:
				AI_b.gameEvent(true);
				//AI_a.goBackHistory(MOVES < 3);
				AI_b.goBackHistory(MOVES < 3);
				break;
			case PLAYER_B:
				AI_a.gameEvent(true);
				//AI_b.goBackHistory(MOVES < 3);
				AI_a.goBackHistory(MOVES < 3);
				break;
			default:
				logger.error("Not supported case!");
				break;
			}
			
		}
		
		if(state_cache == E_GAME_STATE.PLAYER_A){
			STATE = E_GAME_STATE.PLAYER_B;
		}else if(state_cache == E_GAME_STATE.PLAYER_B){
			STATE = E_GAME_STATE.PLAYER_A;
		}else{
			logger.error("No state!");
			return;
		}
		
		logger.debug(()->super.getprintedGameState());
		logger.exit();
	}
	
	/**
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_A(){
		if(!AI_a.getMove()){
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
				AI_a.getOutcome();
				informAIs(false);
			}
		}
	}
	
	/**
	 * @return returns false if no unused moves remain
	 */
	public void moveAI_B(){
		if(!AI_b.getMove()){
			if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
				AI_b.getOutcome();
				informAIs(false);
			}
		}
	}
	
	/**
	 * Add field history entry
	 * AI training related
	 */
	private void addHistory(){
		synchronized (lock) {
			if(!matchHistory.add(copyField(FIELD))){
				logger.error("Couldn't insert into list");
				logger.debug(()->getPrintedHistory());
			}
		}
	}
	
	/**
	 * Go back in history and let player X move again
	 * AI training related
	 */
	protected synchronized void moveAgain(){
		logger.entry();
		logger.debug(()->super.getprintedGameState());
		//logger.debug(()->getPrintedHistory());
		go_back_history(false);
		MOVES--;
		STATE = LAST_PLAYER;
		logger.debug(()->super.getprintedGameState());
		ALLOW_BACK_BOTH = false;
		logger.exit();
	}
	
	/**
	 * Returns the field history as printed string
	 * AI training debug related
	 * @return
	 */
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
	private void go_back_history(final boolean second){
		logger.entry();
		int size = matchHistory.size() - 1; // current last entry
		if(size >= 0){
			if(second){
				if(size > 0){
					matchHistory.remove(size);
					size--;
				}else{
					logger.error("Not enough entries to go back!");
				}
			}
			FIELD = copyField(matchHistory.get(size));
			super.LAST_GAME = null;
		}else{
			logger.fatal("No history ! {}",GController.getprintedGameState());
			System.exit(1);
		}
	}
	
	/**
	 * Stops the current game
	 */
	public synchronized void stopGame(){
		logger.entry();
		switch(GAMEMODE){
		case FUZZING:
		case KI_INTERNAL:
		case KI_TRAINING:
		case SINGLE_PLAYER:
			AI_a.gameEvent(false);
			break;
		case MULTIPLAYER:
			break;
		default:
			break;
		}
	}
	
	/**
	 * Inform AI's and go back if needed
	 * @param reset if set to true we'll only reset the game
	 */
	protected synchronized void informAIs(final boolean game_end){
		logger.entry();
		if(GAMEMODE == E_GAME_MODE.KI_TRAINING){
			if(game_end){
				boolean has_moves = false;
				switch(LAST_PLAYER){
				case PLAYER_A:
					has_moves = AI_a.hasMoreMoves();
					if(has_moves){
						AI_a.gameEvent(false);
						AI_a.goBackHistory(MOVES < 3);
					}else{
						AI_a.getOutcome();
					}
					break;
				case PLAYER_B:
					has_moves = AI_b.hasMoreMoves();
					if(has_moves){
						AI_b.gameEvent(false);
						AI_b.goBackHistory(MOVES < 3);
					}else{
						AI_b.getOutcome();
					}
					break;
				default:
					logger.warn("no player!");
					break;
				}
				if(has_moves){
					logger.debug(()->GController.getprintedGameState());
					moveAgain();
					logger.debug(()->GController.getprintedGameState());
				}else{
					STATE = LAST_PLAYER;
					handle_KI_moveless();
				}
			}else{
				handle_KI_moveless();
			}
		}else if(GAMEMODE == E_GAME_MODE.KI_INTERNAL){
			AI_a.gameEvent(false);
			AI_b.gameEvent(false);
		}else if(GAMEMODE == E_GAME_MODE.SINGLE_PLAYER){
			AI_a.gameEvent(false);
		}
	}
	
	/**
	 * Sets the current game to restart mode
	 * This is used only for KI internals where dataraces are comming
	 */
	public void restart(){
		logger.debug("Restarting");
		STATE = E_GAME_STATE.RESTART;
	}
	
	/**
	 * Capitulate with current player
	 * @param player
	 */
	public synchronized void capitulate(){
		synchronized (lock) {
			GameStore ws = new GameStore(STATE);
			handleWin(ws);
		}
	}
}
