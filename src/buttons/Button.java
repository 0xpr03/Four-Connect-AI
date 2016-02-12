package buttons;

import java.util.List;

import guis.GuiTexture;

public interface Button {
	void onClick(Button button);
	
	void onStartHover(Button button);
	
	void onStopHover(Button button);
	
	void whileHovering(Button button);
	
	void show(List<GuiTexture> guiTextureList);
	
	void hide(List<GuiTexture> guiTextureList);
	
	void playHoverAnimation(float scaleFactor);
	
	void resetScale();
	
	void update();
}
