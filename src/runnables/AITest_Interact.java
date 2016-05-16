package runnables;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import gamelogic.ControllerBase.E_GAME_MODE;
import gamelogic.ControllerBase.E_GAME_STATE;
import test.lib;
import gamelogic.GController;

/**
 * Interactive test Player vs AI of type KBS
 * (most verbose output)
 * @author Aron Heinecke
 *
 */
public class AITest_Interact {
	
	private static Logger logger = LogManager.getLogger();
	
	private static void init(){
		GController.init("localhost", 3306, "ai", "66z1ayi9vweIDdWa1n0Z", "ai");
		Level level_db = Level.TRACE;
		Level level_ai = Level.TRACE;
		Configurator.setLevel("DB", level_db);
		Configurator.setLevel("AI", level_ai);
		lib.registerExitFunction(logger);
	}
	
	public static void main(String[] args){
		init();
		GController.initGame(E_GAME_MODE.SINGLE_PLAYER,Level.INFO, 5,4);
		GController.startGame();
		logger.info("Gamemode: {}",GController.getGamemode());
		E_GAME_STATE state = E_GAME_STATE.NONE;
		while(gameRunning()){
			if(state != GController.getGameState()){
				logger.info("STATE: {}",GController.getGameState());
				state = GController.getGameState();
			}
			if(GController.getGameState() == E_GAME_STATE.PLAYER_A){
				logger.info("Possibilities: \n{}",GController.getPossibilities());
				logger.info("\n{}",GController.getprintedGameState());
				System.out.println("Please select a column, 0-" + (GController.getX_MAX()-1));
				String input;
				try{
					input = lib.readLine("");
					if(input.equals("exit")){
						System.exit(1);
					}
					int in = Integer.parseInt(input);
					logger.debug("Player using {}",in);
					if(in >= GController.getX_MAX() || in < 0){
						System.out.println("Not in range!");
					}else{
						if(!GController.insertStone(in)){
							System.out.println("Wrong input!");
						}
					}
				}catch(NumberFormatException e){
					System.out.println("Not a number!");
				} catch (IOException e1) {
					logger.error(e1);
				}
			}else if(GController.getGameState() == E_GAME_STATE.PLAYER_B){
				logger.info(GController.getprintedGameState());
				GController.moveAI_A();
			}
		}
		System.out.println(GController.getprintedGameState());
		if(GController.getGameState() == E_GAME_STATE.WIN_B){
			System.err.println("You loose!");
		}else if(GController.getGameState() == E_GAME_STATE.WIN_A){
			System.err.print("You win!");
		}else if(GController.getGameState() == E_GAME_STATE.DRAW){
			System.err.println("Draw !");
		}else{
			System.err.println("Unknown state! "+GController.getGameState());
		}
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns false if draw,win_a,win_b is the current gamestate
	 * true otherwise
	 * @return
	 */
	private static boolean gameRunning(){
		switch(GController.getGameState()){
		case DRAW:
		case WIN_A:
		case WIN_B:
		case RESTART:
			return false;
		default:
			return true;
		}
	}
	
}
