@echo off
echo ===================================================
echo [START] Launching Online Exam System C Server in WSL (Ubuntu Linux)
echo ===================================================
wsl -e bash -c "cd $(wslpath '%~dp0..') && make && ./server"
pause
