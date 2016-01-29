/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package entities;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.util.vector.Vector3f;

/**
 *
 * @author Trist
 */
public class Camera {
    
    private float distanceFromEntity = 30;
    private float angleAroundEntity = 0;
    
    private Vector3f position = new Vector3f(100, 15, 0);
    private float pitch = 15f;
    private float yaw;
    private float roll;
    
    private Entity entity;
        
    public Camera(Entity entity) {
        this.entity = entity;
    }

    public void move() {
        calculateZoom();
        calculatePitch();
        calculateAngleAroundEntity();
        float horizontalDistance = calculateHorizontalDistance();
        float verticalDistance = calculateVerticalDistance();
        calculateCameraPosition(horizontalDistance, verticalDistance);
        this.yaw = 180 - (entity.getRotY() + angleAroundEntity);
    }
    
    public Vector3f getPosition() {
        return position;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getRoll() {
        return roll;
    }
    
    private void calculateCameraPosition(float horizDistance, float verticDistance) {
        float theta = entity.getRotY() + angleAroundEntity;
        float offsetX = (float) (horizDistance * Math.sin(Math.toRadians(theta)));
        float offsetZ = (float) (horizDistance * Math.cos(Math.toRadians(theta)));
        position.x = entity.getPosition().x - offsetX;
        position.z = entity.getPosition().z - offsetZ;
        position.y = entity.getPosition().y + verticDistance;
    }
    
    private float calculateHorizontalDistance() {
        return (float) (distanceFromEntity * Math.cos(Math.toRadians(pitch)));
    }
    
    private float calculateVerticalDistance() {
        return (float) (distanceFromEntity * Math.sin(Math.toRadians(pitch)));
    }
    
    private void calculateZoom() {
        float zoomLevel = Mouse.getDWheel() * 0.1f;
        distanceFromEntity -= zoomLevel;
    }

    private void calculatePitch() {
        if(Mouse.isButtonDown(1)) {
            float pitchChange = Mouse.getDY() * 0.1f;
            pitch -= pitchChange;
        }
    }
    
    private void calculateAngleAroundEntity() {
        if(Mouse.isButtonDown(0)) {
            float angleChange = Mouse.getDX() * 0.3f;
            angleAroundEntity -= angleChange;
        }
    }
}

