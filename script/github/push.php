<?php

define('SECRET', ''); // Please use your secret here

$raw = @file_get_contents('php://input');

list($hash, $signature) = explode('=', $_SERVER['HTTP_X_HUB_SIGNATURE'], 2);

if (hash_hmac($hash, $raw, SECRET) === $signature) {
    $json = json_decode($raw, true);

    if (!$json) {
        die('JSON invalid');
    }

    if (!isset($json['ref'])) {
        die('No ref found');
    }

    $branch = str_replace('refs/heads/', '', $json['ref']);

    if (!in_array($branch, array('develop', 'master'))) {
        die("Branch unknown");
    }

    die(shell_exec("update-pssst $branch"));
}
