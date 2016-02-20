package gamelogic.AI;

/**
 * Class representing a single move for a specified field
 * @author Aron Heinecke
 *
 */
public class Move {
	private int move;
	private boolean draw;
	private boolean loose;
	private boolean used;
	private byte[] field;
	private boolean win;
	private long fid;
	private boolean player_a;
	public Move(byte[] field,long fid, int move, boolean loose,boolean draw, boolean win, boolean used,boolean player_a) {
		this.field = field;
		this.move = move;
		this.draw = draw;
		this.loose = loose;
		this.used = used;
		this.player_a = player_a;
		this.fid = fid;
		this.win = win;
	}
	public Move(byte[] field,long fid, int move,boolean player_a) {
		this(field,fid, move, false,false, false, false,player_a);
	}
	
	@Override
	public String toString(){
		return bytesToHex(this.field)+" fid:"+this.fid+" pla:"+this.player_a+" m:"+this.move+" d:"+draw+" l:"+loose+" u:"+used;
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
	 * @return the fid
	 */
	public long getFID() {
		return fid;
	}
	
	/**
	 * @return the player_a
	 */
	public boolean isPlayer_a() {
		return player_a;
	}
	
	/**
	 * @return the draw
	 */
	public boolean isDraw() {
		return draw;
	}
	/**
	 * @param draw the draw to set
	 */
	public void setDraw(boolean draw) {
		this.draw = draw;
	}
	/**
	 * @return the used
	 */
	public boolean isUsed() {
		return used;
	}
	/**
	 * @param used the used to set
	 */
	public void setUsed(boolean used) {
		this.used = used;
	}	
	/**
	 * @return the loose
	 */
	public boolean isLoose() {
		return loose;
	}
	/**
	 * @param loose the loose to set
	 */
	public void setLoose(boolean loose) {
		this.loose = loose;
	}
	/**
	 * @return the move
	 */
	public int getMove() {
		return move;
	}
	/**
	 * @return the field
	 */
	public byte[] getField() {
		return field;
	}
	/**
	 * @return the win
	 */
	public synchronized boolean isWin() {
		return win;
	}
	/**
	 * @param win the win to set
	 */
	public synchronized void setWin(boolean win) {
		this.win = win;
	}
}
