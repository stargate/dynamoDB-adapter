# Cassandra DynamoDB Adapter

This project is a DynamoDB adapter for [Stargate](https://stargate.io/), a data API for Apache Cassandra.
With this adapter as well as Stargate core components, you can run DynamoDB workloads against Apache Cassandra with
almost no change to your application code. In other words, your existing application code can read and write to Apache
Cassandra with an illusion that it is interacting with Amazon DynamoDB.

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
