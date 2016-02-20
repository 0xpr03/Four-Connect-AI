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

import renderEngine.DisplayManager;
import terrain.Terrain;

/**
 *
 * @author Trist
 */
public class Camera {
    
	public static float RUN_SPEED_FORWARD;
	public static float RUN_SPEED_STRAFE;
    private static float UP_DOWN_SPEED;
    	
	private Vector3f position;
    private Logger logger = LogManager.getLogger();
    private float pitch;
    private float rotY = 90;
                  
    public Camera(Vector3f position) {
    	this.position = position;
    }

    public float getRotY() {
		return rotY;
	}

	public void move(Terrain terrain) {
        checkInputs();        
        
        float dx = (RUN_SPEED_FORWARD * (float) Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        float dz = (-RUN_SPEED_FORWARD * (float) Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dx += (RUN_SPEED_STRAFE * (float)Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dz += (RUN_SPEED_STRAFE * (float)Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        position.x += dx;
        position.z += dz;    
        logger.debug("posX {}",position.x);
        logger.debug("posZ {}",position.z);
        position.y += UP_DOWN_SPEED * DisplayManager.getFrameTimeSeconds();
        increasePosition(0, UP_DOWN_SPEED * DisplayManager.getFrameTimeSeconds(), 0);
        float terrainHeight = terrain.getHeightOfTerrain(position.x, position.z);
        
        if(position.y < terrainHeight + 5) {
        	UP_DOWN_SPEED = 0;
            position.y = terrainHeight + 5;
        }
    }
	
	public void move() {
		float dx = (RUN_SPEED_FORWARD * (float) Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        float dz = (-RUN_SPEED_FORWARD * (float) Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dx += (RUN_SPEED_STRAFE * (float)Math.cos(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        dz += (RUN_SPEED_STRAFE * (float)Math.sin(Math.toRadians(rotY))) * DisplayManager.getFrameTimeSeconds();
        position.x += dx;
        position.z += dz;   
        logger.debug("posX {}",position.x);
        logger.debug("posZ {}",position.z);
	}
	
	public void setPosition(Vector3f position) {
		this.position = position;
	}
	
	public void setRotY(float rotY) {
		this.rotY = rotY;
	}
    
    public Vector3f getPosition() {
        return position;
    }

    public float getPitch() {
        return pitch;
    }
          
    private void checkInputs() {
        if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
        	if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
        		RUN_SPEED_FORWARD = 200;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
        		RUN_SPEED_FORWARD = -200;
        	}else {
        		RUN_SPEED_FORWARD = 0;
        	}        
        	if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
        		RUN_SPEED_STRAFE = 200;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
        		RUN_SPEED_STRAFE = -200;
        	}else {
        		RUN_SPEED_STRAFE = 0;
        	}                
        }else{
        	if(Keyboard.isKeyDown(Keyboard.KEY_W)) {
        		RUN_SPEED_FORWARD = 20;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_S)) {
        		RUN_SPEED_FORWARD = -20;
        	}else {
        		RUN_SPEED_FORWARD = 0;
        	}        
        	if(Keyboard.isKeyDown(Keyboard.KEY_D)) {
        		RUN_SPEED_STRAFE = 20;
        	}else if(Keyboard.isKeyDown(Keyboard.KEY_A)) {
        		RUN_SPEED_STRAFE = -20;
        	}else {
        		RUN_SPEED_STRAFE = 0;
        	}        
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
        	UP_DOWN_SPEED = 30;
        }else if(Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
        	UP_DOWN_SPEED = -30;
        }else {
        	UP_DOWN_SPEED = 0;
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
}

