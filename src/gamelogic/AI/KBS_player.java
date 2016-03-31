package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.GController;
import gamelogic.ControllerBase.E_PLAYER;

public class KBS_player<E extends DB> implements AI {
	private Logger logger = LogManager.getLogger("AI");
	private E db;
	private E_PLAYER player;
	
	public KBS_player(E db){
		this.db = db;
	}

	@Override
	public boolean getMove() {
		logger.entry();
		SelectResult moves = db.getMoves(GController.getFieldState(),this.player==E_PLAYER.PLAYER_A);
		if(moves.isEmpty()){
			logger.error("No moves!");
		}else{
			int move = -1;
			if(!moves.getWins().isEmpty()){
				List<Move> possibilities = new ArrayList<Move>();
				for(Move moveelem : moves.getWins()){
					if(!moveelem.isDraw()){
						possibilities.add(moveelem);
					}
				}
				if(!possibilities.isEmpty()){
					logger.debug("Using win move {}",possibilities.get(0).toString());
					move = possibilities.get(0).getMove();
				}else{
					logger.debug("Using possible win move {}",moves.getWins().get(0).toString());
					move = moves.getWins().get(0).getMove();
				}
			}else if(!moves.getDraws().isEmpty()) {
				List<Move> possibilities = new ArrayList<Move>();
				for(Move moveelem : moves.getDraws()){
					if(!moveelem.isLoose()){
						possibilities.add(moveelem);
					}
				}
				if(!possibilities.isEmpty()){
					logger.debug("Using draw move {}",possibilities.get(0).toString());
					move = possibilities.get(0).getMove();
				}else{
					logger.debug("Using possible draw move {}",moves.getDraws().get(0).toString());
					move = moves.getDraws().get(0).getMove();
				}
			}else{
				if(moves.getUnused().isEmpty()){
					logger.debug("Going to capitulate");
				}else{
					logger.error("Capitulating, found {} unused moves!",moves.getUnused().size());
				}
				GController.capitulate();
				return false;
			}
			GController.insertStone(move);
		}
		return false;
	}

	@Override
	public void shutdown() {
		logger.entry();
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry();
		this.player = player;
	}

	@Override
	@Deprecated
	public void preProcessing() {
		// TODO Auto-generated method stub
		
	}

	@Override
	@Deprecated
	public void goBackHistory(boolean allowEmpty) {
		// TODO Auto-generated method stub
		
	}

	@Override
	@Deprecated
	public boolean hasMoreMoves() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	@Deprecated
	public void getOutcome() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	@Deprecated
	public void gameEvent(boolean rollback) {
		
	}
	
}
