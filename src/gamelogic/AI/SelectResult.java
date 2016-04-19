package gamelogic.AI;

import java.util.ArrayList;
import java.util.List;

/**
 * Object containing the result of an move request
 * @author Aron Heinecke
 *
 */
public class SelectResult {
	private List<Move> wins = new ArrayList<Move>(5);
	private List<Move> draws = new ArrayList<Move>(5);
	private List<Move> looses = new ArrayList<Move>(5);
	private List<Move> unused;
	public SelectResult(int amount_unused){
		unused = new ArrayList<Move>(amount_unused);
	}
	
	public SelectResult(){
		this(5);
	}
	
	/**
	 * @return the unused
	 */
	public List<Move> getUnused() {
		return unused;
	}

	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addWin(Move e) {
		return wins.add(e);
	}
	
	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addLoose(Move e) {
		return looses.add(e);
	}
	
	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addUnused(Move e) {
		return unused.add(e);
	}
	
	/**
	 * Returns wether there are no elements in any list
	 * @return
	 */
	public boolean isEmpty(){
		return this.draws.isEmpty() && this.looses.isEmpty() && this.wins.isEmpty() && this.unused.isEmpty();
	}
	
	/**
	 * Add move and sort it automatically into the list
	 * @param e
	 */
	public void add(Move e){
		if(e.isLoose()){
			looses.add(e);
		}else if(e.isDraw()){
			draws.add(e);
		}else if(e.isWin()){
			wins.add(e);
		}else if(!e.isUsed()){
			unused.add(e);
		}
	}
	
	/**
	 * @param e
	 * @return
	 * @see java.util.List#add(java.lang.Object)
	 */
	public boolean addDraw(Move e) {
		return draws.add(e);
	}

	/**
	 * @return the wins
	 */
	public List<Move> getWins() {
		return wins;
	}

	/**
	 * @return the draws
	 */
	public List<Move> getDraws() {
		return draws;
	}

	/**
	 * @return the looses
	 */
	public List<Move> getLooses() {
		return looses;
	}
}
