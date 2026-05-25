<?php
/**
 * 后台管理
*/
require_once('../config/config.game.php');

?>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=uft-8" />
<title>
<?=$cmd['title']?>
</title>
<link href="adminCSS/adIndex.css" rel="stylesheet" type="text/css" />
<script language=javascript src='/javascript/prototype.js'></script>
<style>

</style>
<body background="images/bg<?=rand(1,8)?>.jpg" style="background-size: cover;">
<div class="body">
    <table>
        <tr>
            <td colspan="2" style="text-align:center;padding:25px 0px;">
                <span style="font-weight:bold;font-size:35px;color:#DC143C;letter-spacing:6px;">口袋精灵2 — 后台管理</span>
            </td>
        </tr>
        <tr>
            <td class="menu">
                    <a id="a1" onclick="selectItem(1)" style="color:red;" href='./addPropToUser.php' target="contWindow">给玩家派送道具</a></br>
                    <a id="a2" onclick="selectItem(2)" href='./selectProps.php' target="contWindow">查询道具信息</a></br>
                    <a id="a3" onclick="selectItem(3)" href='./selectPlayer.php' target="contWindow">查询玩家信息</a></br>
                    <a id="a4" onclick="selectItem(4)" href='./selectBB.php' target="contWindow">查询宠物信息</a></br>
                    <a id="a5" onclick="selectItem(5)" href='./selectMerge.php' target="contWindow">查询合成公示表</a></br>
            </td>
            <td class="contents">
                <iframe class="contIframe" frameborder="0" src="./addPropToUser.php" name="contWindow"></iframe>
            </td>
        </tr>
    </table>
</div>
</body>
</html>


<script>
function selectItem(idnum){
    for(var i=1;i<=5;i++){
        document.getElementById("a"+i).style.color = "#1E90FF";
    }
    document.getElementById("a"+idnum).style.color = "red";
}
</script>