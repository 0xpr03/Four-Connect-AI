package gamelogic.AI;

import java.util.List;

import gamelogic.Controller.E_FIELD_STATE;

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
	public abstract List<Move> getMoves(E_FIELD_STATE[][] field);
	/**
	 * Insert moves
	 * @param field
	 * @param moves
	 * @return returns first move
	 */
	public abstract Move insertMoves(E_FIELD_STATE[][] field, List<Integer> moves);
	/**
	 * Set/Update move
	 * @param move
	 */
	public abstract boolean setMove(Move move);
//	public abstract void getLink(byte[] parent);
	/**
	 * Delete all loose moves of child
	 * @param parentHash
	 * @param childHash
	 */
	public abstract boolean deleteLooses(byte[] childHash);
	/**
	 * Delete all moves for this field
	 * @param field
	 */
	public abstract boolean deleteMoves(byte[] fieldHash);
	/**
	 * Shutdown
	 */
	public abstract void shutdown();
}
