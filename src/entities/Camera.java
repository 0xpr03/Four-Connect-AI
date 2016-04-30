/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

import main.MainGameLoop;
import renderEngine.DisplayManager;
import terrain.Terrain;

/**
 * @author Trist
 */
public class Camera {
    
	private float FORWARD_SPEED; //  W/S keys
	private float SIDEWARD_SPEED; // A/D keys
    private float HEIGHT_SPEED;//    shift/space keys

    private Vector3f position;
    private Logger logger = LogManager.getLogger();
    private float pitch;
    private float rotY;
                  
    public Camera(Vector3f position, float rotY, float pitch) {
    	this.position = new Vector3f(position);
    	this.rotY = rotY;
    	this.pitch = pitch;
    }

    public float getRotY() {
		return rotY;
	}
    
	public void move(Terrain terrain) {
        if(!MainGameLoop.getStaticCamera())
		checkInputs();        
        
        float dx = (FORWARD_SPEED * (float) Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        float dz = (-FORWARD_SPEED * (float) Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dx += (SIDEWARD_SPEED * (float)Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dz += (SIDEWARD_SPEED * (float)Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        position.x += dx;
        position.z += dz;
//        logger.debug("S:{}|F:{}",COORDS_SIDEWARD, COORDS_FORWARD);
        position.y += HEIGHT_SPEED * DisplayManager.getFrameTimeSeconds();
        increasePosition(0, HEIGHT_SPEED * DisplayManager.getFrameTimeSeconds(), 0);
        float terrainHeight = terrain.getHeightOfTerrain(position.x, position.z);
        
        if(position.y < terrainHeight + 5) {
        	HEIGHT_SPEED = 0;
            position.y = terrainHeight + 5;
        }
    }
	
	/**
	 * Set forward & sideward movement to 0
	 */
	public void resetMovement(){
		FORWARD_SPEED = 0;
		SIDEWARD_SPEED = 0;
		HEIGHT_SPEED = 0;
	}
	
	/**
	 * Run camera move
	 */
	public void move() {
		float dx = (FORWARD_SPEED * (float) Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        float dz = (-FORWARD_SPEED * (float) Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dx += (SIDEWARD_SPEED * (float)Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dz += (SIDEWARD_SPEED * (float)Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        position.x += dx;
        position.z += dz;
//        logger.debug("posX {} posZ {}",position.x,position.z);
	}
	
	public void setPosition(Vector3f position) {
		this.position = position;
	}
	
	public void setRotY(float rotY) {
		this.rotY = rotY;
	}
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public float getPitch() {
        return pitch;
    }
          
    private void checkInputs() {
        if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
        	if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
        		FORWARD_SPEED = 200;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
        		FORWARD_SPEED = -200;
        	}else {
        		FORWARD_SPEED = 0;
        	}        
        	if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
        		SIDEWARD_SPEED = 200;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
        		SIDEWARD_SPEED = -200;
        	}else {
        		SIDEWARD_SPEED = 0;
        	}                
        }else{
        	if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
        		FORWARD_SPEED = 20;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
        		FORWARD_SPEED = -20;
        	}else {
        		FORWARD_SPEED = 0;
        	}        
        	if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
        		SIDEWARD_SPEED = 20;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
        		SIDEWARD_SPEED = -20;
        	}else {
        		SIDEWARD_SPEED = 0;
        	}        
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
        	HEIGHT_SPEED = 30;
        }else if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
        	HEIGHT_SPEED = -30;
        }else {
        	HEIGHT_SPEED = 0;
        }
        if(Mouse.isButtonDown(1)) {
            float pitchChange = Mouse.getDY() * 0.1f;
            pitch += pitchChange;
            if(pitch <= -90) {
            	pitch = -90;
            }else if(pitch >= 90) {
            	pitch = 90;
            }
            float dy = Mouse.getDX() * 0.1f;
            rotY -= dy;
        }       
    }  
    
    public void increasePosition(float dx, float dy, float dz) {
        this.position.x += dx;
        this.position.y += dy;
        this.position.z += dz;
    }
    
    public void increaseRotation(float dy, float pitch) {
        this.rotY += dy;
        this.pitch += pitch;
    }
    
    public void increaseSideSpeed(float s) {
    	SIDEWARD_SPEED += s;
    }
}

