## Introduction
This project aims to develop a shopping list application that primarily prioritizes local functionality. The application needs to be designed to operate on the user's device, enabling local storage. Additionally, it must incorporate a cloud-based component that facilitates data sharing concurrently among users. 

## Architecture
![image](https://github.com/diogocosta876/Sistemas-Distribuidos-Shopping-Lists/assets/24635445/5efe9ad3-e8d0-4deb-853a-f3c91dc2bf3f)

### Client-server communication
- All requests use the Packet class, which is then serialized to JSON

- The state allows the receiving party to know what the body contains, therefore allowing the deserialization of the information (into shopping lists,  hash ring information, etc…)

- CRDT implementation for list syncing

### Hashring
- Distributed Architecture: It allows for the distribution of data across a set of nodes (such as servers or data centers), which can help in handling high loads and providing redundancy.

- Minimizing Reorganization: When a node is added or removed, only a small portion of the keys need to be remapped, which is much more efficient compared to traditional hash table implementations where a significant amount of keys might need remapping.

- Load Balancing: While not perfect, consistent hashing tends to spread load fairly evenly among the nodes in a cluster.

- Redirection of Requests: Subsequent requests that would have been handled by the failed node are now redirected to the node(s) that have taken over the failed node's data range.

- Data Replication: the data from the failed node will already be present on another node. The number of replicas is configurable in our project through a global constant.


## How to run

- Import the maven project into intelliJ

- Run MiddleManServer

- Run Client


## Sugestion

In order to run multiple clients at a time it's advisable to create these Run/Debug Configurations:

![Alt text](image.png)

Make sure to allow multiple instances for every one:

![Alt text](docs/readme_images/image.png)

Make sure to run the DB first and only then the Router in order for the hashring to be recognized

# Product demo / presentation
https://www.youtube.com/watch?v=Q1iKjNMX6qU&ab_channel=DiogoCosta
