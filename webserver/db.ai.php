<?php

class dbException extends Exception
{
	// Redefine the exception so message isn't optional
	public function __construct($message, $code = 0, Exception $previous = null) {
		// some code

		// make sure everything is assigned properly
		parent::__construct($message, $code, $previous);
	}

	// custom string representation of object
	public function __toString() {
		return __CLASS__ . ": [{$this->code}]: {$this->message}\n";
	}

	public function customFunction() {
		echo "A custom function for this type of exception\n";
	}
}
/**
 * KBS AI DB class
 * @author Aron Heinecke
 */
class aiDB extends dbException {
	
	private $db;
	
	public function __construct(){
		require 'config.ai.db.inc.php';
		$_access = getConfig();
		$this->db = new db_mysqli ( $_access["host"], $_access["user"], $_access["pass"], $_access["db"] );
		$this->setTimeZone();
	}
	
	private function setTimeZone(){
		$this->db->query("SET @@session.time_zone='+00:00'");
	}
	
	/**
	 * Get array of moves and all their data
	 * @param unknown $x xmax 
	 * @param unknown $y ymax
	 * @param unknown $player_a is player a
	 * @param unknown $fid field id
	 * @return moves or null
	 * @throws dbException
	 */
	public function getMoves($x, $y,$player_a, $fid){
		$table = 'moves_'.$x.'_'.$y;
		$pla = $player_a === 'true' ? 1 : 0;
		if($query = $this->db->prepare( "SELECT `move`,`used`,`loose`,`draw`,`win` FROM `$table` WHERE `fid` = ? AND player_a = ?;" )){
			$query->bind_param('ii', $fid, $pla );
			$query->execute();
			$result = $query->get_result();
			if(!$result){
				throw new dbException( '500' );
			}
		
			if($result->num_rows < 1){
				$moves = null;
			}else{
				$moves = array();
				while ( $row = $result->fetch_assoc () ) {
					$move = array();
					$move['move'] = $row['move'];
					$move['player_a'] = $player_a;
					$move['used'] = $row['used'];
					$move['loose'] = $row['loose'];
					$move['draw'] = $row['draw'];
					$move['win'] = $row['win'];
					$moves[] = $move;
				}
			}
				
			$query->close();
		
			return $moves;
		
		}else{
			throw new dbException( $this->db->error );
		}
	}
	
	/**
	 * Retrieve field ID
	 * @param unknown $x x max
	 * @param unknown $y y max
	 * @param unknown $field field hex string
	 * @return field id or null
	 * @throws dbException
	 */
	public function getFieldID($x,$y,$field){
		$table = 'fields_'.$x.'_'.$y;
		if($query = $this->db->prepare( "SELECT fid FROM `$table` WHERE `field` = UNHEX(?);" )){
			$query->bind_param('s', $field );
			$query->execute();
			$result = $query->get_result();
			if(!$result){
				throw new dbException( $query->error );
			}
		
			if($result->num_rows != 1){
				$fid = null;
			}else{
				if($row = $result->fetch_assoc ()){
					$fid = $row['fid'];
				}
			}
		
			$query->close();
		
			return $fid;
		
		}else{
			throw new dbException( $this->db->error );
		}
	}
}

class db_mysqli extends mysqli {
	public function __construct($host, $user, $pass, $db) {
		parent::init();

		if (! parent::options ( MYSQLI_OPT_CONNECT_TIMEOUT, 5 )) {
			die ( 'Setting MYSQLI_OPT_CONNECT_TIMEOUT failed' );
		}

		if (! parent::real_connect ( $host, $user, $pass, $db )) {
			die ( 'Connect Error (' . mysqli_connect_errno () . ') ' . mysqli_connect_error () );
		}

		if (! parent::set_charset( "utf8" )) {
			die ( "Couldn't set character set.");
		}
	}
}