package gamelogic;

import gamelogic.ControllerBase.E_GAME_STATE;

/**
 * Class to store a vector based on two points where the win happened
 * @author Aron Heinecke
 *
 */
public class WinStore {
	private Point point_a;
	private Point point_b;
	private boolean capitulation;
	/**
	 * @return the capitulation
	 */
	public boolean isCapitulation() {
		return capitulation;
	}

	private E_GAME_STATE state;
	
	/**
	 * Creates a new win storage
	 * @param pa
	 * @param pb
	 */
	public WinStore(Point pa, Point pb, E_GAME_STATE state){
		this.point_a = pa;
		this.point_b = pb;
		this.capitulation = false;
		this.state = state == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.WIN_A : E_GAME_STATE.WIN_B;
	}
	
	/**
	 * Create a new CAPITULATION win storage
	 * @param state
	 */
	public WinStore(E_GAME_STATE state){
		this.capitulation = true;
		this.state = state == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.WIN_B : E_GAME_STATE.WIN_A;
	}
	
	public Point getPoint_a() {
		return point_a;
	}

	public Point getPoint_b() {
		return point_b;
	}

	public E_GAME_STATE getState() {
		return state;
	}
}
