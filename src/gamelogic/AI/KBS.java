package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.Controller.E_GAME_STATE;
import gamelogic.Controller.E_PLAYER;
import gamelogic.GController;

/**
 * Knowledge based AI system
 * @author Aron Heinecke
 * 
 * @param <E> needs a DB handler
 */
public class KBS<E extends DB> implements AI {
	
	private Logger logger = LogManager.getLogger("AI");
	private Logger logstate = LogManager.getLogger("AI-TEST");
	private E db;
	private Move MOVE_CURRENT;
	private E_PLAYER player;
	private boolean LEARNING;
	private boolean SHUTDOWN = false;
	
	private final boolean follow_unused = false;
	private List<Move> follow = new ArrayList<Move>();
	
	public KBS(E db, boolean learning){
		this.db = db;
		this.LEARNING = learning;
		logger.info("Knowledge based system initializing..");
	}
	
	@Override
	public void getMove() {
		logger.entry(player);
		
		if(SHUTDOWN){
			logger.debug("Shutdown set");
			return;
		}
		if(GController.getGameState() != E_GAME_STATE.PLAYER_A && GController.getGameState() != E_GAME_STATE.PLAYER_B){
			logger.info("Game already ended");
			return;
		}
		
		List<Move> moves = db.getMoves(GController.getFieldState());
		if(moves.isEmpty()){
			List<Integer> possibilities = GController.getPossibilities();
			logger.debug("Possibilities: {}",possibilities);
			Move move = db.insertMoves(GController.getFieldState(), possibilities);
			if(move == null){ // can happen on concurrency
				logger.info("No moves!");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					logger.warn(e);
				}
				getMove();
				return;
			}
			useMove(move,false);
		}else{
			moveWithDraws(moves);
		}
		
		logger.exit();
	}
	
	private void moveWithDraws(List<Move> moves){ // this function treats draws & looses different
		Move win = null;
		Move draw = null;
		boolean found_loose = false;
		for(Move move : moves){
			if(move.isUsed()){
				if(!move.isLoose() && !move.isDraw()){
					win = move;
					//break;
				}else if(move.isDraw()){
					draw = move;
					//break;
				}else{
					found_loose = true;
				}
			}else if(LEARNING){
				useMove(move,false);
				logger.exit();
				return;
			}else{
				logger.warn("Not in learning mode, but unused move found!");
				return;
			}
		}
		
		if(win != null){ // win move (or not played till now)
			logger.debug("Found win move");
			useMove(win,found_loose);
		}else if(draw != null){ // no winning, but draw move
			logger.debug("Found draw move");
			useMove(draw,found_loose);
		}else{ // no win or draw -> loose, if not already in loosing mode, set current (now last) move as loose
			useMove(moves.get(0),true);
		}
	}
	
	/**
	 * Uses a move
	 * Handles Draw/Win/loose child moves
	 * 
	 * @param move Expects to get a draw or worse loose move only if there's no alternative.
	 * Otherwise this algorithm will fail.
	 */
	private void useMove(Move move,boolean found_loose){
		logger.entry(player);
		logger.debug("Move: {}",()->move.toString());
		
		if(follow_unused){
			follow.add(move);
		}
		
		if(MOVE_CURRENT != null){
			// handle loose/draw marks
			if(move.isLoose()){
				logger.debug("Going to loose");
				MOVE_CURRENT.setLoose(true);
			}else if(move.isDraw()){
				logger.debug("Going to draw");
				MOVE_CURRENT.setDraw(true);
			}
			// set used
			if(move.isUsed()){ // only update parent if child also set
				MOVE_CURRENT.setUsed(true);
				if(!db.setMove(MOVE_CURRENT)){ // data race, table locking
					logger.warn("Restarting!");
					GController.restart();
					return;
				}
				logstate.debug("Marked");
			}
		}
		
		// handle deletes after parent update (crash security)
		if(move.isLoose()){
			if(db.deleteMoves(move.getField())){
				logger.debug("Capitulation state for AI {}",player);
				GController.capitulate(player);
			}else{ // datarace, table locking
				logger.warn("Restarting!");
				GController.restart();
				return;
			}
		}else if(found_loose){
			if(!db.deleteLooses(move.getField())){ // data race, table locking
				logger.warn("Restarting!");
				GController.restart();
				return;
			}
		}
		MOVE_CURRENT = move;
		// use move
		if(!move.isLoose()){
			if(!GController.insertStone(MOVE_CURRENT.getMove())){
				logger.error("Couldn't insert stone! Wrong move! {} \n{}",MOVE_CURRENT.getMove(),GController.getprintedGameState());
				logger.error(move.toString());
			}
		}
		logger.exit();
	}
	
	@Override
	public void gameEvent() {
		logger.entry(player);
		if(SHUTDOWN){
			logger.debug("Shutdown set");
			return;
		}
		E_GAME_STATE state = GController.getGameState();
		switch(state){
		case DRAW:
			if(follow_unused)
				print_follow();
			
			if(!MOVE_CURRENT.isDraw()){
				this.MOVE_CURRENT.setUsed(true);
				this.MOVE_CURRENT.setDraw(true);
				logger.debug("{}",()->MOVE_CURRENT.toString());
				if(!db.setMove(MOVE_CURRENT)){
					logger.warn("Restarting");
					GController.restart();
				}
			}
			break;
		case WIN_A:
		case WIN_B:
			if(follow_unused)
				print_follow();
			
			if(MOVE_CURRENT == null){
				logger.error("No current move on win state! \n{}",GController.getprintedGameState());
				return;
			}
			this.MOVE_CURRENT.setUsed(true);
			if(checkWinnerMatch(state)){
				if(this.MOVE_CURRENT.isLoose()){ // impossible if ai works
					logger.warn("Ignoring possible win-loose");
				}else{
					logger.debug("{}",()->MOVE_CURRENT.toString());
					if(!db.setMove(MOVE_CURRENT)){
						logger.warn("Restarting");
						GController.restart();
					}
				}
			}else{
				if(!this.MOVE_CURRENT.isLoose()){
					this.MOVE_CURRENT.setLoose(true);
					logger.debug("{}",()->MOVE_CURRENT.toString());
					if(!db.setMove(MOVE_CURRENT)){
						logger.warn("Restarting");
						GController.restart();
					}
				}
			}
			break;
		default:
			break;
		}
		MOVE_CURRENT = null;
		logger.exit();
	}
	
	private void print_follow(){
		StringBuilder sb = new StringBuilder();
		for(Move move : follow){
			sb.append(move.toString());
			sb.append("\n");
		}
		logger.warn("Follow: {} \n{}",this.player,sb.toString());
	}

	@Override
	public void shutdown() {
		this.SHUTDOWN = true;
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry(player);
		MOVE_CURRENT = null;
		this.player = player;
	}
	
	private boolean checkWinnerMatch(E_GAME_STATE state){
		if(this.player == E_PLAYER.PLAYER_A && state == E_GAME_STATE.WIN_A){
			return true;
		}else if(this.player == E_PLAYER.PLAYER_B && state == E_GAME_STATE.WIN_B){
			return true;
		}else{
			return false;
		}
	}
	
	@Override
	public void preProcessing() {
		// TODO Auto-generated method stub
		
	}
	
}
