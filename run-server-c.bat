@echo off
title HUST Online Exam Server (C / TCP Socket)
echo ===================================================
echo   COMPILING AND STARTING C TCP SOCKET SERVER (PORT 8888)
echo ===================================================

cd server_c

echo Compiling C source files with GCC...
gcc -O2 -Wall -Ilib -I. server.c codec.c db.c session.c room.c dispatcher.c sha256.c lib/cJSON.c lib/sqlite3.c -o server.exe -lws2_32

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed with exit code %ERRORLEVEL%.
    pause
    exit /b %ERRORLEVEL%
)

echo Starting C Server...
server.exe 8888 ../exam_system.db
pause
