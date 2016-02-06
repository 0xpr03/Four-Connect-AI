package gamelogic.AI;

import java.util.List;

import gamelogic.Controller.E_FIELD_STATE;

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
	public abstract void setMove(Move move);
	/**
	 * Shutdown
	 */
	public abstract void shutdown();
}
