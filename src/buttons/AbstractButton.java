package buttons;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

import guis.GuiRenderer;
import guis.GuiTexture;
import renderEngine.Loader;

public abstract class AbstractButton implements Button{
	
	private final GuiTexture guiTexture;
	
	private final Vector2f originalScale;
	
	private boolean isHidden= true, isHovering = false, wasUnpressed = true;
	
	private Logger logger = LogManager.getLogger();
	
	private GuiRenderer renderer;
	
	public AbstractButton(Loader loader, String texture, Vector2f position, Vector2f scale,GuiRenderer renderer) {
		guiTexture = new GuiTexture(loader.loadTexture(texture), position, scale);
		originalScale = scale;
		this.renderer = renderer;
	}
	
	public AbstractButton(int texture, Vector2f position, Vector2f scale, GuiRenderer renderer) {
		guiTexture = new GuiTexture(texture, position, scale);
		originalScale = scale;
		this.renderer = renderer;
	}
	/***
	 * Checks for collision of mouse and button etc.
	 */
	public void update(){
		if(!isHidden) {
			Vector2f location = guiTexture.getPosition();
			Vector2f scale = guiTexture.getScale();
			Vector2f mouseCoordinates = new Vector2f((2.0f * Mouse.getX()) / Display.getWidth() - 1f, (2.0f * Mouse.getY()) / Display.getHeight() - 1f);
			if(location.y + scale.y > mouseCoordinates.y 
					&& location.y - scale.y < mouseCoordinates.y 
					&& location.x + scale.x > mouseCoordinates.x 
					&& location.x - scale.x < mouseCoordinates.x) {
				whileHovering(this);
				if(!isHovering) {
					isHovering = true;
					onStartHover(this);
				}
				while(Mouse.next()){
					if(Mouse.isButtonDown(0)){
						if(wasUnpressed){
							onClick(this);
							wasUnpressed = false;
						}
					}else{
						wasUnpressed = true;
					}
				}
			}else{
				if(isHovering) {
					isHovering = false;
					onStopHover(this);
				}
			}
		}
	}
	
	public void show() {
		if(isHidden) {
			wasUnpressed = false;
			renderer.addRenderTexture(guiTexture);
			isHidden = false;
		}
	}
	
	public void hide() {
		if(!isHidden) {
			isHidden = true;
			if(!renderer.removeRenderTexture(guiTexture)){
				logger.debug("Unable to remove texture!");
			}
		}else{
			logger.debug("Unable to hide!");
		}
	}
	
	public void resetScale() {
		guiTexture.setScale(originalScale);
	}
	
	public void playHoverAnimation(float scaleFactor) {
		guiTexture.setScale(new Vector2f(originalScale.x + scaleFactor, originalScale.y + scaleFactor));
	}
	
	public boolean isHidden() {
		return isHidden;
	}
}
