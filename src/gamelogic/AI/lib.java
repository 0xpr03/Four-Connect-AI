package gamelogic.AI;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import gamelogic.GController;
import gamelogic.ControllerBase.E_FIELD_STATE;

/**
 * Helper lib for the AI
 * @author Aron Heinecke
 *
 */
public class lib {
	private ByteBuffer bytebuffer = null;
	private IntBuffer intbuffer = null;
	private MessageDigest md;
	Logger logger = LogManager.getLogger();
	
	public lib(){
	    try {
	        md = MessageDigest.getInstance("SHA-1");
	    }catch(NoSuchAlgorithmException e) {
	    	logger.fatal("Can't initialize SHA-1 ! {}",e);
	    }
	}
	
	/**
	 * Bytes to hex for sha ASCII representation
	 * @param bytes
	 * @return
	 */
	public String bytesToHex(byte[] bytes) {
		StringBuffer result = new StringBuffer();
		for (byte byt : bytes)
			result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
		return result.toString();
	}
	
	/**
	 * Field to sha1
	 * @param field
	 * @return
	 */
	public byte[] field2sha(E_FIELD_STATE[][] field){
		return md.digest(field2bytes(field));
	}
	
	/**
	 * Convert E_FIELD_STATE[][] to int[][]
	 * @param field
	 * @return
	 */
	private int[][] field2ints(E_FIELD_STATE[][] field){
		int[][] out = new int[GController.getX_MAX()][GController.getY_MAX()];
		for(int x = 0; x < GController.getX_MAX(); x++){
			for(int y = 0; y < GController.getY_MAX(); y++){
				out[x][y] = field[x][y].ordinal();
			}
		}
		return out;
	}
	
	/**
	 * Convert E_FIELD_STATE[][] to byte[]
	 * @param field
	 * @return
	 */
	public byte[] field2bytes(E_FIELD_STATE[][] field){
		int[][] data = field2ints(field);
		if(bytebuffer == null){
			bytebuffer = ByteBuffer.allocate(GController.getX_MAX() * GController.getY_MAX() * 4);
		}else{
			bytebuffer.clear();
		}
		if(intbuffer == null){
			intbuffer = bytebuffer.asIntBuffer();
		}else{
			intbuffer.clear();
		}
        for(int[] row : data){
        	intbuffer.put(row);
        }

        return bytebuffer.array();
	}
}
