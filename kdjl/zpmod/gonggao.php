<?php
//die();
session_start();
if(isset($_GET['para']))
{
	if($_SESSION['need_gonggao'] == 1)
	{
		
		$_SESSION['need_gonggao'] = 0;
		//$need_id = array(1,0,-2,-3,-5,-6,4,3);
		$need_id = array(-6,-5,-4,-3,-2,-1,0,1,2,3,4,5);
		$id = $_GET['para'];
		if( in_array($id,$need_id) )
		{
			require_once('../kernel/socketmsg.v1.php');
			require_once('../socketChat/config.chat.php');
			require_once('config_prize.php');
			
			$s=new socketmsg();
			$word = '恭喜玩家 '.$_SESSION['nickname']." 通过摩天轮,获得物品:".$prize_info[$id]."1个!";
			$word = iconv('gbk','utf-8',$word);
			$s->sendMsg('an|' . $word);
		}
	}
}

?>
