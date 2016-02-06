package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.Controller.E_FIELD_STATE;
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
	private E db;
	private Move MOVE_LAST;
	private Move MOVE_CURRENT;
	private E_PLAYER player;
	private boolean learning;
	private boolean DRAWING; // true if knowingly drawing
	private boolean LOOSING; // true if knowingly loosing
	
	public KBS(E db, boolean learning){
		this.db = db;
		this.learning = learning;
		logger.info("Knowledge based system initializing..");
	}
	
	@Override
	public void getMove() {
		logger.entry(player);
		if(GController.getGameState() != E_GAME_STATE.PLAYER_A && GController.getGameState() != E_GAME_STATE.PLAYER_B){
			logger.warn("Game already ended");
			return;
		}
		List<Move> moves = db.getMoves(GController.getFieldState());
		if(moves.isEmpty()){
			List<Integer> possibilities = getPossibilities();
			Move move = db.insertMoves(GController.getFieldState(), possibilities);
			if(move == null){
				logger.error("No moves!");
				return;
			}
			useMove(move);
		}else{
			Move move;
			Move win = null;
			Move draw = null;
			for(int x = 0; x < moves.size(); x++){
				move = moves.get(x);
				if(move.isUsed()){
					if(move.isDraw()){
						draw = move;
					}else if(!move.isLoose()){
						win = move;
					}
				}else if(learning){
					useMove(move);
					logger.exit();
					return;
				}
			}
			
			if(win != null){ // win move
				logger.debug("Found win move");
				useMove(win);
			}else if(draw != null){ // no winning, but draw move
				logger.debug("Found draw move");
				if(!DRAWING){
					logger.debug("Going to draw");
					MOVE_CURRENT.setDraw(true);
					db.setMove(MOVE_CURRENT);
					
					DRAWING = true;
				}
				
				logger.debug("Using draw move! {}",player);
				useMove(draw);
			}else{ // no win or draw -> loose, if not already in loosing mode, set current (now last) move as loose
				if(!LOOSING){
					logger.debug("Going to loose");
					MOVE_CURRENT.setLoose(true);
					db.setMove(MOVE_CURRENT);
					LOOSING = true;
				}
				
				logger.debug("Capitulation state for AI {}",player);
				GController.capitulate(player);
				MOVE_LAST = MOVE_CURRENT;
				MOVE_CURRENT = moves.get(0);
			}
		}
		logger.exit();
	}
	
	/**
	 * Use chosen move
	 * Handels old moves etc
	 * @param move
	 */
	private void useMove(Move move){
		logger.entry(player);
		move.setUsed(true);
		logger.info("{}",move.getMove());
		if(MOVE_CURRENT != null){
			MOVE_CURRENT.setUsed(true);
			MOVE_LAST = MOVE_CURRENT;
			MOVE_CURRENT = move;
			db.setMove(MOVE_LAST);
		}else{
			MOVE_CURRENT = move;
		}
		if(!GController.insertStone(MOVE_CURRENT.getMove())){
			logger.error("Couldn't insert stone! Wrong move!");
		}
		logger.exit();
	}
	
	@Override
	public void gameEvent() {
		logger.entry(player);
		E_GAME_STATE state = GController.getGameState();
		switch(state){
		case DRAW:
			if(LOOSING){ // only for logic tests
				logger.error("Loosing:{} state but drawing!",LOOSING);
			}
			if(!DRAWING){
				this.MOVE_CURRENT.setDraw(true);
				this.MOVE_CURRENT.setLoose(false);
				logger.debug("{}",MOVE_CURRENT.toString());
				db.setMove(MOVE_CURRENT);
			}
			break;
		case WIN_A:
		case WIN_B:
			if(checkWinnerMatcH(state)){
				if(LOOSING || DRAWING){ // only for logic tests
					logger.error("Loosing:{} Drawing:{} state but winning!",LOOSING,DRAWING);
				}
				this.MOVE_CURRENT.setLoose(false);
				logger.debug("{}",MOVE_CURRENT.toString());
				db.setMove(MOVE_CURRENT);
			}else{
				if(DRAWING){ // only for logic tests
					logger.error("Drawing:{} state but loosing!",DRAWING);
				}
				if(!LOOSING){
					this.MOVE_CURRENT.setLoose(true);
					logger.debug("{}",MOVE_CURRENT.toString());
					db.setMove(MOVE_CURRENT);
				}
			}
			break;
		default:
			break;
		}
		
		MOVE_CURRENT = null;
		MOVE_LAST = null;
		logger.exit();
	}

	@Override
	public void shutdown() {
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry(player);
		MOVE_LAST = null;
		MOVE_CURRENT = null;
		DRAWING = false;
		LOOSING = false;
		this.player = player;
	}
	
	private boolean checkWinnerMatcH(E_GAME_STATE state){
		if(this.player == E_PLAYER.PLAYER_A && state == E_GAME_STATE.PLAYER_A){
			return true;
		}else if(this.player == E_PLAYER.PLAYER_B && state == E_GAME_STATE.PLAYER_B){
			return true;
		}else{
			return false;
		}
	}
	
	private List<Integer> getPossibilities(){
		List<Integer> possibilities = new ArrayList<Integer>();
		E_FIELD_STATE[][] field = GController.getFieldState();
		for(int x = 0; x < GController.getX_MAX(); x++){
			for(E_FIELD_STATE state : field[x]){
				if(state == E_FIELD_STATE.NONE){
					possibilities.add(x);
					break;
				}
			}
		}
		return possibilities;
	}

	@Override
	public void preProcessing() {
		// TODO Auto-generated method stub
		
	}
	
}
