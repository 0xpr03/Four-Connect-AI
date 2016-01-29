package objConverter;

import models.boxVerticies;

public class ModelData {

	private float[] vertices;
	private float[] textureCoords;
	private float[] normals;
	private int[] indices;
	private float furthestPoint;
	private boxVerticies boxvert;

	public ModelData(float[] vertices, float[] textureCoords, float[] normals, int[] indices,
			float furthestPoint, boxVerticies boxvert) {
		this.vertices = vertices;
		this.textureCoords = textureCoords;
		this.normals = normals;
		this.indices = indices;
		this.furthestPoint = furthestPoint;
		this.boxvert = boxvert;
	}
	
	public boxVerticies getboxVerticies(){
		return boxvert;
	}

	public float[] getVertices() {
		return vertices;
	}

	public float[] getTextureCoords() {
		return textureCoords;
	}

	public float[] getNormals() {
		return normals;
	}

	public int[] getIndices() {
		return indices;
	}

	public float getFurthestPoint() {
		return furthestPoint;
	}

}
