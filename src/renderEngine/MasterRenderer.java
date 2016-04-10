/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renderEngine;

import entities.Camera;
import entities.Entity;
import entities.Light;
import main.MainGameLoop;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.TexturedModel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import shaders.StaticShader;
import shaders.TerrainShader;
import terrain.Terrain;

/**
 *
 * @author Trist
 */
public class MasterRenderer {
    
	
    private static final float FOV = 70;
    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 1000;
    
    private static final float RED = 0.52f; 
    private static final float GREEN = 0.74f; 
    private static final float BLUE = 1; 
    
    private Matrix4f projectionMatrix;
    private StaticShader shader = new StaticShader();
    private EntityRenderer renderer;
    
    private TerrainRenderer terrainRenderer;
    private TerrainShader terrainShader = new TerrainShader();
    
    private Map<TexturedModel, List<Entity>> entities = new HashMap<>();
    private List<Terrain> terrains = new ArrayList<>();
    
    private Logger logger = LogManager.getLogger();
    
    public MasterRenderer() {
    	if(!GLContext.getCapabilities().GL_EXT_framebuffer_object){
    		logger.error("Unable to use FBO for render to texture !");
    	}
        enableCulling();  
        createProjectionMatrix();
        renderer = new EntityRenderer(shader, projectionMatrix);
        terrainRenderer = new TerrainRenderer(terrainShader, projectionMatrix);
    }
    
    public static void enableCulling() {
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);  
    }
    
    public static void disableCulling() {
        glDisable(GL_CULL_FACE);        
    }
    
    public Matrix4f getProjectionMatrix() {
    	return projectionMatrix;
    }
    
    public void render(List<Light> lights, Camera camera) {
        prepare();
        shader.start();
        shader.loadSkyColour(RED, GREEN, BLUE);
        shader.loadLights(lights);
        shader.loadViewMatrix(camera);
        renderer.render(entities);
        shader.stop();
        terrainShader.start();
        terrainShader.loadSkyColour(RED, GREEN, BLUE);
        terrainShader.loadLights(lights);
        terrainShader.loadViewMatrix(camera);
        terrainRenderer.render(terrains);
        terrainShader.stop();
        terrains.clear();
        entities.clear();
    }
    
    /**
     * Prepare render to texture
     * @param width
     * @param height
     * @return texture id
     */
	public FBO prepareRender2Texture(int width, int height){
		//frame buffer
		int FBOId = glGenFramebuffersEXT();
		logger.debug(checkError());
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, FBOId);
		
		logger.debug(checkError());
		
		//depth buffer
		int depthbuffer_id = glGenRenderbuffersEXT();
		logger.debug(checkError());
		glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, depthbuffer_id);
		
		logger.debug(checkError());
		
		//allocate space for the renderbuffer
		glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT, width, height);
		
		logger.debug(checkError());
		
		//attach depth buffer to fbo
		glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, depthbuffer_id);
		
		logger.debug(checkError());
		
		//create texture to render to
		int texture = glGenTextures();
		logger.debug(checkError());
		glBindTexture(GL_TEXTURE_2D, texture);
		logger.debug(checkError());
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		logger.debug(checkError());
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer)null);
		logger.debug(checkError());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		logger.debug(checkError());
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	    
		logger.debug(checkError());
		
	    //attach texture to the fbo
	    glFramebufferTexture2DEXT( GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, texture, 0);
		
	    logger.debug(checkError());
		
		if(glCheckFramebufferStatusEXT(GL_FRAMEBUFFER_EXT) != GL_FRAMEBUFFER_COMPLETE_EXT)
			logger.error("Frame buffer creation failed!");
		checkFrameBuffer(FBOId);
		
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); // would switch to normal screen buffer
		logger.debug(checkError());
    	return new FBO(FBOId,texture);
	}
	
	/**
	 * Clear framebuffer of "render to texture"<br>
	 * And switch to it's buffer
	 * @param handle
	 */
	public void startFBO(int handle, int width, int height){
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, handle); // bind to framebuffer
		logger.debug(checkError());
//		GL11.glDisable(GL11.GL_TEXTURE_2D);
//		logger.debug(checkError());
//		logger.debug(checkError());
//		glPushAttrib(GL_VIEWPORT_BIT);
//		logger.debug(checkError());
//		glViewport(0, 0, width, height);
//		logger.debug(checkError());
//        glClear (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear screen & depth buffer
//        logger.debug(checkError());
    }
	
	public void endFBO(int width, int height){
		glDrawBuffer(1);
		logger.debug(checkError());
		glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);
		logger.debug(checkError());
