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
	 * @param player_a
	 * @return
	 */
	public abstract SelectResult getMoves(E_FIELD_STATE[][] field,boolean player_a);
	/**
	 * Insert moves
	 * @param field
	 * @param moves
	 * @return returns SelectResult with all moves or null on failure
	 */
	public abstract SelectResult insertMoves(E_FIELD_STATE[][] field, List<Integer> moves,boolean player_a);
	/**
	 * Set/Update move
	 * @param move
	 * @return false on error
	 */
	public abstract boolean setMove(Move move);
	/**
	 * Delete all loose moves of child
	 * @param field id
	 * @param player_a
	 * @return false on error
	 */
	public abstract boolean deleteLooses(long fid, boolean player_a);
	/**
	 * Delete all moves for this field
	 * @param field id
	 * @param player_a
	 * @return false on error
	 */
	public abstract boolean deleteMoves(long fid, boolean player_a);
	/**
	 * Shutdown
	 */
	public abstract void shutdown();
	
	public abstract byte[] getHash();
}
