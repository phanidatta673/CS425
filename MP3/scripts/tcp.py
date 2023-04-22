import socket
import json
import time

HOST = "127.0.0.1"  
PORT = 10000 

js = {
    "hostNumber": 10,
    "port": PORT,
    "timestamp": int(time.time() * 1000)
}

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
    s.connect((HOST, PORT))
    # s.send(f"{json.dumps(js)}\r\n\r\n".encode())
    # data = s.recv(1024)

# print(f"Received {data!r}")