# Reports 

## General
1. Algorithm, Design
2. Why it scales to N
3. Protocol

## Explain
1. Why do crash of a node can be detected within 5 seconds of one of any machines in the ring
2. Why can the design talk to others even when 3 nodes are down
3. Why would the system fail if 4 nodes are down

## Logs
- Membership changes
- Failure detected

## MP1
- Mention how it was used to debug

## Measurements
- background bandwidth usage (in Bits/s, not messages/s) for N=6 machines (assuming no membership changes)
- Average bandwidth usage whenever a node joins, leaves or fails (3 different numbers) for N=6 machines
- Plot the false positive rate of your membership service when the message loss rate is 3%, 30% 
    - (you can emulate message losses at the sending end by dropping messages before sending them out to the network)
    - do this last part for a group with N=2, 6 machines (so, 4 total data points). 
    - On any plot, for each data point take at least as many readings as is necessary to get a non-zero false 
        positive rate (at least 5 readings each), and plot averages and standard deviations 
        and confidence intervals. Discuss your plots, don’t just put them on paper, i.e., 
        discuss trends, and whether they are what you expect or not