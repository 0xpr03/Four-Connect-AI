/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import buttons.AbstractButton;
import buttons.Button;
import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.Rohr;
import fontMeshCreator.FontType;
import fontMeshCreator.GUIText;
import fontRendering.TextMaster;
import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import gamelogic.GController;
import gamelogic.AI.WebPlayer;
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
		INTRO, MAIN_MENU, GAME, INGAME_MENU, SPMENU, OPMENU, OPMENU_IG;
	}
	public static enum Color {
		YELLOW, RED;
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
	private static Entity boden;
	private static List<Entity> allentities;
	private static List<Light> lights;
	private static Light auswahlLicht;
	private static Light auswahlLicht2;
	private static Light auswahlLicht3;
	private static Light auswahlLicht4;
	private static Light testLight;
	private static GuiTexture intro;
	private static List<List<AbstractButton>> allButtons;
	private static List<AbstractButton> iMButtonList;
	private static List<AbstractButton> sMButtonList;
	private static List<AbstractButton> SP_ButtonList;
	private static List<AbstractButton> opButtonList;
	private static List<List<GUIText>> allTexts;
	private static List<GUIText> iMButtonTexts;
	private static List<GUIText> sMButtonTexts;
	private static List<GUIText> SP_ButtonTexts;
	private static List<GUIText> opButtonTexts;
	private static Rohr[] pipes = new Rohr[7];
	private static Entity[][] balls = new Entity[7][6];
	protected static Vector3f lastCamPos;
	private static float lastCamRotY;
	private static TexturedModel ballR;
	private static TexturedModel ballG;
	private static boolean shallMoveBall = false;
	private static int lastSpalte = 0;
	private static int lastZeile = 0;
	private static boolean staticCamera = true;
	private static boolean flippedView = true;
	private static int chosenRohr = 3;
	private static int rohre = 0;
	private static TexturedModel rohr;
	private static boolean callAI;
	private static boolean backgroundGame = false;
	private final static Vector3f startCamPos = new Vector3f(15, -3, 75);
	
	private final static Level LOG_CONTRBASE = Level.WARN;
	private final static boolean USE_AA = true; // anti-aliasing
	private final static boolean RENDER_LINES = false; // render vector lines

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		Configurator.setLevel(LogManager.getLogger(MainGameLoop.class).getName(), Level.ALL);
		Configurator.setLevel("DB", Level.INFO);
		Configurator.setLevel("Controller", Level.ALL);
		Configurator.setLevel(LogManager.getLogger(WebPlayer.class).getName(), Level.ALL);
		
		checkLoggingConf();
		logger.info("Version {}", VERSION);
		
		DisplayManager.createDisplay(USE_AA);
		loader = new Loader();
		TextMaster.init(loader);
		
		FontType font = new FontType(loader.loadTexture("tahoma"), new File("res/tahoma.fnt"));
		
		allTexts = new ArrayList<>();
		iMButtonTexts = new ArrayList<>();
		iMButtonTexts.add(new GUIText("Resume", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Restart", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Options", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Concede", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		iMButtonTexts.add(new GUIText("Exit to Main Menu", 5, font, new Vector2f(0, 0.82f), 1f, true, true));
		allTexts.add(iMButtonTexts);
		
		sMButtonTexts = new ArrayList<>();
		sMButtonTexts.add(new GUIText("Singleplayer", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Multiplayer", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Options", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		sMButtonTexts.add(new GUIText("Exit", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		allTexts.add(sMButtonTexts);
		
		SP_ButtonTexts = new ArrayList<>();
		SP_ButtonTexts.add(new GUIText("7x6 WBS2", 5, font, new Vector2f(0, 0.02f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("6x5 Hard", 5, font, new Vector2f(0, 0.22f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("4x4 Hard", 5, font, new Vector2f(0, 0.42f), 1f, true, true));
		SP_ButtonTexts.add(new GUIText("Back", 5, font, new Vector2f(0, 0.62f), 1f, true, true));
		allTexts.add(SP_ButtonTexts);
		
		opButtonTexts = new ArrayList<>();
		for(int i = DisplayManager.getDmi(); i <DisplayManager.getDms().size(); i++) {
			opButtonTexts.add(new GUIText(DisplayManager.getDms().get(i).getWidth() +"x"+ DisplayManager.getDms().get(i).getHeight(), 2, font, new Vector2f(0,0.05f+0.1f * i), 1f, true, true));
		}
		opButtonTexts.add(new GUIText("Fullscreen", 2, font, new Vector2f(0, 0.77f), 1f, true, true));
		opButtonTexts.add(new GUIText("Back", 2, font, new Vector2f(0, 0.87f), 1f, true, true));
		allTexts.add(opButtonTexts);
		initButtons();
		
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
		TexturedModel lamp = loader.loadtoVAO("lamp", "lamp");
		
		TexturedModel brett = loader.loadtoVAO("brett", "boden2");
		brett.getTexture().setShineDamper(7);
		brett.getTexture().setReflectivity(1);
		
		ballR = loader.loadtoVAO("kugel", "kugelR");
		
		ballG = loader.loadtoVAO("kugel", "kugelG");
		
		rohr = loader.loadtoVAO("rohr2", "rohrTexture");
		
		logger.trace("creating entities");
		terrain = new Terrain(-0.5f, -0.5f, loader, texturePack, blendMap, "black");
		
		allentities = new ArrayList<>();
		
		menuGuis = new ArrayList<GuiTexture>();
//		menuGuis.add(new GuiTexture(loader.loadTexture("null"), new Vector2f(0.0f, 0.0f),
//				new Vector2f(1f, 1f)));
		
		guiRenderer = new GuiRenderer(loader);
		
		camera = new Camera(startCamPos, 0, 20);
		
		picker = new MousePicker(camera, renderer.getProjectionMatrix(), terrain);
		
		intro = new GuiTexture(loader.loadTexture("testIntro"), //needs to be squared and the pixel count must be 2^n
				new Vector2f(0f, 0f),
				new Vector2f(Display.getWidth()/Display.getHeight(), 1f));
		lampTest = new Entity(lamp, new Vector3f(15, terrain.getHeightOfTerrain(0, 0)+ 50, 0), 0, 0, 0, 1);
		lampTest.increaseRotation(0, 0, 180);
		allentities.add(lampTest);
		boden = new Entity(brett, new Vector3f(15, terrain.getHeightOfTerrain(0, 0), 0), 0, 0, 0, 5);
		allentities.add(boden);
		
		Light sun = new Light(new Vector3f(7000, 10000, -7000), new Vector3f(0.4f, 0.4f, 0.4f));
		auswahlLicht = new Light(new Vector3f(0, terrain.getHeightOfTerrain(0, 0)+32, 0), new Vector3f(0, 0, 40), new Vector3f(0, 0.04f, 1f));
		auswahlLicht2 = new Light(new Vector3f(0, terrain.getHeightOfTerrain(0, 0)+25, 0), new Vector3f(0, 0, 40), new Vector3f(0, 0.04f, 1.3f));
		auswahlLicht3 = new Light(new Vector3f(0, terrain.getHeightOfTerrain(0, 0)+15, 0), new Vector3f(0, 0, 40), new Vector3f(0, 0.04f, 1.4f));
		auswahlLicht4 = new Light(new Vector3f(0, terrain.getHeightOfTerrain(0, 0)+5, 0), new Vector3f(0, 0, 40), new Vector3f(0, 0.04f, 1.5f));
		lights = new ArrayList<>();
		lights.add(sun);
		testLight = new Light(new Vector3f(-30, -30, 0), new Vector3f(0.9f, 0, 0.3f), new Vector3f(0.5f,0,0));
		lights.add(testLight);
		lights.add(auswahlLicht);
		lights.add(auswahlLicht2);
		lights.add(auswahlLicht3);
		lights.add(auswahlLicht4);
//		lights.add(new Light(new Vector3f(10, terrain.getHeightOfTerrain(10, 10) + 20, 10), new Vector3f(5, 0, 0), new Vector3f(1, 0.01f, 0.002f)));
				
		GController.init();
		logger.trace("entering renderer");
		
		try{
		while (!Display.isCloseRequested()) {
			checkInputs();
			states();			
		}
		}catch(Exception e){
			System.err.println(e);
			logger.fatal("ERROR {}",e);
		}
		exit();
	}
	
	private static void states() {
		if(RENDER_LINES){
			GL11.glEnable(GL11.GL_BLEND);
		    GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		    GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT,GL11.GL_DONT_CARE);
		}
		switch(state) {
		case INTRO:
			guiRenderer.render(intro);
			TextMaster.render();
			DisplayManager.updateDisplay();
			break;
		case MAIN_MENU:
			renderMenuSzene(sMButtonList,true);
			break;
		case SPMENU:
			renderMenuSzene(SP_ButtonList,true);
			break;
		case GAME:
			renderGame();
			DisplayManager.updateDisplay();
			break;
		case INGAME_MENU:
			renderMenuSzene(iMButtonList,false);
			break;
		case OPMENU:
			renderMenuSzene(opButtonList, true);
			break;
		case OPMENU_IG:
			renderMenuSzene(opButtonList, true);
			break;
		default:
			logger.fatal("Unknown case: {}",state);
			break;
		}
	}
	
	private static void renderGame(){
		camera.move(terrain);
		picker.update();
		if (shallMoveBall == true){
			moveBall(lastSpalte, lastZeile);
		}else if (callAI){
			if(backgroundGame){
				switch(GController.getGameState()){
				case PLAYER_A:
				case PLAYER_B:
					List<Integer> i = GController.getPossibilities();
					insertStone(i.get(ThreadLocalRandom.current().nextInt(0, i.size())));
					break;
				default:
					logger.debug(GController.getGameState());
					stopBackgroundGame();
					startBackgroundGame();
					break;
				}
			}else{
				GController.moveAI_A();
				callAI = false;
			}
		}
		Vector3f terrainPoint = picker.getCurrentTerrainPoint(); //Gibt den Punkt aus, auf dem mouse Ray auf terrain trifft.
		if(terrainPoint != null &&terrainPoint.getX() >= 50) {
			testLight.setColour(new Vector3f(0.9f, 0, 0.3f));
		}else {
			testLight.setColour(new Vector3f(0, 0, 0));
		}
		renderer.processTerrain(terrain);
		for (Entity entity : allentities) {
			renderer.processEntity(entity);
		}
		for (Entity entity : pipes) {
			if(entity == null)
				break;
			renderer.processEntity(entity);
		}
		for (Entity[] entityA : balls) {
			if(entityA == null){
				continue;
			}
			for(Entity entity : entityA) {
				if(entity == null){
					break;
				}
				renderer.processEntity(entity);
			}
		}
		auswahlLicht.setPosition(new Vector3f(chosenRohr*5, terrain.getHeightOfTerrain(0, 0)+ 32, 0)); 
		auswahlLicht2.setPosition(new Vector3f(chosenRohr*5, terrain.getHeightOfTerrain(0, 0)+ 25, 0)); 
		auswahlLicht3.setPosition(new Vector3f(chosenRohr*5, terrain.getHeightOfTerrain(0, 0)+ 18, 0)); 
		auswahlLicht4.setPosition(new Vector3f(chosenRohr*5, terrain.getHeightOfTerrain(0, 0)+ 11, 0)); 
		renderer.render(lights, camera);
		TextMaster.render();
	}
	
	private static void hideAllMenus(boolean resetCam) {
		if(resetCam){
			camera.resetMovement();
			if(lastCamPos != null){
				camera.setPosition(lastCamPos);
				camera.setRotY(lastCamRotY);
				logger.debug("lastCamPos {}", lastCamPos);
			}
			else{
				camera.setPosition(startCamPos);
				camera.setRotY(0);
				logger.debug("startCamPos {}", startCamPos);
			}
		}
		for(List<GUIText> l : allTexts) {
			for(GUIText g : l) {
				g.hide();
			}
		}
		for(List<AbstractButton> l : allButtons) {
			for(AbstractButton b : l) {
				b.hide(menuGuis);
			}
		}
	}
	
	private static void showMenu(List<GUIText> textlist) {
		for(GUIText g : textlist) {
			g.show();
		}
	}
	
	/**
	 * Render menu with background
	 * @param buttonList
	 * @param animateCam
	 */
	private static void renderMenuSzene(List<AbstractButton> buttonList, final boolean animateCam){
		guiRenderer.startBackgroundRendering();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		if (animateCam) {
			camera.resetMovement();
			camera.increaseRotation(0.078f, 0f);
			camera.increaseSideSpeed(-3);
			camera.move();
		}
		renderGame();
		guiRenderer.endBackgroundRendering();
		guiRenderer.renderBackground(renderer);
		
		for(AbstractButton b : buttonList) {
			b.show(menuGuis);
			b.update();
		}
		guiRenderer.render(menuGuis);
		TextMaster.render();
		DisplayManager.updateDisplay();
	}
	
	private static void checkInputs() {
		if(Keyboard.isKeyDown(Keyboard.KEY_F9)) {
			logger.debug("State: {}",state);
		}
		switch(state) {
		case INTRO:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
					showMenu(sMButtonTexts);
					startBackgroundGame();
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
			logger.entry();
			break;
		case GAME:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
					lastCamPos = camera.getPosition();
					lastCamRotY = camera.getRotY();
		 			for(GUIText g : iMButtonTexts) {
						g.show();
					}
		 			state = State.INGAME_MENU;
		 		}else{
		 			if(Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
						if(flippedView && chosenRohr != 0){
							chosenRohr -= 1;
							lampTest.increasePosition(-5, 0, 0);
						}else if(!flippedView && chosenRohr != (rohre-1)){
							chosenRohr += 1;
							lampTest.increasePosition(5, 0, 0);
						}
					}
		 			if(Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
						if(flippedView && chosenRohr != (rohre-1)){
							chosenRohr += 1;
							lampTest.increasePosition(5, 0, 0);
						}else if(!flippedView && chosenRohr != 0){
							chosenRohr -= 1;
							lampTest.increasePosition(-5, 0, 0);
						}
	
					}
		 			if(Keyboard.isKeyDown(Keyboard.KEY_RETURN)) {
						if(!shallMoveBall && !callAI){
							insertStone(chosenRohr);
						}
					}
					if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_F))
						staticCamera = !staticCamera;
		 		}
			}
		break;
		case INGAME_MENU:
			while(Keyboard.next()) {
				if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
					hideAllMenus(false);
					state = State.GAME;
				}
			}
		break;
		case OPMENU:
			break;
		default:
			logger.warn("Unknown case for input!");
		}
	}
	
	/**
	 * Insert stone at given point, to be used by player inputs<br>
	 * also sends a command to the controller
	 * @param spalte
	 */
	private static void insertStone(int spalte) {
		logger.entry(spalte);
		if(pipes[spalte] == null)
			return;
		Color color = null;
		if(GController.getGameState() == E_GAME_STATE.PLAYER_A){
			color = Color.RED;
		}else if(GController.getGameState() == E_GAME_STATE.PLAYER_B){
			color = Color.YELLOW;
		}
		if(GController.insertStone(spalte) && color != null){
			setStone(spalte,color);
		}else{
			logger.error("Controller denied insert! {}", GController.getGameState());
			logger.info(GController.getprintedGameState());
		}
	}
	
	/**
	 * Core of the stone insert, only for AI calls, does not ask the controller<br>
	 * Pure GUI handling
	 * @param spalte
	 * @param color
	 */
	public static void setStone(int spalte, Color color){
		logger.entry(spalte,color);
		int zeile = (pipes[spalte]).getBalls();
		if(zeile == 6)
			return;
		balls[spalte][zeile] = new Entity(color == Color.RED ? ballR : ballG, new Vector3f(
				spalte * 5, terrain.getHeightOfTerrain(0, 0)+35, 0), 0, 0, 0, 0);		
		(pipes[spalte]).setBalls(zeile +1);
		shallMoveBall = true;
		lastSpalte = spalte;
		lastZeile = zeile;
	}
	
	/**
	 * Move the ball and animate it<br>
	 * Sets callAI on finish
	 * @param spalte
	 * @param zeile
	 */
	private static void moveBall(int spalte, int zeile){
		if(balls[spalte][zeile] == null)
			return;
		if(balls[spalte][zeile].getScale()<2) {
			balls[spalte][zeile].setScale(balls[spalte][zeile].getScale()+0.1f);
		}
		else if(balls[spalte][zeile].getPosition().getY() > terrain.getHeightOfTerrain(0, 0) + 3 + zeile * 4) {
			balls[spalte][zeile].increasePosition(0, -0.5f, 0);
		}else{
			shallMoveBall = false;
			if(GController.getGamemode() == E_GAME_MODE.SINGLE_PLAYER && GController.getGameState() == E_GAME_STATE.PLAYER_B)
				callAI = true; // call AI in next frame
			else if(GController.getGamemode() == E_GAME_MODE.MULTIPLAYER && !backgroundGame)
				moveCam();
		}
	}
	
	private static void moveCam() {
		int z = (int)camera.getPosition().getZ();
		if(z<0) {
			camera.increasePosition(0, 0, 150);
			camera.increaseRotation(180, 0);
		}else {
			camera.increasePosition(0, 0, -150);
			camera.increaseRotation(180, 0);
		}
		flippedView = z<0;
	}
	
	/**
	 * Init menu button list
	 */
	private static void initButtons(){
		
		/**
		 * INGAME MENU
		 */
		allButtons = new ArrayList<>();
		iMButtonList = new ArrayList<>();
		iMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.8f), new Vector2f(0.4f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("resume");
				hideAllMenus(false);
				state = State.GAME;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.4f), new Vector2f(0.4f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("restart");				
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0f), new Vector2f(0.4f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Options");	
				hideAllMenus(true);
				showMenu(opButtonTexts);
				state = State.OPMENU_IG;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.4f), new Vector2f(0.4f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("concede");				
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		iMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.8f), new Vector2f(0.7f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("exit to main menu");		
				hideAllMenus(true);
				showMenu(sMButtonTexts);
				cleanupGame();
				startBackgroundGame();
				state = State.MAIN_MENU;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		
		allButtons.add(iMButtonList);
		
		/*
		 * START MENU
		 */
		sMButtonList = new ArrayList<>();
		sMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.8f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Menu SinglePlayer");
				hideAllMenus(false);
				showMenu(SP_ButtonTexts);
				state = State.SPMENU;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.4f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Menu Multiplayer");
				startGame(7,6, E_GAME_MODE.MULTIPLAYER, false);
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Options");	
				hideAllMenus(true);
				showMenu(opButtonTexts);
				state = State.OPMENU;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		sMButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.4f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Exit");
				exit();
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		
		allButtons.add(sMButtonList);
		
		/**
		 * Singleplayer Menu
		 */
		
		SP_ButtonList = new ArrayList<>();
		SP_ButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.8f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("7x6 Default");
				startGame(7,6, E_GAME_MODE.SINGLE_PLAYER, false);
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.4f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("6x5 Hard");
				startGame(6,5, E_GAME_MODE.SINGLE_PLAYER, false);
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("4x4 Hard");
				startGame(4,4,E_GAME_MODE.SINGLE_PLAYER, false);
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		SP_ButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.4f), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {
				logger.trace("Back");
				hideAllMenus(false);
				showMenu(sMButtonTexts);
				state=State.MAIN_MENU;
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		
		allButtons.add(SP_ButtonList);
		
		opButtonList = new ArrayList<>();
		for(int i = DisplayManager.getDmi(); i<DisplayManager.getDms().size(); i++) {
			int it= i;
			opButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,0.83f-0.2f*i), new Vector2f(0.2f, 0.08f)) {			
				public void onClick(Button button) {		
					try {
						Display.setDisplayMode(DisplayManager.getDms().get(it));
					} catch (LWJGLException e) {
					}
				}
				public void onStartHover(Button button) {
					this.playHoverAnimation(0.02f);
				}			
				public void onStopHover(Button button) {
					this.resetScale();
				}
				public void whileHovering(Button button) {}			
			});
		}
		opButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.6f), new Vector2f(0.2f, 0.1f)) {			
			public void onClick(Button button) {
				logger.trace("Fscreen");
				try {
					Display.setFullscreen(!Display.isFullscreen());
				} catch (LWJGLException e) {
					e.printStackTrace();
				}
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
		opButtonList.add(new AbstractButton(loader, "whiteButton", new Vector2f(0,-0.8f), new Vector2f(0.2f, 0.1f)) {			
			public void onClick(Button button) {
				logger.trace("Back");
				hideAllMenus(false);
				if(state == State.OPMENU) {
				showMenu(sMButtonTexts);
				state=State.MAIN_MENU;
				}else {
					showMenu(iMButtonTexts);
					state = State.INGAME_MENU;
				}
			}
			public void onStartHover(Button button) {
				this.playHoverAnimation(0.03f);
			}			
			public void onStopHover(Button button) {
				this.resetScale();
			}
			public void whileHovering(Button button) {}			
		});
	
		allButtons.add(opButtonList);
		//Fullscreen button nicht vergessen Du hässlichkeit
		/*opButtonList.add(new AbstractButton(loader, "null", new Vector2f(0,1f-0.1f*i), new Vector2f(0.5f, 0.17f)) {			
			public void onClick(Button button) {		
				try {
					Display.setDisplayMode(DisplayManager.getDms().get(it));
				} catch (LWJGLException e) {
				}
			}
			public void onStartHover(Button button) {
			}			
			public void onStopHover(Button button) {
			}
			public void whileHovering(Button button) {}			
		});*/
		
	}
	
	public static boolean getStaticCamera() {
		return staticCamera;
	}
	
	/**
	 * End game, inform controller, cleanup GUI
	 */
	private static void cleanupGame(){
		GController.stopGame();
		shallMoveBall = false;
		lastCamPos = null;
		lastCamRotY = 0;
		callAI = false;
		for(Entity[] ball_col : balls){
			for(int i = 0; i < ball_col.length; i++){
				ball_col[i] = null;
			}
		}
		for(int i = 0; i < pipes.length; i++)
			pipes[i] = null;
	}
	
	/**
	 * Start game, setup GUI, inform controller
	 * @param anzahl_spalten
	 * @param anzahl_zeilen
	 * @param gameMode
	 * @param background set to true to enable background games
	 */
	private static void startGame(int anzahl_spalten, int anzahl_zeilen, E_GAME_MODE gameMode, boolean background){
		if(!background){
			stopBackgroundGame();
			hideAllMenus(true);
			state = State.GAME;
		}
		rohre = anzahl_spalten;
		for(int i = 0; i< rohre; i++) {
			pipes[i] = new Rohr(rohr, new Vector3f(i*5, terrain.getHeightOfTerrain(0, 0)+1, 0), 0, 0, 0, 2); 
			pipes[i].getModel().getTexture().setUseFakeLighting(true);
		}
		callAI = false;
		GController.initGame(gameMode, LOG_CONTRBASE, anzahl_spalten,	anzahl_zeilen);
		GController.startGame();
	}
	
	public static void startBackgroundGame(){
		logger.entry();
		backgroundGame = true;
		startGame(7, 6, E_GAME_MODE.MULTIPLAYER, true);
		callAI = true;
	}
	
	public static void stopBackgroundGame(){
		logger.entry();
		backgroundGame = false;
		cleanupGame();
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
