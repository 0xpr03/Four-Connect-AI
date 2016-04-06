package gamelogic.AI.learning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.ControllerBase.E_PLAYER;
import gamelogic.GController;
import gamelogic.AI.AI;
import gamelogic.AI.DB;
import gamelogic.AI.Move;
import gamelogic.AI.SelectResult;

/**
 * Knowledge based AI system
 * Training version
 * This AI Version is supposed to run against itself and learn by this all possibilities & outcomes
 * @author Aron Heinecke
 * 
 * @param <E> needs a DB handler
 */
public class KBS_trainer implements AI {
	
	private Logger logger = LogManager.getLogger("AI");
	private DB db;
	private E_PLAYER player;
	private SelectResult MOVES;
	private byte[] lastField = null;
	private Move P_MOVE_CURRENT; // pointer to last moveHistory element
	private List<Move> unused;
	private List<Move> moveHistory;
	private boolean SHUTDOWN = false;
	
	private final KBS_Channel KIChannel;
	
	private boolean follow_unused = false;
	private List<Move> follow = new ArrayList<Move>();
	
	private final boolean USE_FIRST_MOVE;
	private final int FIRST_MOVE;
	private final List<Integer> DISALLOWED_MOVES;
	private boolean first_move_done = false;
	private int retries = 0;
	
	public KBS_trainer(DB db, int first_move, List<Integer> disallowedMoves, KBS_Channel channel){
		this.DISALLOWED_MOVES = disallowedMoves;
		this.USE_FIRST_MOVE = first_move == -1 ? false : true;
		this.FIRST_MOVE = first_move;
		this.db = db;
		this.KIChannel = channel;
		logger.info("Knowledge based trainer system initializing..\nusing first move:{} {}",USE_FIRST_MOVE, first_move);
	}
	
	@Override
	public boolean getMove() {
		return getMove(true);
	}
	
	public boolean getMove(boolean allowrecusion) {
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
					logger.debug("Error on insert");
					if(allowrecusion){
						try {
							Thread.sleep(5);
						} catch (InterruptedException e) {
							logger.error("{}",e);
						}
						retries++;
						return getMove(false);
					}else{
						logger.error("Avoided deadlock!");
						GController.restart();
						return true;
					}
				}

