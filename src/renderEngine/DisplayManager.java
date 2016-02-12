/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renderEngine;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;
import org.lwjgl.util.vector.Vector2f;

/**
 *
 * @author Trist
 */
public class DisplayManager {
    
    private static final int WIDTH = 960;
    private static final int HEIGHT = 540;
    private static final int SCALE = 1;
    private static final int FPS_CAP = 120;
    
    private static long lastFrameTime;
    private static float delta;
    
    public static void createDisplay() {
        
        ContextAttribs attribs = new ContextAttribs(3,2)
        .withForwardCompatible(true)
        .withProfileCore(true);
            
        try {
            Display.setDisplayMode(new DisplayMode(WIDTH*SCALE, HEIGHT*SCALE));
            Display.create(new PixelFormat(), attribs);
            Display.setTitle("Game1ATrial");
        } catch (LWJGLException e) {
            e.printStackTrace();    
        }
        
        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        lastFrameTime = getCurrentTime();
    }
    
    public static void updateDisplay() {
    
        Display.sync(FPS_CAP);
        Display.update();
        long currentFrameTime = getCurrentTime();
        delta = (currentFrameTime - lastFrameTime) / 1000f;        
        lastFrameTime = currentFrameTime;
    }
    
    public static float getFrameTimeSeconds() {
        return delta;
    }
    
    public static void closeDisplay() {
        Display.destroy();
    }
    
    private static long getCurrentTime() {
        return Sys.getTime() * 1000 / Sys.getTimerResolution();
    }
       
}
