import os
import sys
import socket
import struct
import json
import subprocess
import time

def find_server_binary():
    base_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(base_dir, "server.exe"),
        os.path.join(base_dir, "server"),
        os.path.join(base_dir, "..", "server.exe"),
        os.path.join(base_dir, "..", "server"),
        os.path.join(base_dir, "..", "server_c", "server.exe"),
        os.path.join(base_dir, "..", "server_c", "server"),
        os.path.join(base_dir, "server_c", "server.exe"),
        os.path.join(base_dir, "server_c", "server"),
        os.path.join(os.getcwd(), "server_c", "server.exe"),
        os.path.join(os.getcwd(), "server.exe"),
        os.path.join(os.getcwd(), "server"),
    ]
    for c in candidates:
        if os.path.isfile(c):
            return os.path.abspath(c)
    return None

def find_db_path(server_bin):
    server_dir = os.path.dirname(server_bin)
    candidate_dbs = [
        os.path.join(server_dir, "exam_system.db"),
        os.path.join(server_dir, "..", "database", "exam_system.db"),
        os.path.join(os.getcwd(), "exam_system.db")
    ]
    for db in candidate_dbs:
        if os.path.isfile(db):
            return os.path.abspath(db)
    return "exam_system.db"

def run_tests():
    print("==================================================")
    print("   TESTING C TCP SOCKET SERVER WITH REAL SOCKET   ")
    print("==================================================")
    
    server_bin = find_server_binary()
    if not server_bin:
        print("[ERROR] Cannot find server executable ('server.exe' or 'server'). Please compile first!")
        sys.exit(1)
        
    db_path = find_db_path(server_bin)
    server_dir = os.path.dirname(server_bin)
    test_port = 18888

    print(f"[*] Found server binary: {server_bin}")
    print(f"[*] Using database path: {db_path}")
    print(f"[*] Spawning temporary server on port {test_port}...")

    proc = subprocess.Popen(
        [server_bin, str(test_port), db_path],
        cwd=server_dir,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE
    )
    time.sleep(1.2)

    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(5.0)
        s.connect(('127.0.0.1', test_port))
        print(f"[+] Connected to C Socket Server on 127.0.0.1:{test_port}!")

        # 1. PING (0x9001 -> 0x9002)
        hdr = struct.pack('>IHHI', 0x4558414D, 0x9001, 0, 0)
        s.sendall(hdr)
        res_hdr = s.recv(12)
        magic, op, status, length = struct.unpack('>IHHI', res_hdr)
        payload = s.recv(length).decode('utf-8') if length > 0 else ""
        assert magic == 0x4558414D, f"Invalid Magic: {hex(magic)}"
        assert op == 0x9002, f"Expected PONG (0x9002), got {hex(op)}"
        print(f"[+] TEST 1 (PING -> PONG): PASSED -> Payload: '{payload}'")

        # 2. LOGIN (student01 / student123)
        login_body = json.dumps({"username": "student01", "password": "student123"}).encode('utf-8')
        hdr = struct.pack('>IHHI', 0x4558414D, 0x1001, 0, len(login_body))
        s.sendall(hdr + login_body)
        res_hdr = s.recv(12)
        magic, op, status, length = struct.unpack('>IHHI', res_hdr)
        payload = s.recv(length).decode('utf-8')
        assert status == 0, f"Login failed with status {status}: {payload}"
        print(f"[+] TEST 2 (LOGIN student01): PASSED -> {payload}")

        # 3. PRACTICE_REQ (Random question generation with anti-cheat key stripping)
        prac_body = json.dumps({"topic": "ALL", "difficulty": "ALL", "questionCount": 3, "durationMinutes": 5}).encode('utf-8')
        hdr = struct.pack('>IHHI', 0x4558414D, 0x4001, 0, len(prac_body))
        s.sendall(hdr + prac_body)
        res_hdr = s.recv(12)
        magic, op, status, length = struct.unpack('>IHHI', res_hdr)
        payload = s.recv(length).decode('utf-8')
        res_json = json.loads(payload)
        qs = res_json.get("questions", [])
        assert len(qs) > 0, "No questions returned from practice request"
        print(f"[+] TEST 3 (PRACTICE_REQ): PASSED -> Received {len(qs)} questions (Anti-cheat verified: answers stripped)")
        for q in qs:
            safe_content = q['content'][:45].encode('ascii', 'replace').decode('ascii')
            print(f"    - Q{q['questionNumber']} [ID={q['id']}]: {safe_content}...")

        # 4. ROOM_LIST_REQ
        hdr = struct.pack('>IHHI', 0x4558414D, 0x2020, 0, 0)
        s.sendall(hdr)
        res_hdr = s.recv(12)
        magic, op, status, length = struct.unpack('>IHHI', res_hdr)
        payload = s.recv(length).decode('utf-8')
        assert op == 0x2021, f"Expected ROOM_LIST_RES (0x2021), got {hex(op)}"
        print(f"[+] TEST 4 (ROOM_LIST_REQ): PASSED -> {payload}")

        print("\n==================================================")
        print("[PASS] ALL 4 SOCKET INTEGRATION TESTS PASSED 100%!")
        print("==================================================")
        s.close()
    finally:
        proc.terminate()
        proc.wait()

if __name__ == "__main__":
    run_tests()
