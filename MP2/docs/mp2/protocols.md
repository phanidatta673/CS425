# Protocols
- Ping-Ack
    - Sender:
        ```json
        {
            "type": PING,
            "timestamp": long,
            "memberlist": array of booleans,
            "lastSeens": array of longs
        }
        ```
        > 768 bytes
    - Receiver:
        ```json
        {
            "type": ACK
        }
        ```