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
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import models.RawModel;
import renderEngine.Loader;
import toolbox.Maths;

/**
 *
 * @author Trist
 */
public class GuiRenderer {
    
    private final RawModel quad;
    private final RawModel quad_background;
    private GuiShader shader;
//    private int vbo_vertex_handle;
//    private int vertices;
//    private BackgroundShader bShader;
    
    private Logger logger = LogManager.getLogger();
    
    public GuiRenderer(Loader loader) {
    	{
        float[] positions = { -1, 1, -1, -1, 1, 1, 1, -1 };
	        quad = loader.loadtoVAO(positions);
    	}
    	{
//    		float[] positions = {
//    				-1.0f, -1.0f,
//    		        1.0f, -1.0f,
//    		        -1f,1.0f,
//    		        1.0f,1.0f};
    		float[] positions = {
    				-1.0f, 1.0f,
    				-1f,-1.0f,
    		        1.0f,1.0f,
    		        1.0f,-1.0f
    		};
    		quad_background = loader.loadtoVAO(positions);
    	}
    	shader = new GuiShader();
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
    public void renderBackground(int texture){
    	shader.start();
    		//logger.debug(checkError());
		glBindVertexArray(quad_background.getVaoID());
        glEnableVertexAttribArray(0);
        	//logger.debug(checkError());
        glEnable(GL_BLEND);
			//logger.debug(checkError());
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);
        	//logger.debug(checkError());
		glActiveTexture(GL_TEXTURE0);
    		//logger.debug(checkError());
    	glBindTexture(GL_TEXTURE_2D, texture);
		Matrix4f matrix = Maths.createTransformationMatrix(new Vector2f(0,0), new Vector2f(1f,1f));
//		matrix.rotate(3.1399975f, new Vector3f(0f,0f,1f));
		shader.loadTransformation(matrix);
		logger.debug(quad_background.getVertexCount());
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
    
    private void initBackground(){
//    	int vertices = 4;
//    	
//    	float[] vertices = {
//    	        // Left bottom triangle
//    	        -0.5f, 0.5f, 0f,
//    	        -0.5f, -0.5f, 0f,
//    	        0.5f, -0.5f, 0f,
//    	        // Right top triangle
//    	        0.5f, -0.5f, 0f,
//    	        0.5f, 0.5f, 0f,
//    	        -0.5f, 0.5f, 0f
//    	};
//    	// Sending data to OpenGL requires the usage of (flipped) byte buffers
//    	FloatBuffer verticesBuffer = BufferUtils.createFloatBuffer(vertices.length);
//    	verticesBuffer.put(vertices);
//    	verticesBuffer.flip();
////    	
//    	vaoId = GL30.glGenVertexArrays();
//    	GL30.glBindVertexArray(vaoId);
//    	 
//    	// Create a new Vertex Buffer Object in memory and select it (bind)
//    	// A VBO is a collection of Vectors which in this case resemble the location of each vertex.
//    	vbo_vertex_handle = GL15.glGenBuffers();
//    	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
//    	GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW);
//    	// Put the VBO in the attributes list at index 0
//    	GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
//    	// Deselect (bind to 0) the VBO
//    	GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
//    	 
//    	// Deselect (bind to 0) the VAO
//    	GL30.glBindVertexArray(0);
    }
    
    public void render(GuiTexture gui){
    	List<GuiTexture> list =new ArrayList<>(1);
    	list.add(gui);
    	render(list);
    }
    
    public void cleanUp() {
        shader.cleanUp();
        glDeleteBuffers(quad.getVaoID());
        glDeleteBuffers(quad_background.getVaoID());
    }
    
}
