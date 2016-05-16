package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.ControllerBase.E_PLAYER;
import gamelogic.GController;

/**
 * Knowledge based AI system
 * @author Aron Heinecke
 * 
 * @param <E> needs a DB handler
 */
public class KBS2<E extends DB> implements AI {
	
	private Logger logger = LogManager.getLogger("AI");
	private Logger logstate = LogManager.getLogger("AI-TEST");
	private E db;
	private Move MOVE_CURRENT;
	private E_PLAYER player;
	private boolean SHUTDOWN = false;
	
	private boolean follow_unused = false;
	private List<Move> follow = new ArrayList<Move>();
	
	public KBS2(E db){
		this.db = db;
		logger.info("Knowledge based system initializing..");
	}
	
	@Override
	public boolean getMove() {
		logger.entry(player);
		
		if(SHUTDOWN){
			logger.debug("Shutdown set");
			return true;
		}
		if(GController.getGameState() != E_GAME_STATE.PLAYER_A && GController.getGameState() != E_GAME_STATE.PLAYER_B){
			logger.info("Game already ended");
			return true;
		}
		
		SelectResult sr = db.getMoves(GController.getFieldState(), player == E_PLAYER.PLAYER_A);
		if(sr.isEmpty()){
			List<Integer> possibilities = GController.getPossibilities();
			logger.debug("Possibilities: {}",possibilities);
			sr = db.insertMoves(GController.getFieldState(), possibilities, player == E_PLAYER.PLAYER_A);
			if(sr == null){ // can happen on concurrency
				logger.info("No moves!");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					logger.warn(e);
				}
				return false;
			}
			useMove(sr.getUnused().get(ThreadLocalRandom.current().nextInt(0, sr.getUnused().size())),false);
		}else{
			moveWithDraws(sr);
		}
		
		
		logger.exit();
		return true;
	}
	
	/**
	 * Select the best possible move and use it
	 * @param sr
	 */
	private void moveWithDraws(SelectResult sr){ // this function treats draws & looses different
		Move move;
		boolean found_loose = !sr.getLooses().isEmpty();
		if(sr.getWins().size() > 0 || sr.getUnused().size() > 0){ // win move (or not played till now)
			logger.debug("Found win move");
			if(sr.getUnused().size() > 0 )
				move = sr.getUnused().get(ThreadLocalRandom.current().nextInt(0, sr.getUnused().size()));
			else
				move = sr.getWins().get(ThreadLocalRandom.current().nextInt(0, sr.getWins().size()));
			useMove(move,found_loose);
		}else if(!sr.getDraws().isEmpty()){ // no winning, but draw move
			logger.debug("Found draw move");
			useMove(sr.getDraws().get(ThreadLocalRandom.current().nextInt(0, sr.getDraws().size())),found_loose);
		}else{ // no win or draw -> loose, if not already in loosing mode, set current (now last) move as loose
			useMove(sr.getLooses().get(0),true);
		}
	}
	
	/**
	 * Uses a move
	 * Handles Draw/Win/loose child moves
	 * 
	 * @param move Expects to get a draw or worse loose move only if there's no alternative.<br>
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
			if(MOVE_CURRENT != null){
				if(!db.deleteMoves(move.getFID(), move.isPlayer_a())){ // datarace, table locking
					logger.warn("Restarting!");
					GController.restart();
					return;
				}
			}else{
				logger.warn("Can't delete root");
			}
			logger.debug("Capitulation state for AI {}",player);
			GController.capitulate();
		}else if(found_loose){
			if(!db.deleteLooses(move.getFID(), move.isPlayer_a())){ // data race, table locking
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
	public void gameEvent(boolean _rollback_) {
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
				follow_unused = true;
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
		follow.clear();
	}

	@Override
	public void shutdown() {
		if(SHUTDOWN){
			return;
		}
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
	public void preProcess() {
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
	
}
