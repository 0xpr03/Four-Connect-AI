package buttons;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.util.vector.Vector2f;

import guis.GuiTexture;
import renderEngine.Loader;
import toolbox.MousePicker;

public abstract class AbstractButton implements Button{
		
	private MousePicker picker;
	
	private GuiTexture guiTexture;
	
	private Vector2f OriginalScale;
	
	private boolean isHidden= false, isHovering = false;
	
	public AbstractButton(Loader loader, String texture, Vector2f position, Vector2f scale) {
		guiTexture = new GuiTexture(loader.loadTexture(texture), position, scale);
		OriginalScale = scale;
	}
	
	public void update(){
		if(!isHidden) {
			Vector2f location = guiTexture.getPosition();
			Vector2f scale = guiTexture.getScale();
			Vector2f mouseCoordinates = new Vector2f((2.0f * Mouse.getX()) / Display.getWidth() - 1f, (2.0f * Mouse.getY()) / Display.getHeight() - 1f);
			if(location.y + scale.y > -mouseCoordinates.y 
					&& location.y - scale.y < -mouseCoordinates.y 
					&& location.x + scale.x > mouseCoordinates.x 
					&& location.x - scale.x < mouseCoordinates.x) {
				whileHovering(this);
				if(!isHovering) {
					isHovering = true;
					onStartHover(this);
				}
				while(Mouse.next()) 
					if(Mouse.isButtonDown(0)) 
						onClick(this);
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
			guiTextureList.add(guiTexture);
			isHidden = false;
		}
	}
	
	public void hide(List<GuiTexture> guiTextureList) {
		if(!isHidden) {
			guiTextureList.remove(guiTexture);
			isHidden = true;
		}
	}
	
	public void resetScale() {
		guiTexture.setScale(OriginalScale);
	}
	
	public void playHoverAnimation(float scaleFactor) {
		guiTexture.setScale(new Vector2f(OriginalScale.x + scaleFactor, OriginalScale.y + scaleFactor));
	}
	
	public boolean isHidden() {
		return isHidden;
	}
}
