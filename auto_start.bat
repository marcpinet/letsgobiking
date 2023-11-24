@echo off

NET SESSION >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo This script requires elevated privileges. Please run as an administrator.
    pause
    exit
)

cd /d %~dp0

start cmd /k "activemq start"

cd Servers
nuget restore LetsGoBikingServer.sln
msbuild /p:Configuration=Release /p:TargetFrameworkVersion=v4.8

start "Caching Server" .\CachingServer\bin\Release\CachingServer.exe
start "Proxy Server" .\ProxyServer\bin\Release\ProxyServer.exe
start "Routing Server" .\RoutingServer\bin\Release\RoutingServer.exe

cd ..

cd Client
call mvn clean install
call mvn compile
call mvn exec:java -Dexec.mainClass="com.polytech.mwsoc.Main"
cd ..

echo Done.
pause
