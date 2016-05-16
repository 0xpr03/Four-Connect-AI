/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package guis;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import fontMeshCreator.FontType;
import fontMeshCreator.GUIText;
import models.RawModel;
import renderEngine.FBO;
import renderEngine.Loader;
import renderEngine.MasterRenderer;
import shaders.BlurShader;
import toolbox.Maths;

/**
 *
 * @author Trist
 */
public class GuiRenderer {
    
    private final RawModel quad;
    private final RawModel quad_background;
    private GuiShader shader;
    private BlurShader blurShader;
    private FBO fbo_a;
    private FBO fbo_b;
    private final Matrix4f background_matrix;
    private List<GuiTexture> renderTextures = new ArrayList<>();
    private FontType font;
    
	private int buttonTexture;
    
    private Logger logger = LogManager.getLogger();
    
    public GuiRenderer(Loader loader,FontType font) {
    	{
        float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
	        quad = loader.loadtoVAO(positions);
    	}
    	{
    		float[] positions = {
    				-1.0f, -1.0f,
    				-1f,1.0f,
    		        1.0f,-1.0f,
    		        1.0f,1.0f
    		};
    		quad_background = loader.loadtoVAO(positions);
    	}
    	this.font = font;
    	buttonTexture = loader.loadTexture("whiteButton2");
    	shader = new GuiShader();
    	blurShader = new BlurShader();
    	initFBOs();
    	background_matrix = Maths.createTransformationMatrix(new Vector2f(0,0), new Vector2f(1f,1f));
    	background_matrix.rotate((float)Math.PI, new Vector3f(0f,1f,0f));
    	background_matrix.rotate((float)Math.PI, new Vector3f(0f,0f,1f));
    }
    
    private void initFBOs(){
    	fbo_a = new FBO(Display.getHeight(), Display.getWidth(), false);
    	fbo_b = new FBO(Display.getHeight(), Display.getWidth(),false);
    }
    
    public void render() {
    	render(renderTextures);
    }
    
    public GUIText createGameOverlayText(String text){
    	return new GUIText(text, 2, font, new Vector2f(0f, 0f), 1f, false, true);
    }
    
	public void render(List<GuiTexture> guis) {
        shader.start();
        glBindVertexArray(quad.getVaoID());
        glEnableVertexAttribArray(0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        for(GuiTexture gui : guis) {
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, gui.getTexture());
            Matrix4f matrix = Maths.createTransformationMatrix(gui.getPosition(), gui.getScale());
            shader.loadTransformation(matrix);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, quad.getVertexCount());            
        }
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        shader.stop();
    }
    
    /**
     * Render texture map back to front
     * @param texture
     */
    private void drawBackground(int texture){
    	shader.start();
		glBindVertexArray(quad_background.getVaoID());
        glEnableVertexAttribArray(0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
		glActiveTexture(GL_TEXTURE0);
    	glBindTexture(GL_TEXTURE_2D, texture);
		Matrix4f matrix = Maths.createTransformationMatrix(new Vector2f(0,0), new Vector2f(1f,1f));
		matrix.rotate((float)Math.PI, new Vector3f(0f,1f,0f));
		matrix.rotate((float)Math.PI, new Vector3f(0f,0f,1f));
		shader.loadTransformation(matrix);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, quad_background.getVertexCount());  
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    	glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        shader.stop();
    }
    
    
    public String checkError(){
		int errorFlag = GL11.glGetError();
		if(errorFlag != GL11.GL_NO_ERROR){
			return "OpenGL Error: "+GLU.gluErrorString(errorFlag);
		}else{
			return "No Error";
		}
	}
    
    /**
     * Start rendering for background.<br>
     * Has to come at first, followed by the scene
     */
    public void startBackgroundRendering(){
    	fbo_a.bindFrameBuffer();
    }
    
    public void endBackgroundRendering(){
    	fbo_a.unbindCurrentFrameBuffer();
    }
    
    private void prepare(){
    	glEnable(GL_DEPTH_TEST);
    	glClear(GL_COLOR_BUFFER_BIT|GL_DEPTH_BUFFER_BIT);
        glClearColor(MasterRenderer.RED, MasterRenderer.GREEN, MasterRenderer.BLUE, 1);
    }
    
    /**
     * Add blurr to background and draw it
     */
    public void renderBackground(MasterRenderer renderer){
    	prepare();
//    	logger.debug(checkError());
    	blur(fbo_b,8f, new Vector2f(1f,0f), fbo_a.getTexture());
    	blur(fbo_a,8f, new Vector2f(0f,1f), fbo_b.getTexture());
    	
    	drawBackground(fbo_a.getTexture());
    }
    
    private void blur(FBO targetFBO, float amount, Vector2f direction,int texture){
    	targetFBO.bindFrameBuffer();
    	prepare();
    	blurShader.start();
    	glBindVertexArray(quad_background.getVaoID());
        glEnableVertexAttribArray(0);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
		glActiveTexture(GL_TEXTURE0);
    	glBindTexture(GL_TEXTURE_2D, texture);
		blurShader.loadProjectionMatrix(background_matrix);
		blurShader.loadDirectionVariable(direction);
		blurShader.loadResolutionVariable(fbo_a.getHEIGHT());
		blurShader.loadTextureVariable(0);
		blurShader.loadRadiusVariable(3);
        glDrawArrays(GL_TRIANGLE_STRIP, 0, quad_background.getVertexCount());  
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    	glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    	glDisableVertexAttribArray(0);
        glBindVertexArray(0);
        blurShader.stop();
        targetFBO.unbindCurrentFrameBuffer();
//    	logger.debug(checkError());
    }
    
    /**
	 * @return the renderTextures
	 */
	public List<GuiTexture> getRenderTextures() {
		return renderTextures;
	}
	
	/**
	 * Remove GUI Texture from rendering
	 * @param tex
	 * @return true on success
	 */
	public boolean removeRenderTexture(GuiTexture tex){
		logger.entry();
		return renderTextures.remove(tex);
	}
	
	/**
	 * Add GUI Texture for to rendering
	 * @param tex
	 * @return on success
	 */
	public boolean addRenderTexture(GuiTexture tex){
		logger.entry();
		return renderTextures.add(tex);
	}
    
    public void render(GuiTexture gui){
    	List<GuiTexture> list =new ArrayList<>(1);
    	list.add(gui);
    	render(list);
    }
    
    /**
	 * @return the buttonTexture
	 */
	public int getButtonTexture() {
		return buttonTexture;
	}
    
    public void cleanUp() {
        shader.cleanUp();
        glDeleteBuffers(quad.getVaoID());
        glDeleteBuffers(quad_background.getVaoID());
        fbo_a.destroy();
        fbo_b.destroy();
    }

	public void updateResolution() {
		fbo_a.destroy();
		fbo_b.destroy();
		initFBOs();
	}
    
}
