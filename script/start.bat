@echo off
set "JAVA_OPTS= -Xmx1g "
set "DIR=%~dp0%"

echo Using Duser.dir: %DIR%
echo Using CLASSPATH: %DIR%lib
echo Using JAVA_OPTS: %JAVA_OPTS%
echo Using LOG  PATH: %DIR%logs

java -Duser.dir=%DIR% %JAVA_OPTS% -classpath %DIR%lib\simplepush1.0.jar cn.xunsci.simplepush.node.IMServer 2>&1 >> %DIR%logs\simplepush.out