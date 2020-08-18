<?php
$_POST["remote_addr"] = $_SERVER['REMOTE_ADDR'];
$_POST["timestamp"] = date(DATE_ATOM);
$fp = fopen('data.txt', 'a');
fwrite($fp, json_encode($_POST)."\n");
fclose($fp);

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PATCH, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Origin, Content-Type, X-Auth-Token');

echo "ok";
?>