@echo off
title Online Exam System - Client GUI
echo ===================================================
echo   STARTING ONLINE EXAM CLIENT GUI APPLICATION
echo ===================================================

if not exist "bin" mkdir bin

echo Starting ClientApp GUI...
java -cp "bin;lib/*" client.ui.ClientApp
pause
