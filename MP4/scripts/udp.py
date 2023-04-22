import socket
import json
import time

UDP_IP = "127.0.0.1"
UDP_PORT = 10001 
MESSAGE = {
    "type": "PING",
    "timestamp": int(time.time() * 1000),
    "memberlist": [
        True,
        False,
        True,
        True,
        True,
        True,
        True,
        True,
        True,
        True,
        True,
    ]
}


with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock: 
    sock.sendto(bytes(json.dumps(MESSAGE), 'utf-8'), (UDP_IP, UDP_PORT))
    
    data, addr = sock.recvfrom(1024) # buffer size is 1024 bytes
    print("received message: %s" % data)