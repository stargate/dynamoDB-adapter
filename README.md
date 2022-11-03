# Cassandra DynamoDB Adapter

This project is a DynamoDB adapter for [Stargate](https://stargate.io/), a data API for Apache Cassandra.
With this adapter as well as Stargate core components, you can run DynamoDB workloads against Apache Cassandra with
almost no change to your application code. In other words, your existing application code can read and write to Apache
Cassandra with an illusion that it is interacting with Amazon DynamoDB.

## User guide

The following steps assumes you are running everything locally.

### Step 1: Launch Cassandra cluster

Before starting Stargate locally, you will need an instance of Apache Cassandra&reg;.
The easiest way to do this is with a Docker image (see [Cassandra docker images](https://hub.docker.com/_/cassandra)).

> **_NOTE:_** due to the way networking works with Docker for Mac, the Docker method only works on Linux.
> We recommend CCM (see below) for use with MacOS.

Docker: Start a Cassandra 4.0 instance:

```sh
docker run --name local-cassandra \
--net=host \
-e CASSANDRA_CLUSTER_NAME=stargate \
-d cassandra:4.0
```

Cassandra Cluster Manager: Start a Cassandra 4.0 instance ([link to ccm](https://github.com/riptano/ccm). Make sure 
your `JAVA_HOME` points to JDK 8. Note it's typically preferable to specify a patch version number such as `4.0.6`).

```sh
ccm create stargate -v 4.0.6 -n 1 -s -b
```

### Step 2: Launch Stargate coordinator

> **_NOTE:_**  Before starting Stargate on MacOS you'll need to add an additional loopback:

```sh
sudo ifconfig lo0 alias 127.0.0.2
```

```
docker run --name stargate -d stargateio/coordinator-4_0:v2.0.0 --cluster-name stargate --cluster-seed 127.0.0.1 --cluster-version 4.0 --listen 127.0.0.2 --simple-snitch
```

### Step 3: Launch cassandra-dynamoDB-adapter

```
docker run liboxuanhk/cassandra-dynamodb-adapter:v1.0.0-SNAPSHOT
```

## Development guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.
If you want to learn more about Quarkus, please visit its [website](https://quarkus.io/).

It's recommended that you install Quarkus CLI in order to have a better development experience.
See [CLI Tooling](https://quarkus.io/guides/cli-tooling) for more information.

Note that this project uses Java 17, please ensure that you have the target JDK installed on your system.

### Create a Docker image

You can create a Docker image named `liboxuanhk/cassandra-dynamodb-adapter` using:
```
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true
```

If you want to learn more about building container images, please consult [Container images](https://quarkus.io/guides/container-image).
