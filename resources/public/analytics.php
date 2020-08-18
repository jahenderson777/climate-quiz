<?php
$_POST["remote_addr"] = $_SERVER['REMOTE_ADDR'];
$_POST["timestamp"] = date(DATE_ATOM);
$fp = fopen('data.txt', 'a');
fwrite($fp, json_encode($_POST)."\n");
fclose($fp);

echo "ok";
?>