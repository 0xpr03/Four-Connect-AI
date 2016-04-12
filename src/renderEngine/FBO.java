package renderEngine;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;

/**
 * FrameBufferObject
 * @author Aron Heinecke
 */
public class FBO {
	private final int WIDTH;
	private final int HEIGHT;

	private int FrameBuffer;
	private int Texture;
	private int DepthBuffer;
	
	private int DepthTexture;
	private final boolean createDepthTexture;

	/**
	 * Create new FBO with fixed width & height
	 * @param height
	 * @param width
	 * @param createDepthTexture set to true if needed<br>
	 * this can't be changed afterwards
	 */
	public FBO(final int height,final int width,final boolean createDepthTexture) {
		this.WIDTH = width;
		this.HEIGHT = height;
		this.createDepthTexture = createDepthTexture;
		initFrameBuffer();
	}

	public void cleanUp() {// call when closing the game
		GL30.glDeleteFramebuffers(FrameBuffer);
		GL11.glDeleteTextures(Texture);
		GL30.glDeleteRenderbuffers(DepthBuffer);
	}

	/**
	 * Bind to this FBO instance
	 */
	public void bindFrameBuffer() {
		bindFrameBuffer(FrameBuffer, WIDTH, HEIGHT);
	}

	/**
	 * Unbind to default FBO (screen)
	 */
	public void unbindCurrentFrameBuffer() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
		GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
	}
	
	/**
	 * Get depth texturepointer
	 * @return 
	 * @throws throws an exception if the depth texture was never created
	 */
	public int getDepthTexture(){
		if(createDepthTexture)
			return DepthTexture;
		else
			throw new NullPointerException("Depth texture was not initiated!");
	}

	/**
	 * Get texture pointer
	 * @return
	 */
	public int getTexture() {
		return Texture;
	}

	/**
	 * Get depth buffer pointer
	 * @return
	 */
	public int getDepthBuffer() {
		return DepthBuffer;
	}

	private void initFrameBuffer() {
		FrameBuffer = createFrameBuffer();
		Texture = createTextureAttachment(WIDTH, HEIGHT);
		DepthBuffer = createDepthBufferAttachment(WIDTH, HEIGHT);
		if(createDepthTexture)
			DepthTexture = createDepthTextureAttachment(WIDTH,HEIGHT);
		unbindCurrentFrameBuffer();
	}

	private void bindFrameBuffer(int frameBuffer, int width, int height) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);// To make sure the texture
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer); // set as current framebuffer
		GL11.glViewport(0, 0, width, height); // set viewport
	}

	private int createFrameBuffer() {
		int frameBuffer = GL30.glGenFramebuffers();
		// generate name for frame buffer
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBuffer);
		// create the framebuffer
		GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
		// indicate that we will always render to color attachment 0
		return frameBuffer;
	}

	private int createTextureAttachment(int width, int height) {
		int texture = GL11.glGenTextures(); // create texture
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); // bind text
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, width, height, 0, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null); // reserve space
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, texture, 0); // bind to framebuffer
		return texture;
	}

	private int createDepthTextureAttachment(int width, int height) {
		int texture = GL11.glGenTextures(); // create texture
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (ByteBuffer) null); // reserve  space
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL32.glFramebufferTexture(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, texture, 0); // bind to framebuffer
		return texture;
	}

	private int createDepthBufferAttachment(int width, int height) {
		int depthBuffer = GL30.glGenRenderbuffers(); // create depth buffer
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthBuffer); // bind
		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height); // reserve space
		GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthBuffer);
		return depthBuffer;
	}

}
