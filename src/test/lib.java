package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.Logger;

import gamelogic.GController;

/**
 * library for tests
 * @author Aron Heinecke
 *
 */
public class lib {
	
	public static void registerExitFunction(Logger logger) {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("State: {}",GController.getGameState());
				GController.shutdown();
			}
		});
	}
	public static String readLine(String format, Object... args) throws IOException {
	    if (System.console() != null) {
	        return System.console().readLine(format, args);
	    }
	    System.out.print(String.format(format, args));
	    BufferedReader reader = new BufferedReader(new InputStreamReader(
	            System.in));
	    return reader.readLine();
	}
}
