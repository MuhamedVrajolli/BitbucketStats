#!/bin/bash

# Function to clean up resources
cleanup() {
    echo "An error occurred. Cleaning up..."
    docker rm -f bitbucket-stats >/dev/null 2>&1 || true
    exit 1
}

# Trap any error signals (ERR) and call the cleanup function
trap 'cleanup' ERR

echo "Building App Image..."
docker build -f Dockerfile_app -t bitbucket-stats ../

# Start container
echo "Starting container"
docker run -d --name bitbucket-stats -p 8080:8080 bitbucket-stats

echo "Setup completed successfully."
