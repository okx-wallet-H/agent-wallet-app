#!/bin/bash
# Agent Wallet — One-command deployment script
# Run on your cloud server after cloning the repo.

set -e

echo "=== Agent Wallet Deployment ==="

# 1. Check Docker
if ! command -v docker &> /dev/null; then
    echo "Installing Docker..."
    curl -fsSL https://get.docker.com | sh
fi

# 2. Build backend
echo "Building backend..."
cd backend
./gradlew buildFatJar

# 3. Start services
echo "Starting PostgreSQL + Redis..."
cd ..
docker-compose up -d postgres redis

# 4. Wait for DB
echo "Waiting for database..."
sleep 5

# 5. Set up .env if not exists
if [ ! -f .env ]; then
    echo "Creating .env from example..."
    cp .env.example .env
    echo "⚠️  Edit .env with your API keys before running!"
    exit 1
fi

# 6. Build and start backend
echo "Building backend Docker image..."
cd backend
docker build -t agent-wallet-backend -f Dockerfile .
docker run -d --name agent-wallet-api \
    --network host \
    --env-file ../.env \
    agent-wallet-backend

echo ""
echo "=== Deployment Complete ==="
echo "Backend: http://$(curl -s ifconfig.me):8080"
echo "Test: curl -X POST http://localhost:8080/api/chat -H 'Content-Type: application/json' -d '{\"message\":\"你好\"}'"
