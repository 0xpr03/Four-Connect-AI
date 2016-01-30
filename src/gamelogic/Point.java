package gamelogic;

/**
 * Point class to store game field points
 * @author Aron Heinecke
 *
 */
public class Point {
	private int x;
	private int y;
	
	/**
	 * Create a new point
	 * @param x
	 * @param y
	 */
	public Point(int x, int y){
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
}