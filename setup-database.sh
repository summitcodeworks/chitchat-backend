#!/bin/bash

echo "Setting up ChitChat Backend Databases..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Check if database directory exists
if [ ! -d "database" ]; then
    print_error "Database directory not found. Please run this script from the project root."
    exit 1
fi

print_status "Starting database containers..."

# Start PostgreSQL and MongoDB containers
docker-compose up -d postgres mongodb

# Wait for databases to be ready
print_status "Waiting for databases to be ready..."
sleep 30

# Check PostgreSQL connection
print_status "Testing PostgreSQL connection..."
if docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
    print_status "PostgreSQL is ready!"
else
    print_warning "PostgreSQL might not be ready yet. Waiting a bit more..."
    sleep 10
    if docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
        print_status "PostgreSQL is ready!"
    else
        print_error "Failed to connect to PostgreSQL. Please check the logs:"
        docker logs chitchat-postgres
        exit 1
    fi
fi

# Check MongoDB connection
print_status "Testing MongoDB connection..."
if docker exec chitchat-mongodb mongosh --eval "db.runCommand('ping')" > /dev/null 2>&1; then
    print_status "MongoDB is ready!"
else
    print_warning "MongoDB might not be ready yet. Waiting a bit more..."
    sleep 10
    if docker exec chitchat-mongodb mongosh --eval "db.runCommand('ping')" > /dev/null 2>&1; then
        print_status "MongoDB is ready!"
    else
        print_error "Failed to connect to MongoDB. Please check the logs:"
        docker logs chitchat-mongodb
        exit 1
    fi
fi

# Verify PostgreSQL tables
print_status "Verifying PostgreSQL tables..."
TABLES=$(docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
if [ "$TABLES" -gt 0 ]; then
    print_status "PostgreSQL tables created successfully! ($TABLES tables found)"
    docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "\dt"
else
    print_warning "No tables found in PostgreSQL. The initialization script might not have run."
fi

# Verify MongoDB collections
print_status "Verifying MongoDB collections..."
COLLECTIONS=$(docker exec chitchat-mongodb mongosh chitchat --eval "db.getCollectionNames().length" --quiet | tr -d ' ')
if [ "$COLLECTIONS" -gt 0 ]; then
    print_status "MongoDB collections created successfully! ($COLLECTIONS collections found)"
    docker exec chitchat-mongodb mongosh chitchat --eval "db.getCollectionNames()"
else
    print_warning "No collections found in MongoDB. The initialization script might not have run."
fi

# Check admin user
print_status "Checking default admin user..."
ADMIN_COUNT=$(docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM admin_users WHERE username = 'admin';" | tr -d ' ')
if [ "$ADMIN_COUNT" -gt 0 ]; then
    print_status "Default admin user created successfully!"
    print_warning "Default admin credentials: username='admin', password='admin123'"
    print_warning "Please change the default password in production!"
else
    print_warning "Default admin user not found."
fi

print_status "Database setup completed!"
echo ""
echo "Database Connection Details:"
echo "=========================="
echo "PostgreSQL:"
echo "  Host: localhost"
echo "  Port: 5432"
echo "  Database: chitchat"
echo "  Username: summitcodeworks"
echo "  Password: 8ivhaah8"
echo ""
echo "MongoDB:"
echo "  Host: localhost"
echo "  Port: 27017"
echo "  Database: chitchat"
echo "  Username: summitcodeworks"
echo "  Password: 8ivhaah8"
echo ""
echo "You can now start the application services using:"
echo "  ./start-services.sh"
echo ""
echo "To stop the databases:"
echo "  docker-compose down"
