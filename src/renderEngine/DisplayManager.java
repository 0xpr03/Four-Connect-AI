/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renderEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.opengl.ContextAttribs;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.PixelFormat;

import main.MainGameLoop;

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
    private static List<DisplayMode> dms;
    private static Logger logger = LogManager.getLogger(MainGameLoop.class);
	public static boolean fullscreen = false;
	public static int dmi = 0;
    
    private static long lastFrameTime;
    private static float delta;
    
    public static void createDisplay(boolean useAA) {
        
        ContextAttribs attribs = new ContextAttribs(3,2)
        .withForwardCompatible(false)
        .withProfileCore(true);
            
        try {
        	adm = Display.getAvailableDisplayModes();
        	HashMap<String, DisplayMode> modes = new HashMap<>();
        	for(DisplayMode dm : adm) {
        		boolean freq = dm.getFrequency() == 60;
        		boolean width = false;
        		boolean height = false;
        		switch(dm.getHeight()){
        		case 360:
        		case 480:
        		case 720:
        		case 900:
        		case 960:
        		case 1080:
        			height = true;
        			break;
        		default:
        		}
        		switch(dm.getWidth()){
        		case 480:
        		case 640:
        		case 1280:
        		case 1440:
        		case 1600:
        		case 1920:
        			width = true;
        			break;
        		default:
        		}
        		if(freq && width && height && modes.size()<7){
        			modes.put(""+dm.getFrequency()+dm.getHeight()+dm.getWidth(),dm);
        		}
        	}
        	
        	dms = new ArrayList<DisplayMode>(modes.values());
   	
        	for(DisplayMode md : dms){ // statt adm ein array was obbe erzeugt wird an if(fruq && width && height)`
        		logger.debug("Height {} Width {} Freq {} {} BPP {}", md.getHeight(),md.getWidth(),md.getFrequency(), calculateRatio(md), md.getBitsPerPixel());
        		// alternativ ein [print to screen zur auswahl]
        	}
            Display.setDisplayMode(dms.get(dmi));
            Display.create(new PixelFormat(32, 0, 24, 0, useAA ? 4 : 0), attribs);
            Display.setTitle("Four connect, KBS demo");
            Display.setFullscreen(fullscreen);
        } catch (LWJGLException e) {
            logger.debug("Error with the Display-Modes {}", e);  
        }
        
        GL11.glViewport(0, 0, dms.get(dmi).getWidth(), dms.get(dmi).getHeight());
        lastFrameTime = getCurrentTime();
    }
    
    private static String calculateRatio(DisplayMode dm) {
    	String ratio;
    	int nw = -1;
    	int nh = -1;
    	String nws = "N/A";
		String nhs = "N/A";		
    	if(((double)dm.getHeight()/(double)dm.getWidth() * 5) == 4) {
    		nw = 5;
    		nh = 4;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 4) == 3) {
    		nw = 4;
    		nh = 3;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 3) == 2) {
    		nw = 3;
    		nh = 2;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 16) == 10) {
    		nw = 16;
    		nh = 10;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 5) == 3) {
    		nw = 5;
    		nh = 3;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 16) == 9) {
    		nw = 16;
    		nh = 9;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 19) == 10) {
    		nw = 19;
    		nh = 10;
    	}else if(((double)dm.getHeight()/(double)dm.getWidth() * 21) == 9) {
    		nw = 21;
    		nh = 9;
    	}
    	if(nw != -1 && nh != -1) {
    		nws = Integer.toString(nw);
    		nhs = Integer.toString(nh);
    	}
    	ratio = "Ratio " + nws + " : " + nhs;
    	return ratio;
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
    
    public static List<DisplayMode> getDms() {
		return dms;
	}
    
    public static int getDmi() {
    	return dmi;
    }
    
    public void setDmi(int dmi) {
    	DisplayManager.dmi = dmi;
    }
    
    private static long getCurrentTime() {
        return Sys.getTime() * 1000 / Sys.getTimerResolution();
    }       
   
}
