package gamelogic.AI;

import java.util.List;

import gamelogic.ControllerBase.E_FIELD_STATE;

/**
 * DB Interface for AIs
 * @author Aron Heinecke
 *
 */
public interface DB {
	/**
	 * Get all moves for field
	 * @param field
	 * @return
	 */
	public abstract SelectResult getMoves(E_FIELD_STATE[][] field,boolean player_a);
	/**
	 * Insert moves
	 * @param field
	 * @param moves
	 * @return returns first move
	 */
	public abstract SelectResult insertMoves(E_FIELD_STATE[][] field, List<Integer> moves,boolean player_a);
	/**
	 * Set/Update move
	 * @param move
	 * @return false on error
	 */
	public abstract boolean setMove(Move move);
//	public abstract void getLink(byte[] parent);
	/**
	 * Delete all loose moves of child
	 * @param parentHash
	 * @param childHash
	 * @return false on error
	 */
	public abstract boolean deleteLooses(byte[] childHash);
	/**
	 * Delete all moves for this field
	 * @param field
	 * @return false on error
	 */
	public abstract boolean deleteMoves(byte[] fieldHash);
	/**
	 * Shutdown
	 */
	public abstract void shutdown();
	
	
	public abstract boolean insertRelation(Move parent, E_FIELD_STATE[][] child);
	public abstract boolean insertRelation(Move parent, byte[] child);
	public abstract byte[] getHash();
}
