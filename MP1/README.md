# Mp1

Server-client structure to query log files from different remote machines.

reference: https://courses.engr.illinois.edu/cs425/fa2022/MP1.CS425.FA22.pdf

## Visuals
> TBD
Depending on what you are making, it can be a good idea to include screenshots or even a video (you'll frequently see GIFs rather than actual videos). Tools like ttygif can help, but check out Asciinema for a more sophisticated method.

## Prerequsites
- gradle
- java

## Install
```bash
git clone https://gitlab.engr.illinois.edu/poweiww2/mp1.git
cd mp1
git checkout <branch>
sudo chmod +x gradlew
bash scripts/install_on_vm.sh
```

## Usage
- Start the application:
    > ALWAYS RUN THE SERVER FIRST THEN THE CLIENT!
    - `server`:
    ```bash
    cd ${YOUR_PROJECT_ROOT}/server
    ../gradlew --console plain run
    ```
    - `client`: 
    ```bash
    cd ${YOUR_PROJECT_ROOT}/client
    ../gradlew --console plain run
    ```
- Querying from client:
    - `client` terminal:
        ```bash
        // Enter the grep command after seeing the prompt>
        Please enter the grep command: <Your command here>
        ```
        - Example grep commands:
            - `grep -r "uiuc" .`: Look for "uiuc" recursively
            - `grep -r -n "uiuc" . `: Same as previous but outpus line number as well 
        - You will see the output of the server in the `client` terminal
- Run unit tests:
    > We're using VM01 as client and VM02~06 for servers
    1. Setup:
        ```bash
        # On server
        cd ${YOUR_PROJECT_ROOT}/server
        cp -r testcases/ ~
        ```
    2. Remove extra logs according to each server
        - For server 02:
            ```bash
            rm ~/testcases/some.log ~/testcases/all.log
            ```
        - For server 03~05:
            ```bash
            rm ~/testcases/one.log ~/testcases/all.log
            ```
        - For server 06:
            ```bash
            rm ~/testcases/one.log ~/testcases/some.log
            ```
    3. Start servers from VM02~06
        ```bash
        # On server
        cd ${YOUR_PROJECT_ROOT}/server
        ../gradlew --console plain run
        ``` 
    4. Run the tests
        ```bash
        # On client
        cd ${YOUR_PROJECT_ROOT}/client
        ../gradlew test
        ```
        
## Program overview
- `Server`
> The machine with logs to be queried
    - wait for client to connect
    - after connect, receive a grep command
    - query using that parsed grep command
    - pass the output string back

- `Client`
> We have single and multithreaded versions, while the single-threaded client was finsished first
    - single-threaded
        - build n sockets to all servers 
        - stdin the grep command
        - iterate all sockets and send the grep command
        - iterate sockets and read the output
        - print
    - multi-threaded
        - stdin the grep command
        - build n threads, each:
            - has one socket to one server
            - send te grep command
            - wait for the output
            - store in a variable
        - gather the output and print

## Notes
- The benchmark time is in milliseconds