@echo off
cd /d "%~dp0..\tests"
echo ===================================================
echo [TEST] Running Socket Integration Test Suite (Python)
echo ===================================================
python test_socket.py
pause