//		glPopAttrib();
//		logger.debug(checkError());
	}
	
	/**
	 * Check for error's and print em
	 * @return string with details
	 */
	public String checkError(){
		int errorFlag = GL11.glGetError();
		if(errorFlag != GL11.GL_NO_ERROR){
			return "OpenGL Error: "+GLU.gluErrorString(errorFlag);
		}else{
			return "No Error";
		}
	}
	
	
	
	public void renderFBOToScreen(int texture, int width, int height){
//		glLoadIdentity ();                                              // Reset The Modelview Matrix
//        glTranslatef (0.0f, 0.0f, -6.0f);                               // Translate 6 Units Into The Screen and then rotate
//        glRotatef(0,0.0f,1.0f,0.0f);
//        glRotatef(0,1.0f,0.0f,0.0f);
//        glRotatef(0,0.0f,0.0f,1.0f);
//        glColor3f(1,1,1);                                               // set the color to white
//		glEnable(GL_TEXTURE_2D);
		logger.debug(checkError());
		glBindTexture(GL_TEXTURE_2D, texture);
		logger.debug(checkError());
		
		logger.debug(checkError());
		drawTexture(0,0,width,height);
		logger.debug(checkError());
		glBindTexture(GL_TEXTURE_2D, 0);

	}
	
	private void checkFrameBuffer(int buffer){
		int framebuffer = glCheckFramebufferStatusEXT( GL_FRAMEBUFFER_EXT ); 
		switch ( framebuffer ) {
		    case GL_FRAMEBUFFER_COMPLETE_EXT:
		        break;
		    case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT exception" );
		    case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT exception" );
		    case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT exception" );
		    case GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT exception" );
		    case GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT exception" );
		    case GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT:
		        throw new RuntimeException( "FrameBuffer: " + buffer
		                + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT exception" );
		    default:
		        throw new RuntimeException( "Unexpected reply from glCheckFramebufferStatusEXT: " + buffer );
		}
	}
	
	private void drawTexture(float x, float y, int width, int height) {
		glBegin(GL_QUADS);
		glTexCoord2f(0f, 0f);
		logger.debug(checkError());
		glVertex2f(x, y);
		
		glTexCoord2f(1f, 0f);
		glVertex2f(x + width, y);
		
		glTexCoord2f(1f, 1f);
		glVertex2f(x + width, y + height);
		
		glTexCoord2f(0f, 1f);
		glVertex2f(x, y + height);
		glEnd();
	}
    
    public void render() {
    	prepare();
    	renderer.render(entities);
    	entities.clear();
    }
    
    public void processTerrain(Terrain terrain) {
        terrains.add(terrain);
    }
    
    public void processEntity(Entity entity) {
        TexturedModel entityModel = entity.getModel();
        List<Entity> batch = entities.get(entityModel);
        if(batch!=null) {
            batch.add(entity);
        }else{
            List<Entity> newBatch = new ArrayList<Entity>();
            newBatch.add(entity);
            entities.put(entityModel, newBatch);
        }
    }
    
    public void cleanUp() {
        shader.cleanUp();
        terrainShader.cleanUp();
    }
    
    public void prepare(){
        glEnable(GL_DEPTH_TEST);
        logger.debug(checkError());
        glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        logger.debug(checkError());
        glClearColor(RED, GREEN, BLUE, 1);
        logger.debug(checkError());
    }
    
    private void createProjectionMatrix() {
    	float aspectRatio = (float) Display.getWidth() / (float) Display.getHeight();
        float y_scale = (float) ((1f / Math.tan(Math.toRadians(FOV/2f))) * aspectRatio);
        float x_scale = y_scale / aspectRatio;
        float frustum_length = FAR_PLANE - NEAR_PLANE;
        
        projectionMatrix = new Matrix4f();
        projectionMatrix.m00 = x_scale;
        projectionMatrix.m11 = y_scale;
        projectionMatrix.m22 = -((FAR_PLANE + NEAR_PLANE) / frustum_length);
        projectionMatrix.m23 = -1;
        projectionMatrix.m32 = -((2 * NEAR_PLANE * FAR_PLANE) / frustum_length);
        projectionMatrix.m33 = 0;
    }
    
}
