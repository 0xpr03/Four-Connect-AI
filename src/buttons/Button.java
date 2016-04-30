package buttons;

public interface Button {
	void onClick(Button button);
	
	void onStartHover(Button button);
	
	void onStopHover(Button button);
	
	void whileHovering(Button button);
	
	void show();
	
	void hide();
	
	void playHoverAnimation(float scaleFactor);
	
	void resetScale();
	
	void update();
}
