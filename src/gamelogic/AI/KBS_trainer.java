package gamelogic.AI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.ControllerBase.E_PLAYER;
import gamelogic.GController;

/**
 * Knowledge based AI system
 * Training version
 * @author Aron Heinecke
 * 
 * @param <E> needs a DB handler
 */
public class KBS_trainer<E extends DB> implements AI {
	
	private Logger logger = LogManager.getLogger("AI");
	private E db;
	private E_PLAYER player;
	private SelectResult moves;
	private byte[] lastField = null;
	private Move P_MOVE_CURRENT; // pointer to last moveHistory element
	private List<Move> unused;
	private List<Move> moveHistory;
	private boolean SHUTDOWN = false;
	
	private boolean follow_unused = false;
	private List<Move> follow = new ArrayList<Move>();
	
	public KBS_trainer(E db){
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
		
		Move new_move = null;
		if(Arrays.equals(lastField, db.getHash())){
			if(unused.size() != 0){
				new_move = unused.get(0);
			}
		}else{
			SelectResult moves = db.getMoves(GController.getFieldState(),this.player==E_PLAYER.PLAYER_A);
			if(moves.isEmpty()){
				List<Integer> possibilities = GController.getPossibilities();
				logger.debug("Possibilities: {}",possibilities);
				SelectResult sel = db.insertMoves(GController.getFieldState(), possibilities,player==E_PLAYER.PLAYER_A);
				if(sel == null){ // can happen on concurrency
					logger.error("Error on insert!");
					System.exit(1);
					return true;
				}
				
				lastField = db.getHash();
				unused = new ArrayList<Move>(sel.getUnused());
				new_move = unused.get(0);
				if(unused.size() != possibilities.size()){
					logger.error("Wrong list size {} poss. {}",unused.size(),possibilities.size());
				}
			}else{
				if(!moves.getUnused().isEmpty()){
					lastField = db.getHash();
					unused = new ArrayList<Move>(moves.getUnused());
					new_move = unused.get(0);
				}
			}
		}
		if(new_move == null){
			logger.debug("No new moves");
			logger.debug(()->printUnused());
			return false;
		}else{
			moveHistory.add(new_move);
			updatePointer();
			logger.debug("Using move {}",()->P_MOVE_CURRENT.toString());
			if(!GController.insertStone(P_MOVE_CURRENT.getMove())){
				logger.error("Couldn't insert stone! Wrong move! \n{}",GController.getprintedGameState());
				logger.error(P_MOVE_CURRENT.toString());
			}
		}
		logger.exit();
		return true;
	}
	
	/**
	 * Returns true if the AI has more moves in store
	 * Ignoring the first, as it's the latest used one
	 * @see gamelogic.Controller#informAIs(boolean) code
	 */
	public boolean hasMoreMoves(){
		return unused.size() > 1; // ignore current
	}
	
	private String printUnused(){
		StringBuilder sb = new StringBuilder();
		sb.append("Unused list:\n");
		for(Move move : unused) {
			sb.append(move.toString());
		}
		return sb.toString();
	}
	
	/**
	 * Set pointer P_MOVE_CURRENT to last entry in moveHistory
	 */
	private void updatePointer(){
		P_MOVE_CURRENT = moveHistory.get(moveHistory.size() -1);
	}
	
	/**
	 * Go back one move in the history
	 */
	public void goBackHistory(boolean allowEmpty){
		if(logger.isDebugEnabled()){
			logger.debug("{} {}",this.player,printHistory());
		}
		if(moveHistory.size() > 1){
			moveHistory.remove(moveHistory.size() - 1);
			updatePointer();
		}else if(allowEmpty){
			moveHistory.remove(moveHistory.size() -1 );
			P_MOVE_CURRENT = null;
		}else{
			logger.error("Can't go back one more!  Elements:{} {}",moveHistory.size(),this.player);
		}
		if(unused.size() > 0){
			if(!unused.get(0).isUsed()){
				logger.error("Unused not used! {}\n{}",unused.get(0).toString(),printUnused());
				System.exit(1);
			}
			if(logger.isDebugEnabled())
				logger.debug("Remoing {}",unused.get(0).toString());
			unused.remove(0);
		}else{
			logger.debug("Not removing, empty unused index.");
		}
		if(logger.isDebugEnabled()){
			logger.debug("{} {}",this.player,printHistory());
		}
	}
	
	private String printHistory(){
		StringBuilder sb = new StringBuilder();
		sb.append(moveHistory.size());
		sb.append("\nHistory:\n");
		for(Move move : moveHistory){
			sb.append(move.toString());
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
	@Override
	public void gameEvent(boolean rollback) {
		logger.entry(player);
		if(SHUTDOWN){
			logger.debug("Shutdown set");
			return;
		}
		if(follow_unused)
			print_follow();
		
		if(P_MOVE_CURRENT == null){
			logger.error("No current move on win state! \n{}",GController.getprintedGameState());
			logger.warn("Null move");
			return;
		}
		
		switch(GController.getGameState()){
		case DRAW:
			this.P_MOVE_CURRENT.setDraw(true);
		case WIN_A:
		case WIN_B:
			if(checkLoose(GController.getGameState()))
				this.P_MOVE_CURRENT.setLoose(true);
			this.P_MOVE_CURRENT.setUsed(true);
			if(!db.setMove(P_MOVE_CURRENT)){
				logger.error("Invalid set");
				System.exit(1);
			}
			break;
		default:
			logger.error("Unknown case for gameEvent!");
			break;
		}
		P_MOVE_CURRENT = null;
		if(logger.isDebugEnabled())
			logger.debug("{} {}",this.player,printUnused());
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
		P_MOVE_CURRENT = null;
		moveHistory = new ArrayList<Move>( (GController.getX_MAX() * GController.getY_MAX()) /2 );
		this.player = player;
	}
	
	private boolean checkLoose(E_GAME_STATE state){
		if(this.player == E_PLAYER.PLAYER_A && state == E_GAME_STATE.WIN_B){
			return true;
		}else if(this.player == E_PLAYER.PLAYER_B && state == E_GAME_STATE.WIN_A){
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
