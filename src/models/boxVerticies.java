package models;

import org.lwjgl.util.vector.Vector3f;

/**
 * Stores the minimal and maximal vertex used to create a physics box.
 * @author Aron Heinecke
 *
 */
public class boxVerticies {
	private Vector3f min;
	private Vector3f max;
	/**
	 * Create a new object storing the minimal and maximal vectors of an object.
	 * This is used for physic box creation.
	 * @param min
	 * @param max
	 */
	public boxVerticies(Vector3f min, Vector3f max) {
		this.max = max;
		this.min = min;
	}
	
	public Vector3f getMax(){
		return this.max;
	}
	public Vector3f getMina(){
		return this.min;
	}
}
