/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package models;

import textures.ModelTexture;

/**
 *
 * @author Trist
 */
public class TexturedModel {
    
    private RawModel rawModel;
    private ModelTexture texture;
    
    public TexturedModel(RawModel model, ModelTexture texture) {
        this.rawModel = model;
        this.texture = texture;
    }

    public RawModel getRawModel() {
        return rawModel;
    }

    public ModelTexture getTexture() {
        return texture;
    }  
    
    
}
