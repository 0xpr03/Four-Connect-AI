/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import buttons.AbstractButton;
import buttons.Button;
import entities.Camera;
import entities.Entity;
import entities.Light;
import fontMeshCreator.FontType;
import fontMeshCreator.GUIText;
import fontRendering.TextMaster;
import guis.GuiRenderer;
import guis.GuiTexture;
import models.TexturedModel;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import terrain.Terrain;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.MousePicker;

/**
 * @author Trist
 */
public class MainGameLoop {

	private static enum State {
		INTRO, MAIN_MENU, GAME, INGAME_MENU, SPMENU;
	}
	private static State state = State.INTRO;
	private static final Logger logger = LogManager.getLogger(MainGameLoop.class);
	private static String VERSION = "0.1";	
	
	// GLOBAL VARS, GAME LOOP
	private static GuiRenderer guiRenderer;
	private static MasterRenderer renderer;
	private static Loader loader;
	private static Camera camera;
	private static Terrain terrain;
	private static MousePicker picker;
	private static List<GuiTexture> menuGuis;
	private static Entity lampTest;
	private static List<Entity> allentities;
	private static List<Light> lights;
	private static GuiTexture mouseCircle;
	private static GuiTexture intro;
	private static List<AbstractButton> iMButtonList;
	private static List<AbstractButton> sMButtonList;
	private static List<AbstractButton> SP_ButtonList;
	private static GuiTexture menu;
	private static List<GUIText> iMButtonTexts;
	private static List<GUIText> sMButtonTexts;
	private static List<GUIText> SP_ButtonTexts;
	protected static Vector3f lastCamPos;
	private static float lastCamRotY;

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Configurator.setLevel(LogManager.getLogger(MainGameLoop.class).getName(), Level.DEBUG);
		
		checkLoggingConf();
		logger.info("User {}", VERSION);
		
		DisplayManager.createDisplay();
		loader = new Loader();
		TextMaster.init(loader);
		
		FontType font = new FontType(loader.loadTexture("tahoma"), new File("res/tahoma.fnt"));
		
		iMButtonTexts = new ArrayList<>();
		iMButtonTexts.add(new GUIText("Resume", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Restart", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Options", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Concede", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Exit to Main Menu", 5, font, new Vector2f(0, 0.82f), 1f, true, true));
		
