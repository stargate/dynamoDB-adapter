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

echo "Running DynamoDB adapter $PROJTAG with Stargate coordinator $SGTAG and Cassandra 3.11 (developer mode)"

# Can start all containers in parallel since C* is running inside single Stargate coordinator
# Bring up the stargate: Coordinator first, then APIs

docker-compose -f docker-compose-dev-mode.yml up -d coordinator
(docker-compose logs -f coordinator &) | grep -q "Finished starting bundles"
docker-compose -f docker-compose-dev-mode.yml up -d dynamoapi
