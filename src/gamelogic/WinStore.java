package gamelogic;

import org.apache.logging.log4j.LogManager;

import gamelogic.Controller.E_GAME_STATE;

/**
 * Class to store a vector based on two points where the win happened
 * @author Aron Heinecke
 *
 */
public class WinStore {
	private Point point_a;
	private Point point_b;
	private E_GAME_STATE state;
	
	/**
	 * Creates a new win storage
	 * @param pa
	 * @param pb
	 */
	public WinStore(Point pa, Point pb, E_GAME_STATE state){
		this.point_a = pa;
		this.point_b = pb;
		this.state = state == E_GAME_STATE.PLAYER_A ? E_GAME_STATE.WIN_A : E_GAME_STATE.WIN_B;
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
