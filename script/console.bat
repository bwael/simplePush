@echo off
set "DIR=%~dp0%"

java -classpath %DIR%lib\simplepush1.0.jar cn.xunsci.simplepush.node.IMServerConsole %1