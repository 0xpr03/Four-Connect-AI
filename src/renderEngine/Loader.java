/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package renderEngine;

import models.RawModel;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import models.TexturedModel;
import objConverter.ModelData;
import objConverter.OBJFileLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import textures.ModelTexture;

/**
 *
 * @author Trist
 */
public class Loader {
    
    private List<Integer> vaos = new ArrayList<>();
    private List<Integer> vbos = new ArrayList<>();
    private List<Integer> textures = new ArrayList<>();
    
    public RawModel loadToVAO(float[] positions, float[] textureCoords, 
            float[] normals, int[] indices) {
        int vaoID = createVAO();
        bindIndicesBuffer(indices);
        storeDataInAttributeList(0, 3, positions);
        storeDataInAttributeList(1, 2, textureCoords);
        storeDataInAttributeList(2, 3, normals);
        unbindVAO();
        return new RawModel(vaoID, indices.length);        
    }
    
    /**
     * Saves a RawModel into a VAO
     * @param positions
     * @return
     */
    public RawModel loadtoVAO(float[] positions) {
        int vaoID = createVAO();
        this.storeDataInAttributeList(0, 2, positions);
        unbindVAO();
        return new RawModel(vaoID, positions.length / 2);
    }
    
    /**
     * Saves a TexturedModel into a VAO
     * @param model
     * @param texture
     * @return
     */
    public TexturedModel loadtoVAO(String model, String texture) {
        ModelData modelData = OBJFileLoader.loadOBJ(model);
        
        RawModel rawModel = loadToVAO(modelData.getVertices(), 
                modelData.getTextureCoords(), modelData.getNormals(), 
                modelData.getIndices());
        
        TexturedModel texturedModel = new TexturedModel(
                rawModel, new ModelTexture(
                loadTexture(texture)));
        return texturedModel;
    }
    
    public int loadTexture(String fileName) {
        Texture texture = null;
        try {
            texture = TextureLoader.getTexture("PNG", new FileInputStream("res/"+fileName+".png"));
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_LOD_BIAS, -0.4f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int textureID = texture.getTextureID();
        textures.add(textureID);
        return textureID;    
    }
    
    /**
     * Because you need 2 cleanUp everything in openGL, Buffer cleaning and stuff
     */
    public void cleanUp() {
        for(int vao:vaos) {
            GL30.glDeleteVertexArrays(vao);
        }
        for(int vbo:vbos) {
            GL15.glDeleteBuffers(vbo);
        }
        for(int texture:textures) {
            GL11.glDeleteTextures(texture);
        }
    }
    
    /**
     * Creates VAOs(duh) which represent an Object and they get filled with VBOs so you get normals and positions and
     * that stuff, giving you a full set of ModelData in the end.
     * @return
     */
    private int createVAO() {
        int vaoID = GL30.glGenVertexArrays();
        vaos.add(vaoID);
        GL30.glBindVertexArray(vaoID);
        return vaoID;
    }
    
    /**
     * Stores data in VBOs. VBOs are a part of the modelData and they represent the instances of data in a model
     * for example all the normal vectors get stored in a VBO. All the VBOs then get stored in a VAO.
     * @param attributeNumber
     * @param coordinateSize
     * @param data
     */
    private void storeDataInAttributeList(int attributeNumber, int coordinateSize, float[] data) {
        int vboID = GL15.glGenBuffers();
        vbos.add(vboID);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboID);
        FloatBuffer buffer = storeDataInFloatBuffer(data);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(attributeNumber, coordinateSize, GL11.GL_FLOAT, false, 0, 0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }
    
    /**
     * cleanUp stuff
     */
    private void unbindVAO() { 
        GL30.glBindVertexArray(0);
    }
    
    private void bindIndicesBuffer(int[] indices) {
        int vboID = GL15.glGenBuffers();
        vbos.add(vboID);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboID);
        IntBuffer buffer = storeDataInIntBuffer(indices);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
    }
    
    private IntBuffer storeDataInIntBuffer(int[] data) {
     IntBuffer buffer = BufferUtils.createIntBuffer(data.length);
     buffer.put(data);
     buffer.flip();
     return buffer;        
    }   
        
    private FloatBuffer storeDataInFloatBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data);
        buffer.flip();
        return buffer;
    }
}
