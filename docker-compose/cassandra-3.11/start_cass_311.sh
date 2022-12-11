#!/bin/sh

# Default to INFO as root log level
LOGLEVEL=INFO
SGTAG=v2
PROJTAG=v1.0.0

while getopts "lqr:t:" opt; do
  case $opt in
    l)
      PROJTAG="v$(../../mvnw -f ../.. help:evaluate -Dexpression=project.version -q -DforceStdout)"
      ;;
    q)
      REQUESTLOG=true
      ;;
    r)
      LOGLEVEL=$OPTARG
      ;;
    t)
      PROJTAG=$OPTARG
      ;;
    \?)
      echo "Valid options:"
      echo "  -t <tag> - use Docker images tagged with specified cassandra-dynamodb-adapter version (will pull images from Docker Hub if needed)"
      echo "  -l - use Docker images from local build (see project README for build instructions)"
      echo "  -q - enable request logging for APIs in 'io.quarkus.http.access-log' (default: disabled)"
      echo "  -r - specify root log level for APIs (defaults to INFO); usually DEBUG, WARN or ERROR"
      exit 1
      ;;
  esac
done

export LOGLEVEL
export REQUESTLOG
export SGTAG
export PROJTAG

echo "Running DynamoDB adapter $PROJTAG with Stargate coordinator $SGTAG and Cassandra 3.11"

# Make sure cassandra-1, the seed node, is up before bringing up other nodes and stargate

docker-compose up -d cassandra-1

# Wait until the seed node is up before bringing up more nodes

(docker-compose logs -f cassandra-1 &) | grep -q "Created default superuser role"

# Bring up the 2nd C* node

docker-compose up -d cassandra-2
(docker-compose logs -f cassandra-2 &) | grep -q "Starting listening for CQL clients on"

# Bring up the 3rd C* node

docker-compose up -d cassandra-3
(docker-compose logs -f cassandra-3 &) | grep -q "Starting listening for CQL clients on"

# Bring up the stargate: Coordinator first, then APIs

docker-compose up -d coordinator
(docker-compose logs -f coordinator &) | grep -q "Finished starting bundles"
docker-compose up -d dynamoapi
