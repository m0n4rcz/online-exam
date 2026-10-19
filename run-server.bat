@echo off
title Online Exam System - Server
echo ===================================================
echo   STARTING ONLINE EXAM TCP SOCKET SERVER (PORT 8888)
echo ===================================================

if not exist "bin" mkdir bin

echo Compiling Java source files...
javac -encoding UTF-8 -cp ".;lib/*" -d bin shared\*.java shared\dtos\*.java server\core\*.java server\db\*.java server\db\dao\*.java server\room\*.java client\*.java client\net\*.java client\ui\*.java client\ui\components\*.java client\ui\screens\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed with exit code %ERRORLEVEL%.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting ServerCore...
java -cp "bin;lib/*" server.core.ServerCore 8888
pause
