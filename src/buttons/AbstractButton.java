package buttons;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

import guis.GuiTexture;
import renderEngine.Loader;

public abstract class AbstractButton implements Button{
	
	private final GuiTexture guiTexture;
	
	private final Vector2f originalScale;
	
	private boolean isHidden= true, isHovering = false, wasUnpressed = true;
	
	private Logger logger = LogManager.getLogger();
	
	public AbstractButton(Loader loader, String texture, Vector2f position, Vector2f scale) {
		guiTexture = new GuiTexture(loader.loadTexture(texture), position, scale);
		originalScale = scale;
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
	
	public void show(List<GuiTexture> guiTextureList) {
		if(isHidden) {
			wasUnpressed = false;
			guiTextureList.add(guiTexture);
			isHidden = false;
		}
	}
	
	public void hide(List<GuiTexture> guiTextureList) {
		if(!isHidden) {
			guiTextureList.remove(guiTexture);
			isHidden = true;
		}else{
			logger.error("Unable to hide!");
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
