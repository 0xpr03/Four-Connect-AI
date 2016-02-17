/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renderEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

/**
 *
 * @author Trist
 */
public class DisplayManager {
    
    private static final int FPS_CAP = 60;
    /***
     * adm list of all available Display Modes (Resolution and frequency etc.
     * dms list of Display Modes used by us
     * dmi public index for switching modes
     */
    private static DisplayMode[] adm;
    private static DisplayMode[] dms;
    private static DisplayMode dm_640x480;		//4:3
	private static DisplayMode dm_1280x960;		//4:3
	private static DisplayMode dm_1280x768;		//5:3
	private static DisplayMode dm_1440x900;		//16:10
	private static DisplayMode dm_1280x720;		//16:9
	private static DisplayMode dm_1366x768;		//16:9
	private static DisplayMode dm_1600x900;		//16:9
	private static DisplayMode dm_1920x1080;	//16:9
	public static boolean fullscreen = false;
	public static int dmi = 1;
    
    private static long lastFrameTime;
    private static float delta;
    
    public static void createDisplay() {
        
        ContextAttribs attribs = new ContextAttribs(3,2)
        .withForwardCompatible(true)
        .withProfileCore(true);
            
        try {
        	adm = Display.getAvailableDisplayModes();
    /***
     * Resolution list at the bottom
     */
        	dm_640x480 = adm[68]; 
        	dm_1280x960 = adm[39];
        	dm_1280x768 = adm[6];
        	dm_1440x900 = adm[11];
        	dm_1280x720 = adm[83];
        	dm_1366x768 = adm[15];
        	dm_1600x900 = adm[86];
        	dm_1920x1080 = adm[76];      	
        	
        	dms = new DisplayMode[8];
        	dms[0] = dm_640x480;
        	dms[1] = dm_1280x768;
        	dms[2] = dm_1280x960;
        	dms[3] = dm_1440x900;
        	dms[4] = dm_1280x720;
        	dms[5] = dm_1366x768;
        	dms[6] = dm_1600x900;
        	dms[7] = dm_1920x1080;
            Display.setDisplayMode(dms[dmi]);
            Display.create(new PixelFormat(), attribs);
            Display.setTitle("Game1ATrial");
            Display.setFullscreen(fullscreen);            
        } catch (LWJGLException e) {
            e.printStackTrace();    
        }
        
        GL11.glViewport(0, 0, dms[dmi].getWidth(), dms[dmi].getHeight());
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
       
    /***
     * 	1600 x 1024 x 32 @100Hz
		1360 x 768 x 32 @120Hz
		1366 x 768 x 32 @120Hz
		1366 x 768 x 32 @100Hz
		1360 x 768 x 32 @100Hz
		1600 x 1024 x 32 @120Hz
		1280 x 768 x 32 @60Hz
		720 x 576 x 32 @144Hz
		1366 x 768 x 32 @85Hz
		1360 x 768 x 32 @85Hz
		1600 x 1024 x 32 @85Hz
		1440 x 900 x 32 @60Hz
		1680 x 1050 x 32 @144Hz
		1280 x 800 x 32 @60Hz
		1280 x 768 x 32 @100Hz
		1366 x 768 x 32 @60Hz
		1360 x 768 x 32 @60Hz
		1440 x 900 x 32 @85Hz
		1280 x 800 x 32 @85Hz
		1280 x 768 x 32 @120Hz
		1600 x 1024 x 32 @60Hz
		1440 x 900 x 32 @100Hz
		1280 x 800 x 32 @100Hz
		1280 x 960 x 32 @144Hz
		1280 x 768 x 32 @85Hz
		1280 x 800 x 32 @120Hz
		1440 x 900 x 32 @120Hz
		1280 x 960 x 32 @100Hz
		720 x 576 x 32 @60Hz
		1280 x 800 x 32 @144Hz
		1440 x 900 x 32 @144Hz
		1680 x 1050 x 32 @60Hz
		1280 x 960 x 32 @120Hz
		1280 x 768 x 32 @144Hz
		1280 x 960 x 32 @85Hz
		720 x 576 x 32 @120Hz
		1680 x 1050 x 32 @100Hz
		1680 x 1050 x 32 @120Hz
		720 x 576 x 32 @100Hz
		1280 x 960 x 32 @60Hz
		1360 x 768 x 32 @144Hz
		720 x 576 x 32 @85Hz
		1366 x 768 x 32 @144Hz
		1600 x 1024 x 32 @144Hz
		1680 x 1050 x 32 @85Hz
		1920 x 1080 x 32 @144Hz
		800 x 600 x 32 @85Hz
		640 x 480 x 32 @85Hz
		1024 x 768 x 32 @60Hz
		1280 x 1024 x 32 @60Hz
		800 x 600 x 32 @120Hz
		640 x 480 x 32 @100Hz
		720 x 480 x 32 @60Hz
		640 x 480 x 32 @120Hz
		800 x 600 x 32 @100Hz
		1024 x 768 x 32 @100Hz
		1280 x 1024 x 32 @100Hz
		720 x 480 x 32 @85Hz
		1152 x 864 x 32 @144Hz
		1024 x 768 x 32 @120Hz
		1280 x 1024 x 32 @120Hz
		1280 x 720 x 32 @144Hz
		800 x 600 x 32 @60Hz
		720 x 480 x 32 @120Hz
		720 x 480 x 32 @100Hz
		1600 x 900 x 32 @144Hz
		1024 x 768 x 32 @85Hz
		1280 x 1024 x 32 @85Hz
		640 x 480 x 32 @60Hz
		720 x 480 x 32 @144Hz
		1600 x 900 x 32 @100Hz
		1280 x 720 x 32 @120Hz
		1280 x 720 x 32 @100Hz
		1152 x 864 x 32 @85Hz
		1600 x 900 x 32 @120Hz
		1152 x 864 x 32 @100Hz
		1920 x 1080 x 32 @60Hz
		1280 x 720 x 32 @85Hz
		1024 x 768 x 32 @144Hz
		1280 x 1024 x 32 @144Hz
		1600 x 900 x 32 @85Hz
		1152 x 864 x 32 @120Hz
		800 x 600 x 32 @144Hz
		1280 x 720 x 32 @60Hz
		1920 x 1080 x 32 @85Hz
		640 x 480 x 32 @144Hz
		1600 x 900 x 32 @60Hz
		1920 x 1080 x 32 @120Hz
		1152 x 864 x 32 @60Hz
		1920 x 1080 x 32 @100Hz
      */
}