				lastField = db.getHash();
				MOVES = null; // needed ?
				MOVES = sel;
				unused = MOVES.getUnused();
				if(GController.getMoves() == 0){
					if(USE_FIRST_MOVE) {
						if(first_move_done){
							logger.info("Done with branch!");
							return false;
						}
						new_move = unused.get(FIRST_MOVE);
						first_move_done = true;
					}else{
						new_move = unused.get(0);
					}
				}else{
					new_move = unused.get(0);
				}
			}else{
				MOVES = null;
				MOVES = moves;
				if(!moves.getUnused().isEmpty()){
					lastField = db.getHash();
					unused = MOVES.getUnused();
					
					if(GController.getMoves() == 0){
						if(USE_FIRST_MOVE){ // use specified move
							if(first_move_done){
								logger.info("Done with branch!");
								return false;
							}
							Move chosen_move = null;
							for(Move move_int : unused){
								if(move_int.getMove() == FIRST_MOVE){
									chosen_move = move_int;
								}
							}
							if( chosen_move != null ){
								new_move = chosen_move;
							}else{
								logger.error("specified first move not found ! {}",FIRST_MOVE);
								return false;
							}
							first_move_done = true;
						}else if(!DISALLOWED_MOVES.isEmpty()){ // use non forbidden listed move
							new_move = null;
							for(int i = 0; i < unused.size(); i++){
								if(!DISALLOWED_MOVES.contains(unused.get(i).getMove())){
									new_move = unused.get(i);
									break;
								}
							}
							if(new_move == null){
								logger.info("Done with branch!");
								return false;
							}
						}else{ // or use first move
							new_move = unused.get(0);
						}
					}else{
						new_move = unused.get(0);
					}
				}
			}
		}
		if(logger.isDebugEnabled())
			logger.debug("MOVES pla:{} win:{} loose:{} draw:{} unused:{}",this.player,MOVES.getWins().size(),MOVES.getLooses().size(),MOVES.getDraws().size(),MOVES.getUnused().size());
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
		sb.append("Unused list:");
		for(Move move : unused) {
			sb.append("\n");
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
				logger.debug("Removing {}",unused.get(0).toString());
			unused.remove(0);
		}else{
			logger.debug("Not removing, empty unused index.");
		}
		if(logger.isDebugEnabled()){
			logger.debug("MOVES pla:{} win:{} loose:{} draw:{} unused:{}",this.player,MOVES.getWins().size(),MOVES.getLooses().size(),MOVES.getDraws().size(),MOVES.getUnused().size());
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
		logger.entry(player, rollback);
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
		
		if(logger.isDebugEnabled()){
			logger.debug("rollback: {} state:{}",rollback,GController.getGameState());
			logger.debug("Before event: {}",P_MOVE_CURRENT.toString());
		}
		/**
		 * TODO: - leaving unused fields ?! => error on internal retry ?
		 * TODO: - 5*4 field: move 1/3 are seen as direct win
		 */
		if(rollback){ // no real move happened, we're going back 1 move
			if(KIChannel.isWIN_A() && this.player == E_PLAYER.PLAYER_A || KIChannel.isWIN_B() && this.player == E_PLAYER.PLAYER_B){
				this.P_MOVE_CURRENT.setWin(true);
			}else if(KIChannel.isDRAW()){
				this.P_MOVE_CURRENT.setDraw(true);
			}else if(KIChannel.isWIN_A() || KIChannel.isWIN_B()){
				this.P_MOVE_CURRENT.setLoose(true);
			}else{
				logger.fatal("No win/loose data for rollback evaluation\n{}",this.P_MOVE_CURRENT.toString());
				System.exit(1);
			}
		}else{ // real move
			switch(GController.getGameState()){
			case DRAW:
				this.P_MOVE_CURRENT.setDraw(true);
				break;
			case WIN_A:
			case WIN_B:
				if(checkLoose(GController.getGameState())){
					this.P_MOVE_CURRENT.setLoose(true);
				}else{
					this.P_MOVE_CURRENT.setWin(true);
				}
				break;
			default:
				logger.error("Unknown case for gameEvent! {}",GController.getGameState());
				return;
			}
		}
		this.P_MOVE_CURRENT.setUsed(true);
		if(!db.setMove(P_MOVE_CURRENT)){
			logger.error("Invalid set");
			GController.restart();
			return;
		}
		
		logger.debug("After event: {}",P_MOVE_CURRENT.toString());
		if(P_MOVE_CURRENT.isLoose()){
			MOVES.addLoose(P_MOVE_CURRENT);
		}else if(P_MOVE_CURRENT.isWin()){
			MOVES.addWin(P_MOVE_CURRENT);
		}
		if(P_MOVE_CURRENT.isDraw()){
			MOVES.addDraw(P_MOVE_CURRENT);
		}
		if(logger.isDebugEnabled()){
			logger.debug("MOVES pla:{} win:{} loose:{} draw:{} unused:{}",this.player,MOVES.getWins().size(),MOVES.getLooses().size(),MOVES.getDraws().size(),MOVES.getUnused().size());
			logger.debug("{} {}",this.player,printUnused());
		}
		P_MOVE_CURRENT = null;
		logger.exit();
	}
	
	/**
	 * Sets the outcome of all possible moves for the current field
	 */
	public void getOutcome(){
		logger.entry();
		boolean player_a = this.player == E_PLAYER.PLAYER_A;
		if(!MOVES.getWins().isEmpty()){
			KIChannel.setWIN_A(player_a);
			KIChannel.setWIN_B(!player_a);
			KIChannel.setDRAW(false);
		}else if(!MOVES.getDraws().isEmpty()){
			KIChannel.setWIN_A(false);
			KIChannel.setWIN_B(false);
			KIChannel.setDRAW(true);
		}else if(!MOVES.getLooses().isEmpty()){
			KIChannel.setWIN_A(!player_a);
			KIChannel.setWIN_B(player_a);
			KIChannel.setDRAW(false);
		}
		if(logger.isDebugEnabled()){
			logger.debug("pla:{} WIN_A:{} WIN_B:{} DRAW:{}",this.player,KIChannel.isWIN_A(),KIChannel.isWIN_B(),KIChannel.isDRAW());
		}
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
		logger.info("Retries for {}: {}", player, retries);
		this.SHUTDOWN = true;
		db.shutdown();
	}

	@Override
	public void start(E_PLAYER player) {
		logger.entry(player);
		P_MOVE_CURRENT = null;
		MOVES = null;
		first_move_done = false;
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
	@Deprecated
	public void preProcessing() {
		// TODO Auto-generated method stub
		
	}
	
}
