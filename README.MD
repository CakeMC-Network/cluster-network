# CakeMC Cluster Project

The CakeMC Cluster Project is a high-performance, fully connected network topology with a ring-based packet transmitting system. It uses Netty as its underlying networking framework and provides a scalable, reliable system for distributed applications. Each node in the network can communicate with every other node using a ring-topology-based packet distribution mechanism.

## Table of Contents

1. [Overview](#overview)
2. [System Architecture](#system-architecture)
3. [Core Components](#core-components)
  - [AbstractNode](#abstractnode)
  - [NetworkClient & NetworkServer](#networkclient--networkserver)
  - [Packet System](#packet-system)
  - [RingTopologicalAlgorithm](#ringtopologicalalgorithm)
4. [Tick System](#tick-system)
5. [Getting Started](#getting-started)
6. [Usage](#usage)
7. [Node Communication](#node-communication)
8. [Testing](#testing)
9. [Contributing](#contributing)
10. [License](#license)

---

## Overview

The CakeMC Cluster Project is built for applications where multiple nodes need to communicate efficiently in a distributed environment. It is designed around a **fully connected network topology** in which nodes are connected to every other node. However, the **ring-based packet transmission system** allows nodes to send packets in a structured way, optimizing performance.

Netty powers the networking layer, ensuring low-latency, non-blocking I/O. The system also provides robust packet handling, dynamic node management, and a scheduling system for periodic tasks (via the tick system).

## System Architecture

The system follows a layered architecture with several key components:

- **Nodes**: Each node is a part of the cluster. Nodes are capable of sending and receiving packets and performing various tasks via the tick system.

- **Network Topology**: All nodes are connected in a **fully connected** network, ensuring direct communication between any two nodes. However, packet distribution follows a **ring topology**, where packets are forwarded to neighbors in the ring until they reach the destination.

- **Packet System**: The core of the communication is the packet-based system, which defines how nodes transmit and respond to messages.

- **Connection Management**: Each node maintains multiple client-server connections, ensuring communication reliability.

- **Tick System**: A mechanism for scheduling regular updates to ensure each node performs tasks like connection checks, packet forwarding, and state updates.

## Core Components

### AbstractNode

The `AbstractNode` class is the backbone of the system and provides the following functionality:

- **Packet Dispatching**: The node can send both ring packets and request packets across the network.

- **Node Management**: Keeps track of the node’s own information as well as the connections to other nodes.

- **Connection Manager**: Handles active connections between nodes and manages data transmission.

- **Packet Registry**: Each node registers the packet types it supports and knows how to serialize/deserialize.

### NetworkClient & NetworkServer

- **NetworkClient**: Represents a client node that establishes outgoing connections to other nodes in the cluster. It is responsible for:
  - Sending packets.
  - Receiving and processing packets.
  - Handling the connection lifecycle.

- **NetworkServer**: A server that listens for incoming connections from other nodes in the network. It maintains open connections with clients and forwards packets to the appropriate handler.

### Packet System

Packets are the primary way nodes communicate. The `Packet` class defines the structure of the data sent between nodes. The system provides several types of packets:

- **RingPacket**: Sent around the ring and forwarded by nodes until it reaches its destination.
- **RequestPacket**: A packet that expects a response from a target node.
- **ResponsePacket**: The reply packet to a `RequestPacket`.

Each packet type implements serialization methods to ensure that data is properly encoded and decoded for transmission.

### RingTopologicalAlgorithm

The `RingTopologicalAlgorithm` implements the core logic of how packets are transmitted through the ring. Even though the network is fully connected, the ring topology ensures that packets are forwarded through neighboring nodes in a structured manner, optimizing network traffic and reducing complexity.

Key features:
- **Efficient Packet Routing**: Packets are forwarded in a circular manner to reach the destination node.
- **Load Balancing**: Distributes packets across the ring to prevent bottlenecks.

## Tick System

The `TickThread` and `TickAble` classes form the tick system, which schedules periodic updates for nodes. This system allows nodes to perform tasks at regular intervals, such as:

- Checking the status of connections.
- Forwarding packets.
- Executing custom logic at each tick.

The tick rate is set at **50 milliseconds**, ensuring a balance between responsiveness and performance.

## Getting Started

Follow these steps to set up the CakeMC Cluster Project:

### 1. Clone the repository:

```bash
git clone https://github.com/CakeMC-Network/cakemc-cluster.git
cd cakemc-cluster
```

### 2. Build the project:

Ensure you have a Java environment and build tool (Maven or Gradle) installed. Use Maven as an example:
```bash
mvn clean install
```

### 3. Run the cluster:
You can start multiple nodes using the provided `NodeTest` class in the `test` package.
````java
NodeAddress address = new NodeAddress(1, "127.0.0.1", 30001);
ClusterNode node = new ClusterNode(address, otherNodeInformationList, "cluster-key");
node.start();
````

## Usage

Each node starts by initializing the following components:

- Node Manager: Manages other nodes in the network, handling connection health and status.
- Connection Manager: Responsible for creating and maintaining the connections between nodes.
- Packet Registry: Handles the registration of all packet types used in communication.
- Network Client/Server: Manages network connections between nodes.

The cluster is initialized with a list of NodeInformation objects, each representing a node in the network. For example:
```java
List<NodeInformation> nodes = List.of(
  new NodeInformation(new NodeAddress(1, "127.0.0.1", 30001), NodeStatus.INACTIVE),
  new NodeInformation(new NodeAddress(2, "127.0.0.1", 30002), NodeStatus.INACTIVE)
);
ClusterNode clusterNode = new ClusterNode(new NodeAddress(1, "127.0.0.1", 30001), nodes, "secret-key");
clusterNode.start();
```

## Node Communication

### Packet Dispatch
Nodes can dispatch packets to the ring or send direct requests and handle replies:
```java
RequestPacket requestPacket = new RequestPacket(snowflake.nextId(), somePayload);
clusterNode.dispatchRequestToRing(requestPacket, response -> {
    // Handle the response
});
```
### Ring Packet Transmission
The system utilizes the ring-based packet transmission to efficiently route packets between nodes, even though the network is fully connected.

## Testing
To test the functionality of the cluster, the `NodeTest` class can be used. It simulates the start of multiple nodes and validates that all nodes are connected and can communicate.
```java
public static void main(String[] args) {
    NodeTest.startNode(new NodeAddress(1, "127.0.0.1", 30001));
    NodeTest.startNode(new NodeAddress(2, "127.0.0.1", 30002));
}
```
You can customize the number of nodes and their configurations for more extensive testing scenarios.

Contributing
Contributions to the CakeMC Cluster Project are welcome. To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bug fix.
3. Submit a pull request describing your changes.

## License

This project is licensed under the Apache License 2.0 License. See the `LICENSE` file for more details.