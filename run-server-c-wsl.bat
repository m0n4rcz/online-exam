@echo off
title HUST Online Exam Server (C / Linux Ubuntu WSL)
echo ===================================================
echo   COMPILING AND STARTING C SERVER INSIDE UBUNTU WSL
echo ===================================================

wsl -d Ubuntu bash -c "cd /mnt/d/online-exam-project/server_c && make clean && make && ./server 8888 ../exam_system.db"
pause
