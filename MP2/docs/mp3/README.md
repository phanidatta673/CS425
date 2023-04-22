# UIUC425 MP3
- design:
    - Total ordering (Sequencer based)
    - Leader Election (Ring based)
    - Consistency level W = 4 and R = 1 (to tolerate 3 simultaneous failures)
    - Quorum size = ? 
    - Use TCP for the whole implementation

- Messages
    - Failure detection
        - `Ping`: every 100 ms, timeout 100ms
            > Re-replica if failure detected    
            - memberlist (bool array)
            - last_seen (long array)
        - `Pong`

    - File
        - `File`
            - fileName
            - fileSize
            - fileVersion

        - `Delete`
            - fileName 

        - `Get`
            - fileName 
            - requester

        - `FileRequest`
            - fileName
            - fileSize
            - fileVersion

    - Leader Election
        - `Election`
            - initiatorID
            - highestID

        - `Elected`
            - machineID
            - ringStorage

- Procedures
    - `put`
        1. client -> `File` -> leader
        2. client -> starts streaming file chunks -> leader
        3. leader randomly pick one alive machine and its 3 children 
        4. leader -> `File` -> 4 machines 
        5. leader -> starts streaming file chunks -> 4 machines
        6. when machine receives whole file: machine -> `ACK` -> leader
        7. when leader receives `ACK` from machine, update table
        8. when leader receives 4 / 2 + 1 = 3 acks, send ack to client

    - `get`
        1. client -> `Get` -> leader
        2. leader -> `FileRequest` -> one machine with latest file
        3. machine -> `File` -> leader
        4. machine -> starts streaming file chunks -> leader
        5. when leader receives whole file: leader -> `File` -> client and starts streaming

    - `delete`
        1. client -> `Delete` -> leader
        2. leader -> `Delete` -> machines with this file
        3. leader updates table when receiving acks from machines
        4. when leader receives (number of machines) / 2 + 1 acks, leader -> `ACK` -> client

    - `election`
        1. Machines who detected the leader failure -> `Election` -> one clockwise child
        2. Receivers of the machine keeps track of the highest initiator and the highest ID
        3. If machine receives `Election` and the initiator is lower than local highest initiator: omit
           Else:
            if highestID == self:
                machine -> `Elected` -> one clockwise child
            else:
                if machine ID greater than highestID: 
                    modify highestID in msg to self's ID
                forward `Election` with self's ID -> one clockwise child

    - `rereplica`:
        1. Machine detects failure -> memberlist will propogate to the leader using the membership protocol
        2. Once the leader realize a machine is down, start rereplica
        3. Get all the files of that dead node from other machines
        4. Random pick machine for each of these files to store
        5. leader -> `File` -> machine
        6. leader -> starts streaming file chunks -> machine
        7. machine -> `ACK` -> leader
        8. leader updates table


- Roles:
    - Node (listens at port 10000):
    - Leader (listens at port 10001):

- issues:
    - How should the coordinator decide which servers to store?
        - random
    - How many quorums?
