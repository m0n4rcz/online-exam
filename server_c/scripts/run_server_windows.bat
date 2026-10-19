@echo off
cd /d "%~dp0.."
echo ===================================================
echo [START] Launching Online Exam System C Server (Windows)
echo ===================================================
if not exist server.exe (
    echo [INFO] Compiling server.exe using gcc...
    gcc -Wall -O2 -I. -Ilib server.c codec.c db.c session.c room.c dispatcher.c sha256.c lib/cJSON.c lib/sqlite3.c -o server.exe -lws2_32
)
server.exe
pause
