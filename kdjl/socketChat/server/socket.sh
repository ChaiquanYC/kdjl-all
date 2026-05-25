#!/bin/sh

ID=`ps aux |grep snb.php | awk "{print \$2}"` >/dev/null 2>&1
for id in $ID
do
kill -9 $id >/dev/null 2>&1
done
cd /www/wwwroot/kdjl/socketChat/server/
sleep 2
nohup php /www/wwwroot/kdjl/socketChat/server/snb.php 127.0.0.1 11211 2077 127.0.0.1 kdjl 0 >/dev/null 2>&1 &
exit 0;

#!/bin/sh
ID=`ps aux |grep snb.php | awk "{print \$2}"` >/dev/null 2>&1
for id in $ID
do
kill -9 $id >/dev/null 2>&1
done
cd /www/wwwroot/kdjl/socketChat/server/
sleep 2
nohup php /www/wwwroot/kdjl/socketChat/server/snb.php 127.0.0.1 11211 2077 127.0.0.1 kdjl 0 >/dev/null 2>&1 &
exit 0;

