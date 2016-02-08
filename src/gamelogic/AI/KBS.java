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
			logger.info("Game already ended");
			return;
		}
		List<Move> moves = db.getMoves(GController.getFieldState());
		if(moves.isEmpty()){
			List<Integer> possibilities = getPossibilities();
			Move move = db.insertMoves(GController.getFieldState(), possibilities);
			if(move == null){// can happen on concurrency
				logger.info("No moves!");
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					logger.warn(e);
				}
				getMove();
				return;
			}
			useMove(move);
		}else{
			moveWithDraws(moves);
		}
		
		logger.exit();
	}
	
	private void moveWithDraws(List<Move> moves){ // this function treats draws & looses different
		Move win = null;
		Move draw = null;
		for(Move move : moves){
			if(move.isUsed()){
				if(!move.isLoose() && !move.isDraw()){
					win = move;
					//break;
				}else if(move.isDraw()){
					draw = move;
					//break;
				}
			}else if(learning){
				useMove(move);
				logger.exit();
				return;
			}
		}
		
		if(win != null){ // win move (or not played till now)
			logger.debug("Found win move");
			useMove(win);
		}else if(draw != null){ // no winning, but draw move
			logger.debug("Found draw move");
			if(!DRAWING){
				logger.debug("Going to draw");
				if(MOVE_CURRENT != null){
					MOVE_CURRENT.setDraw(true);
					if(!db.setMove(MOVE_CURRENT)){  // datarace, table locking
						GController.restart();
						return;
					}
				}
				DRAWING = true;
			}
			
			if(db.deleteLooses(draw.getField())){
				logger.debug("Using draw move! {}",player);
				useMove(draw);
			}else{ // datarace, table locking
				GController.restart();
			}
		}else{ // no win or draw -> loose, if not already in loosing mode, set current (now last) move as loose
			if(!LOOSING){
				logger.debug("Going to loose");
				if(MOVE_CURRENT != null){
					MOVE_CURRENT.setLoose(true);
					if(!db.setMove(MOVE_CURRENT)){  // datarace, table locking
						GController.restart();
						return;
					}
				}
				LOOSING = true;
			}
			
			if(db.deleteMoves(moves.get(0).getField())){
				logger.debug("Capitulation state for AI {}",player);
				GController.capitulate(player);
			}else{ // datarace, table locking
				GController.restart();
			}
			
//				MOVE_LAST = MOVE_CURRENT;
//				MOVE_CURRENT = moves.get(0);
		}
	}
	
	@SuppressWarnings("unused")
	private void moveWithoutDraws(List<Move> moves){ // this function deletes also draws, treating them as looses
		Move win = null;
		for(Move move : moves){
			if(move.isUsed()){
				if(!move.isLoose() && !move.isDraw()){
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
		}else{ // no win or draw -> loose, if not already in loosing mode, set current (now last) move as loose
			if(!LOOSING){
				logger.debug("Going to loose");
				if(MOVE_CURRENT != null){
					MOVE_CURRENT.setLoose(true);
					if(!db.setMove(MOVE_CURRENT)){  // datarace, table locking
						GController.restart();
						return;
					}
				}
				LOOSING = true;
			}
			
//			if(db.deleteDrawAndLooses(moves.get(0).getField())){
//				logger.debug("Capitulation state for AI {}",player);
//				GController.capitulate(player);
//			}else{ // datarace, table locking
//				GController.restart();
//			}
			
//				MOVE_LAST = MOVE_CURRENT;
//				MOVE_CURRENT = moves.get(0);
		}
	}
	
	/**
	 * Use move, set last to used
	 * @param move
	 */
	private void useMove(Move move){
		logger.entry(player);
		move.setUsed(true);
		logger.info("{}",move.getMove());
		if(MOVE_CURRENT != null){
			MOVE_CURRENT.setUsed(true);
			if(db.setMove(MOVE_CURRENT)){
				MOVE_CURRENT = move;
			}else{ // datarace, table locking
				GController.restart();
				return;
			}
		}else{
			MOVE_CURRENT = move;
		}
		if(!GController.insertStone(MOVE_CURRENT.getMove())){
			logger.error("Couldn't insert stone! Wrong move!  \n{}",MOVE_CURRENT.getMove(),GController.getprintedGameState());
		}
		logger.exit();
	}
	
	@Override
	public void gameEvent() {
		logger.entry(player);
		E_GAME_STATE state = GController.getGameState();
		switch(state){
		case DRAW:
			if(!DRAWING){
				this.MOVE_CURRENT.setDraw(true);
				logger.debug("{}",MOVE_CURRENT.toString());
				db.setMove(MOVE_CURRENT);
			}
			break;
		case WIN_A:
		case WIN_B:
			if(checkWinnerMatcH(state)){
				if(this.MOVE_CURRENT.isLoose()){ // schlechter gegner, macht zug nicht valide
					logger.warn("Ignoring possible win-loose");
				}else{
					logger.debug("{}",MOVE_CURRENT.toString());
					db.setMove(MOVE_CURRENT);
				}
			}else{
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
		logger.exit();
	}

	@Override
	public void shutdown() {
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry(player);
		MOVE_CURRENT = null;
		DRAWING = false;
		LOOSING = false;
		this.player = player;
	}
	
	private boolean checkWinnerMatcH(E_GAME_STATE state){
		logger.info("Player {} winner={}",this.player);
		if(this.player == E_PLAYER.PLAYER_A && state == E_GAME_STATE.WIN_A){
			return true;
		}else if(this.player == E_PLAYER.PLAYER_B && state == E_GAME_STATE.WIN_B){
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
