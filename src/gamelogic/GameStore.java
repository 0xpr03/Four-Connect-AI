package gamelogic;

import gamelogic.ControllerBase.E_GAME_STATE;

/**
 * Class to store the last game ending state
 * On win a vector based on two points where the win happened
 * @author Aron Heinecke
 *
 */
public class GameStore {
	private Point point_a = null;
	private Point point_b = null;
	private boolean capitulation;
	private boolean draw;
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
	public GameStore(Point pa, Point pb, E_GAME_STATE state){
		this.point_a = pa;
		this.point_b = pb;
		this.capitulation = false;
		this.draw = false;
		this.state = state == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.WIN_A : E_GAME_STATE.WIN_B;
	}
	
	/**
	 * Create a new CAPITULATION win storage
	 * @param state
	 */
	public GameStore(E_GAME_STATE state){
		this.capitulation = true;
		this.draw = false;
		this.state = state == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.WIN_B : E_GAME_STATE.WIN_A;
	}
	
	/**
	 * Create a DRAW gamestore
	 */
	public GameStore(){
		this.draw = true;
		this.capitulation = false;
		this.state = E_GAME_STATE.DRAW;
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

	public synchronized boolean isDraw() {
		return draw;
	}
}
