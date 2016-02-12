/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import entities.Camera;
import entities.Entity;
import entities.Light;
import fontMeshCreator.FontType;
import fontMeshCreator.GUIText;
import fontRendering.TextMaster;
import guis.GuiRenderer;
import guis.GuiTexture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import models.TexturedModel;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import buttons.AbstractButton;
import buttons.Button;
import renderEngine.MasterRenderer;
import terrain.Terrain;
import textures.TerrainTexture;
import textures.TerrainTexturePack;
import toolbox.MousePicker;

/**
 *
 * @author Trist
 */
public class MainGameLoop {

	private static final Logger logger = LogManager.getLogger(MainGameLoop.class);
	private static String VERSION = "0.1";
	private static GuiRenderer guiRenderer;
	private static MasterRenderer renderer;
	private static Loader loader;
	private static boolean menu = false;
	private static boolean menuToggle = true;

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		checkLoggingConf();
		logger.info("User {}", VERSION);

		DisplayManager.createDisplay();
		loader = new Loader();
		TextMaster.init(loader);
		
		FontType font = new FontType(loader.loadTexture("tahoma"), new File("res/tahoma.fnt"));
		GUIText text = new GUIText("This is a text test!", 1, font, new Vector2f(0, 0), 1f, true);

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
		// Create Entities
		Terrain terrain = new Terrain(-0.5f, -0.5f, loader, texturePack, blendMap, "black");

		List<Entity> allentities = new ArrayList<>();

		List<GuiTexture> menuGuis = new ArrayList<GuiTexture>();
		GuiTexture gui = new GuiTexture(loader.loadTexture("testMenu"), new Vector2f(0.0f, 0.0f),
				new Vector2f(1f, 1f));
		menuGuis.add(gui);
		GuiTexture test = new GuiTexture(loader.loadTexture("mouse"), new Vector2f(0.5f, 0.5f), new Vector2f(0.25f, 0.35f));
		menuGuis.add(test);

		guiRenderer = new GuiRenderer(loader);
		
		Camera camera = new Camera(new Vector3f(0, 10, 0));

		MousePicker picker = new MousePicker(camera, renderer.getProjectionMatrix(), terrain);

		createRandomEntities(allentities,terrain, tree, 100, 300-150, -300,0f,0f,0f,0.5f);
//		createRandomEntities(allentities,terrain, fern, 100, 300-150, -300,0f,360,0f,0.9f);
		Entity lampTest = new Entity(lamp, new Vector3f(0, 0, 0), 0, 0, 0, 1);
		allentities.add(lampTest);

		Light sun = new Light(new Vector3f(0, 10000, -7000), new Vector3f(0.4f, 0.4f, 0.4f));
		List<Light> lights = new ArrayList<>();
		lights.add(sun);
//		lights.add(new Light(new Vector3f(10, terrain.getHeightOfTerrain(10, 10) + 20, 10), new Vector3f(5, 0, 0), new Vector3f(1, 0.01f, 0.002f)));
	
//		allentities.add(new Entity(lamp, new Vector3f(10, terrain.getHeightOfTerrain(10, 10), 10), 0, 0, 0, 1));
		
		AbstractButton path = new AbstractButton(loader, "null", new Vector2f(0,0), new Vector2f(0.2f, 0.2f)) {

			@Override
			public void onClick(Button button) {
				logger.trace("Knopf ist gedrückt werdend");
				
			}

			@Override
			public void onStartHover(Button button) {
				button.playHoverAnimation(0.092f);
				
			}

			@Override
			public void onStopHover(Button button) {
				button.resetScale();
				
			}

			@Override
			public void whileHovering(Button button) {
				// TODO Auto-generated method stub
				
			}
			
		};	
		
		logger.trace("entering renderer");
		
		while (!Display.isCloseRequested()) {
			checkMenu();
			if(menu != true) {
			camera.move(terrain);
			picker.update();
			path.hide(menuGuis);
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
			} else {				
				float x = (2.0f * Mouse.getX()) / Display.getWidth() - 1f;
				float y = (2.0f * Mouse.getY()) / Display.getHeight() - 1f;				
				test.setPosition(new Vector2f(x, y));
				path.show(menuGuis);				
				path.update();
				renderer.render(lights, camera);	
				guiRenderer.render(menuGuis);
				TextMaster.render();
				DisplayManager.updateDisplay();
			}
		}
		exit();
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
	}
	
	private static void checkMenu() {
		if(!Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			menuToggle = true;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && menu == false && menuToggle == true) {
			menu = true;
			menuToggle = false;
		}else if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) && menu == true && menuToggle == true){
			menu = false;
			menuToggle = false;
		}
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
