package gamelogic.AI;

import gamelogic.ControllerBase.E_PLAYER;

/**
 * The AI interface to be implemented by all AIs.
 * The AI has to use the GController's function calls
 * @author Aron Heinecke
 */
public interface AI {
	/**
	 * Event called when the AI's supposed to make it's move.
	 * @return false if all states where used
	 */
	public abstract boolean getMove();
	/**
	 * Event if the game is ended by loose/win/draw/cancel
	 */
	public abstract void gameEvent();
	/**
	 * Event call on system shutdown.
	 */
	public abstract void shutdown();
	/**
	 * Called is a new game is started
	 * @param player set to the user the AI is playing
	 */
	public abstract void start(E_PLAYER player);
	
	/**
	 * Called so the AI can calculate it's next step while the animation is still running.
	 */
	public abstract void preProcessing();
	
	/**
	 * Go back one in the move history and update this as the current-last move
	 */
	public abstract void goBackHistory();
}
