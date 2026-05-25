<?php
ini_set('display_errors',true);
error_reporting(E_ALL);
require_once('../config/config.game.php');
require_once('../sec/dblock_fun.php');

require_once('config_prize.php');


 //$str = '&roulette=error&rouletteStr=摩天轮已损坏，转不动了！试试礼包或砸蛋吧！';
 //echo iconv('gbk','utf-8',$str);
 //die();

$needYB = 10;
$resNeedYB = $_pm['mysql'] -> getOneRecord("SELECT * from welcome WHERE code = 'zpNeedYB'" );
if(is_array($resNeedYB)){
    if($resNeedYB["contents"]>0){
        $needYB = $resNeedYB["contents"];
    }
}

$res = $_pm['mysql'] -> getOneRecord(" SELECT yb from player WHERE id = '".$_SESSION['id']."' LIMIT 1" );
if(!is_array($res) || $res['yb'] < $needYB )
{
	$str = '&roulette=error&rouletteStr=您的元宝不足(一次需要消耗'.$needYB.'元宝)';
	echo iconv('gbk','utf-8',$str);
	unLockItem($id);
	die();
}

$a = getLock($_SESSION['id']);
if(!is_array($a))
{
	realseLock();
	unLockItem($id);
	die('服务器繁忙，请稍候再试！');
}

//检查背包空间
$user = $_pm['user']->getUserById($_SESSION['id']);
$bags = $_pm['user']->getUserBagById($_SESSION['id']);
$bagNum=0;
if(is_array($bags))
{
	foreach($bags as $x => $y)
	{
		if($y['sums']>0 and $y['zbing'] == 0) 
		{
			$bagNum++;		
		}
	}
}
$snum = $user['maxbag'] - $bagNum;
if($snum < 3)
{
	unLockItem($id);
	$str = '&roulette=error&rouletteStr=您的背包空间不足,请至少留3个位置';
	echo iconv('gbk','utf-8',$str);
	die();
}
if($bagNum >= $user['maxbag'])
{
	unLockItem($id);
	$str = '&roulette=error&rouletteStr=您的背包空间不足';
	echo iconv('gbk','utf-8',$str);
	die();
}

$sql = "UPDATE player SET yb = yb - ".$needYB." WHERE id = {$_SESSION['id']} AND yb >= ".$needYB;
$_pm['mysql']->query($sql);

//$prize_id = -2;
//计算奖品
$luck_num = rand(1,20000);
$db_welcome = unserialize($_pm['mem']->get('db_welcome'));
foreach($db_welcome as $key => $val)
{
	if($val['code'] == 'zp_prize_info')
	{
		$infomation = $val['contents'];
		break;
	}
}
$arr = explode(',',$infomation);
foreach($arr as $info)
{
	$mid = explode(':',$info);
	$size = explode('-',$mid[1]);
	if($luck_num >= $size[0] && $luck_num <= $size[1])
	{
		$prize_id = $mid[0];
		$result = $prize_info[$mid[0]];
		break;
	}
}
//发奖
$task = new task();
$task->saveGetPropsMore($prize_id_config[$prize_id],1);

$vip = $res['yb']-$needYB;
$str = "恭喜您获得:".$result." ×1(还剩元宝:".$vip.")";
$str = "&roulette=".$prize_id."&rouletteStr=".$str;
echo iconv('gbk','utf-8',$str);
//日志
$log = '使用'.$needYB.'元宝用于摩天轮，得到:'.$result."(还剩元宝:".$vip.")";
$_pm['mysql'] -> query("INSERT INTO gamelog (seller,buyer,ptime,pnote,vary) VALUES({$_SESSION['id']},{$_SESSION['id']},".time().",'$log',236)");
$_SESSION['need_gonggao'] = 1;
realseLock();
unLockItem($id);
?>
