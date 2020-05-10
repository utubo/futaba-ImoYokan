<?php
// ------------------------------------
// 画像サムネイル作成中間サーバー
// ------------------------------------
// ディレクトリ構造
//   ./
//     thumb.php ..... 本体
//     img/ .......... 読み込み失敗時の代替サムネイル置き場
//     thumb/ ........ サムネイル置き場
//       .htaccess ... サムネが存在しないときはこのphpを実行するという設定
//     lock/ ......... ロックファイル置き場
// 必要なアプリ
//   convert ......... gifのサムネ作成に必要
//   ffmpeg .......... 動画のサムネ作成に必要
// cron設定
//   # 古いサムネや作業用の動画ファイルを削除
//   0 3 * * * find <このスクリプトがあるディレクトリ>/thumb/ -name 's*.*' -mtime 1 -exec rm -f {} \;

// 環境設定
define('PATH_TO_CONVERT', '/usr/local/bin/convert');
define('PATH_TO_FFMPEG',  '/home/utb/bin/ffmpeg');

// ここから本体
// ファイル名解析
$filename = basename($_GET['filename']);
if (! preg_match('/^(f|fu|sa|sp|sq|ss|su)\d+\.(png|jpg|jpeg|gif|mp4|webm|webp)\.thumb\.jpg$/', $filename)) {
	http_response_code(400);
	echo 'BAD FILENAME';
	return;
}
preg_match('/(\.\w+)\.thumb.jpg$/', $filename, $ext_match);
$ext =  $ext_match[1];
$is_video = (strpos('.webm.mp4.webp', $ext) !== false);
$cache_filename = 'thumb/' . $filename;
$lock_filename = $cache_filename . '.lock';
$lock = fopen($lock_filename, 'a');

