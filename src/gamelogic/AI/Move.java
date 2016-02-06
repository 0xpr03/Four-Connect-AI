package gamelogic.AI;

/**
 * Class representing a single move for a specified field
 * @author Aron Heinecke
 *
 */
public class Move {
	private int move;
	private boolean draw;
	private boolean loose;
	private boolean used;
	private byte[] field;
	public Move(byte[] field, int move, boolean draw, boolean loose, boolean used) {
		this.field = field;
		this.move = move;
		this.draw = draw;
		this.loose = loose;
		this.used = used;
	}
	public Move(byte[] field, int move) {
		this(field, move, false, false, false);
	}
	
	/**
	 * @return the draw
	 */
	public boolean isDraw() {
		return draw;
	}
	/**
	 * @param draw the draw to set
	 */
	public void setDraw(boolean draw) {
		this.draw = draw;
	}
	/**
	 * @return the used
	 */
	public boolean isUsed() {
		return used;
	}
	/**
	 * @param used the used to set
	 */
	public void setUsed(boolean used) {
		this.used = used;
	}	
	/**
	 * @return the loose
	 */
	public boolean isLoose() {
		return loose;
	}
	/**
	 * @param loose the loose to set
	 */
	public void setLoose(boolean loose) {
		this.loose = loose;
	}
	/**
	 * @return the move
	 */
	public int getMove() {
		return move;
	}
	/**
	 * @return the field
	 */
	public byte[] getField() {
		return field;
	}
}
