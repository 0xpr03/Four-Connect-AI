<?php
require_once 'db.ai.php';

if (true){ //NUR FUER DEBUGGING!!! AM ENDE WIEDER RAUSMACHEN!!!
	$e = ini_set('display_errors',true);
	if(!$e === "1"){
		echo $e;
	}
	error_reporting(E_ALL);
}
handler();

function handler(){ // true = WRONG_INPUT
	header('Content-Type: application/json');
	$moves = null;
	$errors = false;
	if(isset($_REQUEST['field']) && isset($_REQUEST['player_a']) && isset($_REQUEST['x']) && isset($_REQUEST['y'])){
		$x = $_REQUEST['x'];
		settype($x, "int");
		$y = $_REQUEST['y'];
		settype($y, "int");
		$player_a = $_REQUEST['player_a'];
		
		$db = new aiDB();
		$fid = $db->getFieldID($x,$y,$_REQUEST['field']);
		if($fid != null){
			$moves = $db->getMoves($x, $y,$player_a, $fid);
		}else{
			$errors = 'unknown field';
		}
		
	}else{
		$errors = 'invalid input';
	}
	$out = array();
	$out['moves'] = $moves;
	$out['error'] = $errors;
	echo json_encode($out);
	return true;
}
