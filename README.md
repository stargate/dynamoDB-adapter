# Cassandra DynamoDB Adapter

This project is a DynamoDB adapter for [Stargate](https://stargate.io/), a data API for Apache Cassandra.
With this adapter as well as Stargate core components, you can run DynamoDB workloads against Apache Cassandra with
almost no change to your application code. In other words, your existing application code can read and write to Apache
Cassandra with an illusion that it is interacting with Amazon DynamoDB.

## User guide

The following steps assumes you are running everything locally.

### Step 1: Launch Cassandra + Stargate coordinator + DynamoDB adapter

We recommend using docker to launch the program. We provided you with a docker-compose script and a start script [here](./docker-compose).

### Step 2: Generate authentication token

Then you should be able to visit [Auth API: /v1/auth/token/generate](http://localhost:8081/swagger-ui/#/auth/createToken_1) to generate a token with
the following payload:

```json
{
  "key": "cassandra",
  "secret": "cassandra"
}
```

Alternatively, you can run the following command in your terminal:

```bash
curl -X 'POST' \
  'http://localhost:8081/v1/auth/token/generate' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "key": "cassandra",
  "secret": "cassandra"
}'
```

You will get a response that looks like this:

```json
{
  "authToken": "726a2b56-88e4-4ada-91b6-e9617044ad36"
}
```

Copy the generated token value (i.e. `726a2b56-88e4-4ada-91b6-e9617044ad36` in this example) since we will need this token to authenticate all our requests.

### Step 3: Create Keyspace

If you haven't done so, invoke [/v2/keyspace/create API](http://localhost:8082/swagger-ui/#/default/post_v2_keyspace_create) to create a keyspace.
Note that you need to first click on `Authorize` and enter the auth token generated just now. The generated keyspace has a fixed name, "dynamodb".

Alternatively, you can also run the following command in your terminal:

```bash
curl -X 'POST' \
  'http://localhost:8082/v2/keyspace/create' \
  -H 'accept: */*' \
  -H 'X-Cassandra-Token: <YOUR GENERATED TOKEN>' \
  -H 'content-type: application/json;charset=UTF-8 '
```

### Step 4: Add endpoint and auth token to DynamoDB client

You should set the `aws.accessKeyId` property to be your generated token, and `aws.secretKey` to any string. Below
is an example in Java.

```java
public AmazonDynamoDB getClient() {
    Properties props = System.getProperties();
    props.setProperty("aws.accessKeyId", "<YOUR GENERATED TOKEN>");
    props.setProperty("aws.secretKey", "any-string");
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration("http://localhost:8082/v2", "any-string");
    return AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).build();
}
```

### Optional: Use DynamoDB low-level API

You can also use DynamoDB low-level HTTP API. You can use [Swagger UI](http://localhost:8082/swagger-ui/) for experiments.

## Development guide

This project uses Quarkus, the Supersonic Subatomic Java Framework.
If you want to learn more about Quarkus, please visit its [website](https://quarkus.io/).

It's recommended that you install Quarkus CLI in order to have a better development experience.
See [CLI Tooling](https://quarkus.io/guides/cli-tooling) for more information.

Note that this project uses Java 17, please ensure that you have the target JDK installed on your system.

### Create a Docker image manually

You can create a Docker image named `liboxuanhk/cassandra-dynamodb-adapter` using:
```
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true
```

If you want to learn more about building container images, please consult [Container images](https://quarkus.io/guides/container-image).

### Release process

The release process is automated. Whenver a commit is tagged
with `v1.x.y`, a release will be triggered on GitHub Actions. Specifically,
a release workflow includes the following steps:

1. Tag the commit to be released. This is the only
   step done by the developer.
2. Build a container image.
3. Publish the container image to Docker Hub.
4. Create a GitHub release.
5. Create a Pull Request to bump the project version
   in maven.
