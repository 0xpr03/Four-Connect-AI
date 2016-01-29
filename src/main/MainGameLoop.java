/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import entities.Camera;
import entities.Entity;
import entities.Light;
import guis.GuiRenderer;
import guis.GuiTexture;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.lwjgl.opengl.Display;
import renderEngine.DisplayManager;
import renderEngine.Loader;
import models.TexturedModel;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import renderEngine.MasterRenderer;
import terrain.Terrain;
import textures.TerrainTexture;
import textures.TerrainTexturePack;

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

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
		checkLoggingConf();
		logger.info("User {}", VERSION);

		DisplayManager.createDisplay();
		loader = new Loader();

		logger.trace("loading textures");
		// LOAD TERRAIN STUFF
		TerrainTexture backgroundTexture = new TerrainTexture(loader.loadTexture("NeuGrass"));
		TerrainTexture rTexture = new TerrainTexture(loader.loadTexture("Dreck"));
		TerrainTexture gTexture = new TerrainTexture(loader.loadTexture("Stein"));
		TerrainTexture bTexture = new TerrainTexture(loader.loadTexture("Strasse"));

		TerrainTexturePack texturePack = new TerrainTexturePack(backgroundTexture, rTexture, gTexture, bTexture);
		TerrainTexture blendMap = new TerrainTexture(loader.loadTexture("blendMapTest1"));

		logger.trace("loading models");
		// LOAD MODELS & TEXTURES
		TexturedModel tree = loader.loadtoVAO("tree", "tree");

		TexturedModel fern = loader.loadtoVAO("fern", "fern");

		TexturedModel bunny = loader.loadtoVAO("bunny", "white");

		TexturedModel lamp = loader.loadtoVAO("lamp", "lamp");

		logger.trace("creating entities");
		// Create Entities
		Terrain terrain = new Terrain(-0.5f, -0.5f, loader, texturePack, blendMap, "heightMap");

		List<Entity> allentities = new ArrayList<>();

		List<GuiTexture> guis = new ArrayList<GuiTexture>();
		GuiTexture gui = new GuiTexture(loader.loadTexture("fern"), new Vector2f(0.5f, 0.5f),
				new Vector2f(0.25f, 0.25f));
		guis.add(gui);

		guiRenderer = new GuiRenderer(loader);

		createRandomEntities(allentities,terrain, tree, 100, 300-150, -300,0f,0f,0f,4);
		createRandomEntities(allentities,terrain, fern, 100, 300-150, -300,0f,360,0f,0.9f);
		

		Light sun = new Light(new Vector3f(0, 10000, -7000), new Vector3f(0.4f, 0.4f, 0.4f));
		List<Light> lights = new ArrayList<>();
		lights.add(sun);
		lights.add(new Light(new Vector3f(185, 24, -293), new Vector3f(5, 0, 0), new Vector3f(1, 0.01f, 0.002f)));
		lights.add(new Light(new Vector3f(370, 17, -300), new Vector3f(0, 2, 2), new Vector3f(1, 0.01f, 0.002f)));
		lights.add(new Light(new Vector3f(293, 7, -305), new Vector3f(2, 2, 0), new Vector3f(1, 0.01f, 0.002f)));

		allentities.add(new Entity(lamp, new Vector3f(185, 10, -293), 0, 0, 0, 1));
		allentities.add(new Entity(lamp, new Vector3f(370, 17, -300), 0, 0, 0, 1));
		allentities.add(new Entity(lamp, new Vector3f(293, 7, -305), 0, 0, 0, 1));

		Entity start = new Entity(bunny, new Vector3f(185, 10, -293), 0, 0, 0, 0.5f);
		Camera camera = new Camera(start);
		
		logger.trace("entering renderer");
		renderer = new MasterRenderer();

		while (!Display.isCloseRequested()) {
			// player.move(terrain);
			camera.move();
			// renderer.processEntity(player);
			renderer.processTerrain(terrain);
			for (Entity entity : allentities) {
				renderer.processEntity(entity);
			}

			renderer.render(lights, camera);
			guiRenderer.render(guis);
			DisplayManager.updateDisplay();
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
		if (guiRenderer != null)
			guiRenderer.cleanUp();
		if (renderer != null)
			renderer.cleanUp();
		if (loader != null)
			loader.cleanUp();
		DisplayManager.closeDisplay();
		logger.exit();
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
