package gamelogic.AI;

import java.util.List;

import gamelogic.Controller.E_FIELD_STATE;

public interface DB {
	public abstract List<Move> getMoves(E_FIELD_STATE[][] field);
	public abstract void insertMoves(E_FIELD_STATE field, List<Integer> moves);
	public abstract void setMove(Move move);
	public abstract void shutdown();
}
