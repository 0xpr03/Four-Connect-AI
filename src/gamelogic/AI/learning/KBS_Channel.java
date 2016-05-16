package gamelogic.AI.learning;

/**
 * Channel for the KBS trainer to communicate with the other side
 * @author Aron Heinecke
 */
public class KBS_Channel {
	private boolean WIN_A = false, WIN_B = false, DRAW = false;

	/**
	 * @return the wIN_A
	 */
	public synchronized boolean isWIN_A() {
		return WIN_A;
	}

	/**
	 * @param wIN_A the wIN_A to set
	 */
	public synchronized void setWIN_A(boolean wIN_A) {
		WIN_A = wIN_A;
	}

	/**
	 * @return the wIN_B
	 */
	public synchronized boolean isWIN_B() {
		return WIN_B;
	}

	/**
	 * @param wIN_B the wIN_B to set
	 */
	public synchronized void setWIN_B(boolean wIN_B) {
		WIN_B = wIN_B;
	}

	/**
	 * @return the dRAW
	 */
	public synchronized boolean isDRAW() {
		return DRAW;
	}

	/**
	 * @param dRAW the dRAW to set
	 */
	public synchronized void setDRAW(boolean dRAW) {
		DRAW = dRAW;
	}
	
}
