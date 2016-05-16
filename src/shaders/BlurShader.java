/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shaders;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;

/**
 * BlurrShader<br>
 * Very simple version with some problems
 * @author Aron Heinecke
 */
public class BlurShader extends ShaderProgram{
    
    private static final String VERTEX_FILE = "/shaders/blurVertexShader.txt";
    private static final String FRAGMENT_FILE = "/shaders/blurFragmentShader.txt";
    
    private int location_direction;
    private int location_radius;
    private int location_texture;
    private int location_matrix;
    private int location_resolution;
        
    public BlurShader() {
        super(VERTEX_FILE, FRAGMENT_FILE);
    }

    @Override
    protected void bindAttributes() {
        super.bindAttribute(0, "position");
        super.bindAttribute(1, "texCoord");
        super.bindAttribute(2, "color");
    }

    @Override
    protected void getAllUniformLocations() {
    	location_direction = super.getUniformLocation("dir");
    	location_radius = super.getUniformLocation("radius");
    	location_resolution = super.getUniformLocation("resolution");
    	location_texture = super.getUniformLocation("u_texture");
        location_matrix = super.getUniformLocation("u_projView");
    }
    
    public void loadTextureVariable(int texture){
    	super.loadInt(location_texture, texture);
    }
    
    public void loadResolutionVariable(float size){
    	super.loadFloat(location_resolution, size);
    }
    
    public void loadDirectionVariable(Vector2f direction){
    	super.load2DVector(location_direction, direction);
    }
    
    public void loadRadiusVariable(float radius) {
        super.loadFloat(location_radius, radius);
    }
    
    public void loadProjectionMatrix(Matrix4f projection) {
        super.loadMatrix(location_matrix, projection);
    }
    
    
}
