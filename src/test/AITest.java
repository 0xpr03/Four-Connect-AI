package test;

import gamelogic.Controller.E_GAME_MODE;
import gamelogic.GController;

public class AITest {
	public static void main(String[] args){
		GController.initGame(E_GAME_MODE.KI_INTERNAL);
		GController.startGame();
	}
}