try { // ロックファイルをクローズ＆削除するためのtry-finally

// ロック(5回までリトライ)
for ($sleep_count = 5; 0 <= $sleep_count; $sleep_count --) {
	if (file_exists($cache_filename)) {
		// ロック待ちの間にキャッシュが出来上がった場合
		header('Content-Type: image/jpg');
		readfile($cache_filename);
		return;
	}
	if ($sleep_count == 0) {
		http_response_code(503); // Server busy
		return;
	}
	if (5 <= count(glob('thumb/*.lock'))) {
		sleep(1);
		continue;
	}
	if (!flock($lock, LOCK_EX | LOCK_NB)) {
		sleep(1);
		continue;
	}
	break;
}

// キャッシュがない場合は塩から読み込んでサムネを作る
function startsWith($haystack, $needle) {
	$length = strlen($needle);
	return (substr($haystack, 0, $length) === $needle);
}

function createRandomId($prefix = '') {
	return  $prefix . substr(str_shuffle('0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'), 0 , 4);
}

$sio_root = '';
switch (true) {
case startsWith($filename, 'sa') : $sio_root = 'http://www.nijibox6.com/futabafiles/001/src/'; break;
case startsWith($filename, 'sp') : $sio_root = 'http://www.nijibox2.com/futabafiles/003/src/'; break;
case startsWith($filename, 'sq') : $sio_root = 'http://www.nijibox6.com/futabafiles/mid/src/'; break;
case startsWith($filename, 'ss') : $sio_root = 'http://www.nijibox5.com/futabafiles/kobin/src/'; break;
case startsWith($filename, 'su') : $sio_root = 'http://www.nijibox5.com/futabafiles/tubu/src/'; break;
case startsWith($filename, 'fu') : $sio_root = 'https://dec.2chan.net/up2/src/'; break;
case startsWith($filename, 'f')  : $sio_root = 'https://dec.2chan.net/up/src/'; break;
}

// ここに画像を読み開始
$img = new Imagick();
$range = $is_video ? '0-307200' : ''; // 動画は300Kbだけ読み込んでサムネを作る
for ($try_count = 0; $try_count < 2; $try_count ++) {

	// ダウンロードの準備
	$curl = curl_init();
	curl_setopt($curl, CURLOPT_URL, $sio_root . str_replace('.thumb.jpg', '', $filename));
	curl_setopt($curl, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($curl, CURLOPT_CUSTOMREQUEST, 'GET');
	curl_setopt($curl, CURLOPT_HEADER, 0);
	curl_setopt($curl, CURLOPT_BINARYTRANSFER, 1);
	curl_setopt($curl, CURLOPT_USERAGENT, 'Imoyokan/renrakusaki twitter @utbutb');
	$ip = $_SERVER['REMOTE_ADDR'];
	curl_setopt($curl, CURLOPT_HTTPHEADER, array("REMOTE_ADDR: $ip", "X_FORWARDED_FOR: $ip"));
	// 動画はでかいので初めの方だけ読み込んでサムネを作る
	if ($is_video) {
		curl_setopt($curl, CURLOPT_RANGE, $range);
	}

	// ダウンロード
	$download_data = curl_exec($curl);
	$status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
	curl_close($curl);
	if ($status != 200 && $status != 206) {
		http_response_code($status);
		return;
	}

	if ($is_video) {
		// 動画の場合はffmpegでサムネ作成
		// ここで作ったファイルはcronで消す
		$temp_id = createRandomId('.');
		$temp_mp4 = $cache_filename . $temp_id . $ext;
		$temp_jpg = $cache_filename . $temp_id . '.jpg';
		$fw = fopen($temp_mp4, 'w');
		fwrite($fw, $download_data);
		fclose($fw);
		shell_exec(PATH_TO_FFMPEG . " -i $temp_mp4 -vf thumbnail -frames:v 1 $temp_jpg");
		if (file_exists($temp_jpg)) {
			$img->readImage($temp_jpg);
			break; // 読み込み成功
		}
		if ($try_count == 0) {
			// 失敗したら3Mb読み込んで再挑戦
			$range = '0-3072000';
			continue;
		} else {
			// 3Mbでダメならあきらめる
			$img->readImage('img/' . substr($ext, 1) . '.jpg');
		}
	} elseif ($ext == '.gif') {
		// GIFはメモリ上でjpgにすると微妙なので一旦コマンドでjpgにする…そのうちやり方調べる
		$temp_id = createRandomId('.');
		$temp_gif = $cache_filename . $temp_id . $ext;
		$temp_jpg = $cache_filename . $temp_id . '.jpg';
		$fw = fopen($temp_gif, 'w');
		fwrite($fw, $download_data);
		fclose($fw);
		shell_exec(PATH_TO_CONVERT . " '${temp_gif}[0]' $temp_jpg");
		if (file_exists($temp_jpg)) {
			$img->readImage($temp_jpg);
		} else {
			// ファイルが壊れてると変換に失敗する
			// エラーログは…まぁいいか…
			$img->readImage('img/broken.jpg');
		}
	} else {
		// 画像ファイルはそのまま読み込めばOK
		try {
			$img->readImageBlob($download_data);
		} catch (Exception $e) {
			// 壊れた画像を何度も塩に取りにいかないようにする
			$img->readImage('img/broken.jpg');
			$msg = $e->getMessage() . "\n" . $e->getTraceAsString();
			file_put_contents($cache_filename . '.error.log', $msg, FILE_APPEND | LOCK_EX);
		}
	}
	// ここまでくれば読み込み成功
	break;

}
// 画像読み込みここまで

// リサイズして保存
$img->thumbnailImage(250, 250, true);
$img->setCompressionQuality(70);
// 書き込み中に別の人が読み込むとおかしくなるから一時ファイルに書き込んでからリネームするよ
$temp_filename = $cache_filename . 'temp.jpg';
$temp = fopen($temp_filename, 'wb');
$img->writeImagesFile($temp);
fclose($temp);
rename($temp_filename, $cache_filename);

// できた！
header('Content-Type: image/jpg');
echo $img;

} finally {
	fclose($lock);
	if (file_exists($lock_filename)) {
		unlink($lock_filename);
	}
}

?>

