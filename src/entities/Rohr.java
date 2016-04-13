package entities;

import org.lwjgl.util.vector.Vector3f;

import models.TexturedModel;

public class Rohr extends Entity{

	private int balls = 0;
	
	public Rohr(TexturedModel model, Vector3f position, float rotX, float rotY, float rotZ, float scale) {
		super(model, position, rotX, rotY, rotZ, scale);
	}
	
	public int getBalls() {
		return balls;
	}

	public void setBalls(int balls) {
		this.balls = balls;
	}	

}
