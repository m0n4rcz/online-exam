#!/bin/bash
echo "==================================================="
echo "  COMPILING AND STARTING C TCP SOCKET SERVER (LINUX)"
echo "==================================================="

cd server_c || exit 1
make clean
make

if [ $? -ne 0 ]; then
    echo "Compilation failed."
    exit 1
fi

echo "Starting C Server on Linux..."
./server 8888 ../exam_system.db
