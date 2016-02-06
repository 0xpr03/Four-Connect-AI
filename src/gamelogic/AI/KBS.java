package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.Controller.E_FIELD_STATE;
import gamelogic.Controller.E_GAME_STATE;
import gamelogic.Controller.E_PLAYER;
import gamelogic.GController;

public class KBS<E extends DB> implements AI {
	
	private Logger logger = LogManager.getLogger();
	private E db;
	private Move MOVE_LAST;
	private Move MOVE_CURRENT;
	private E_PLAYER player;
	private boolean learning;
	
	public KBS(E db, boolean learning){
		this.db = db;
		this.learning = learning;
		logger.info("Knowledge based system initializing..");
	}
	
	@Override
	public void getMove() {
		List<Move> moves = db.getMoves(GController.getFieldState());
		if(moves.isEmpty()){
			db.insertMoves(GController.getFieldState(), getPossibilities());
			//TODO: generate all possibilities
		}else{
			Move move;
			Move win = null;
			Move draw = null;
			for(int x = 0; x < moves.size(); x++){
				move = moves.get(x);
				if(move.isUsed()){
					if(move.isDraw())
						draw = move;
					if(!move.isLoose())
						win = move;
				}else if(learning){
					useMove(move);
					return;
				}
			}
			
			if(win != null){
				useMove(win);
			}else if(draw != null){
				logger.debug("Using draw move! {}",player);
				useMove(draw);
			}else{
				logger.debug("Capitulation state for AI {}",player);
				GController.capitulate(player);
				MOVE_LAST = MOVE_CURRENT;
				MOVE_CURRENT = moves.get(0);
			}
		}
		// TODO Auto-generated method stub

	}
	
	/**
	 * Use chosen move
	 * Handels old moves etc
	 * @param move
	 */
	private void useMove(Move move){
		if(MOVE_CURRENT != null){
			MOVE_CURRENT.setUsed(true);
			MOVE_LAST = MOVE_CURRENT;
			MOVE_CURRENT = move;
			db.setMove(MOVE_LAST);
		}
	}
	
	@Override
	public void gameEvent() {
		E_GAME_STATE state = GController.getGameState();
		switch(state){
		case DRAW:
			this.MOVE_CURRENT.setDraw(true);
			this.MOVE_CURRENT.setLoose(false);
			db.setMove(MOVE_CURRENT);
			break;
		case WIN_A:
		case WIN_B:
			if(checkWinnerMatcH(state)){
				this.MOVE_CURRENT.setLoose(false);
			}else{
				this.MOVE_CURRENT.setLoose(true);
			}
			db.setMove(MOVE_CURRENT);
			break;
		default:
			break;
		
		}
		
		MOVE_CURRENT = null;
		MOVE_LAST = null;
	}

	@Override
	public void shutdown() {
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		MOVE_LAST = null;
		MOVE_CURRENT = null;
		this.player = player;
	}
	
	private boolean checkWinnerMatcH(E_GAME_STATE state){
		if(this.player == E_PLAYER.PLAYER_A && state == E_GAME_STATE.PLAYER_A){
			return true;
		}else if(this.player == E_PLAYER.PLAYER_B && state == E_GAME_STATE.PLAYER_B){
			return true;
		}
		return false;
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