		sMButtonTexts = new ArrayList<>();
		sMButtonTexts.add(new GUIText("Singleplayer", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Multiplayer", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Options", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Exit", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		
		SP_ButtonTexts = new ArrayList<>();
		SP_ButtonTexts.add(new GUIText("7x6 Default", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("6x6 Hard", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("5x5 Hard", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("Back", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		
		renderer = new MasterRenderer();
		
		logger.trace("loading textures");
		// LOAD TERRAIN STUFF
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("grass"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("grass"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("grass"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("grass"));
		
		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("black"));
		
		logger.trace("loading models");
		// LOAD MODELS & TEXTURES
		TexturedModel tree = loader.loadtoVAO("lowPolyTree", "lowPolyTree");
		
		TexturedModel lamp = loader.loadtoVAO("lamp", "lamp");
		
		logger.trace("creating entities");
		terrain = new Terrain(-0.5f, -0.5f, loader, texturePack, blendMap, "black");
		
		allentities = new ArrayList<>();
		
		menuGuis = new ArrayList<GuiTexture>();
//		menuGuis.add(new GuiTexture(loader.loadTexture("null"), new Vector2f(0.0f, 0.0f),
//				new Vector2f(1f, 1f)));
		mouseCircle = new GuiTexture(loader.loadTexture("mouse"), new Vector2f(0.5f, 0.5f), new Vector2f(0.25f, 0.35f));
		menuGuis.add(mouseCircle);
		
		guiRenderer = new GuiRenderer(loader);
		
		camera = new Camera(new Vector3f(0, 10, 0));
		
		picker = new MousePicker(camera, renderer.getProjectionMatrix(), terrain);
		
		intro = new GuiTexture(loader.loadTexture("testIntro"), //needs to be squared and the pixel count must be 2^n
				new Vector2f(0f, 0f),
				new Vector2f(Display.getWidth()/Display.getHeight(), 1f));
		menu = new GuiTexture(loader.loadTexture("lamp"), //needs to be squared and the pixel count must be 2^n
				new Vector2f(0f, 0f),
				new Vector2f(Display.getWidth()/Display.getHeight(), 1f));
		createRandomEntities(allentities,terrain, tree, 100, 300-150, -300,0f,0f,0f,0.5f);
		lampTest = new Entity(lamp, new Vector3f(0, 0, 0), 0, 0, 0, 1);
		allentities.add(lampTest);
		
		Light sun = new Light(new Vector3f(0, 10000, -7000), new Vector3f(0.4f, 0.4f, 0.4f));
		lights = new ArrayList<>();
		lights.add(sun);
//		lights.add(new Light(new Vector3f(10, terrain.getHeightOfTerrain(10, 10) + 20, 10), new Vector3f(5, 0, 0), new Vector3f(1, 0.01f, 0.002f)));
		
		initButtons();
		
		logger.trace("entering renderer");
		
		while (!Display.isCloseRequested()) {
			checkInputs();
			states();			
		}
		exit();
	}
	
	private static void states() {
		switch(state) {
		case INTRO:
			guiRenderer.render(intro);
			TextMaster.render();
			DisplayManager.updateDisplay();
			break;
		case MAIN_MENU:
			renderMenuScene(sMButtonList,true);
			break;
		case SPMENU:
			renderMenuScene(SP_ButtonList,true);
			break;
		case GAME:
			camera.move(terrain);
			picker.update();
			Vector3f terrainPoint = picker.getCurrentTerrainPoint(); //Gibt den Punkt aus, auf dem mouse Ray auf terrain trifft.
			if(terrainPoint != null) {
				lampTest.setPosition(terrainPoint);
			}
			renderer.processTerrain(terrain);
			for (Entity entity : allentities) {
				renderer.processEntity(entity);
			}
			
			renderer.render(lights, camera);
			TextMaster.render();
			DisplayManager.updateDisplay();
			break;
		case INGAME_MENU:
			renderMenuScene(iMButtonList,false);
			break;
		}
	}
	
	private static void hideIngameMenu(boolean resetCam){
		hideMenu(iMButtonList,iMButtonTexts);
	}
	
	private static void showMainMenu(){
		showMenu(sMButtonTexts);
	}
	
	private static void hideMainMenu(){
		hideMenu(sMButtonList, sMButtonTexts);
	}
	
	private static void showSPMenu(){
		showMenu(SP_ButtonTexts);
	}
	
	private static void hideSPMenu(boolean resetCam){
		if(resetCam){
			if(lastCamPos != null){
				camera.setPosition(lastCamPos);
				camera.setRotY(lastCamRotY);
			}
		}
		hideMenu(SP_ButtonList,SP_ButtonTexts);
	}
	
	private static void showMenu(List<GUIText> list){
		for(GUIText g: list){
			g.show();
		}
	}
	
	private static void hideMenu(List<AbstractButton> buttonList, List<GUIText> textList){
		for(AbstractButton b : buttonList) {
			b.hide(menuGuis);
		}
		for(GUIText g : textList) {
			g.hide();
		}
	}
	
	private static void renderMenuScene(List<AbstractButton> buttonList, final boolean animateCam){
		logger.debug(renderer.checkError());
		menuBackground.bindFrameBuffer();
			logger.debug(renderer.checkError());
		if(animateCam){
			camera.resetMovement();
			camera.increaseRotation(0.1f, 0f);
			camera.move();
		}
		float x = (2.0f * Mouse.getX()) / Display.getWidth() - 1f;
		float y = (2.0f * Mouse.getY()) / Display.getHeight() - 1f;				
		mouseCircle.setPosition(new Vector2f(x, y));
		renderer.processTerrain(terrain);
		for (Entity entity : allentities) {
			renderer.processEntity(entity); 
		}
		
		renderer.render(lights, camera);
		
		menuBackground.unbindCurrentFrameBuffer();
			logger.debug(renderer.checkError());
		renderer.prepare();
	    	logger.debug(renderer.checkError());
//		renderer.renderFBOToScreen(menuBackground.getTexture(), Display.getWidth(), Display.getHeight());
		
		for(AbstractButton b : buttonList) {
			b.show(menuGuis);
			b.update();
		}
		guiRenderer.render(menuGuis);
			logger.debug(renderer.checkError());
		TextMaster.render();
			logger.debug(renderer.checkError());
		
			logger.debug(renderer.checkError());
		DisplayManager.updateDisplay();
	}
	
	private static void checkInputs() {
		switch(state) {
		case INTRO:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
					showMainMenu();
					state = State.MAIN_MENU;
				}
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))  {
					exit();
				}
			}
		break;
		case MAIN_MENU:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))  {
					state = State.INTRO;
				}
				
			}
		break;
		case SPMENU:
			
			break;
		case GAME: 	
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
		 			for(GUIText g : iMButtonTexts) {
						g.show();
					}
		 			lastCamPos = camera.getPosition();
					lastCamRotY = camera.getRotY();
		 			logger.debug("Test");
		 			state = State.INGAME_MENU;
		 		}
			}
		break;
		case INGAME_MENU:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
					hideIngameMenu(true);
					state = State.GAME;
				}
			}
		break;
		default:
			logger.warn("Unknown case for input!");
		}
	}
	
	private static void createRandomEntities(List<Entity> allentities, Terrain terrain, TexturedModel model, int amount, int r_x, int r_z, float rotX, float rotY, float rotZ, float scale){
		logger.entry();
		Random random = new Random();
		for (int i = 0; i < amount; i++) {
			float x = random.nextFloat() * r_x;
			float z = random.nextFloat() * r_z;
			float y = terrain.getHeightOfTerrain(x, z);
			allentities.add(new Entity(model, random.nextInt(4), new Vector3f(x, y, z), rotX, random.nextFloat() * rotY, rotZ,
					scale));
		}
	}
	
	/**
	 * Init menu button list
	 */
	private static void initButtons(){
		
		/**
		 * INGAME MENU
		 */
		iMButtonList = new ArrayList<>();
		iMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.8f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("resume");
				hideIngameMenu(true);
				state = State.GAME;
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("restart");				
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("options");				
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,-0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("concede");				
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,-0.8f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("exit to main menu");		
				hideIngameMenu(false);
				showMainMenu();
				state = State.MAIN_MENU;
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		
		/*
		 * START MENU
		 */
		sMButtonList = new ArrayList<>();
		sMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.8f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("Menu SinglePlayer");
				hideMainMenu();
				showSPMenu();
				state = State.SPMENU;
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("Menu Multiplayer");				
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("Options");				
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,-0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("Exit");
				exit();
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		
		/**
		 * Singleplayer Menu
		 */
		
		SP_ButtonList = new ArrayList<>();
		SP_ButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.8f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("7x6 Default");
				startSPGame();
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("6x6 Hard");
				startSPGame();
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,0f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("5x5 Hard");
				startSPGame();
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,-0.4f), new Vector2f(0.5f, 0.15f)) {			
			public void onClick(Button button) {
				logger.trace("Back");
				hideSPMenu(false);
				showMainMenu();
				state=State.MAIN_MENU;
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});
		
	}
	
	private static void startSPGame(){
		hideSPMenu(true);
		state = State.GAME;
	}
	
	/**
	 * Cleanup openGL context for exiting
	 */
	public static void exit() {
		logger.trace("exiting");
		TextMaster.cleanUp();
		if (guiRenderer != null)
			guiRenderer.cleanUp();
		if (renderer != null)
			renderer.cleanUp();
		if (loader != null)
			loader.cleanUp();
		DisplayManager.closeDisplay();
		logger.exit();
		System.exit(0);
	}

	/**
	 * Checks if a new config is existing, to overwrite the internal logging
	 * conf
	 */
	private static void checkLoggingConf() {
		java.io.File f = new java.io.File(ClassLoader.getSystemClassLoader().getResource(".").getPath() + "/log.xml");
		if (f.exists() && f.isFile()) {
			if (Configurator.initialize(null, f.getAbsolutePath()) == null) {
				System.err.println("Faulty log config!");
			}
		}
	}

}
