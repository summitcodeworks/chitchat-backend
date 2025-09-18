#!/bin/bash

echo "Verifying ChitChat Backend Database Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
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

print_header() {
    echo -e "${BLUE}[HEADER]${NC} $1"
}

# Check if Docker containers are running
print_header "Checking Docker Containers..."

if docker ps | grep -q "chitchat-postgres"; then
    print_status "PostgreSQL container is running"
else
    print_error "PostgreSQL container is not running"
    echo "Start it with: docker-compose up -d postgres"
    exit 1
fi

if docker ps | grep -q "chitchat-mongodb"; then
    print_status "MongoDB container is running"
else
    print_error "MongoDB container is not running"
    echo "Start it with: docker-compose up -d mongodb"
    exit 1
fi

# Test PostgreSQL connection and tables
print_header "Verifying PostgreSQL Database..."

if docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
    print_status "PostgreSQL connection successful"
    
    # Check tables
    TABLES=$(docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
    print_status "Found $TABLES tables in PostgreSQL"
    
    # List all tables
    echo "PostgreSQL Tables:"
    docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "\dt" | grep -E "^\s+\w+\s+\|\s+table"
    
    # Check admin user
    ADMIN_COUNT=$(docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM admin_users WHERE username = 'admin';" | tr -d ' ')
    if [ "$ADMIN_COUNT" -gt 0 ]; then
        print_status "Default admin user exists"
    else
        print_warning "Default admin user not found"
    fi
    
else
    print_error "Failed to connect to PostgreSQL"
    echo "Check logs with: docker logs chitchat-postgres"
    exit 1
fi

# Test MongoDB connection and collections
print_header "Verifying MongoDB Database..."

if docker exec chitchat-mongodb mongosh --eval "db.runCommand('ping')" > /dev/null 2>&1; then
    print_status "MongoDB connection successful"
    
    # Check collections
    COLLECTIONS=$(docker exec chitchat-mongodb mongosh chirp --eval "db.getCollectionNames().length" --quiet | tr -d ' ')
    print_status "Found $COLLECTIONS collections in MongoDB"
    
    # List all collections
    echo "MongoDB Collections:"
    docker exec chitchat-mongodb mongosh chirp --eval "db.getCollectionNames()" --quiet
    
    # Check sample data
    SAMPLE_GROUPS=$(docker exec chitchat-mongodb mongosh chirp --eval "db.groups.countDocuments()" --quiet | tr -d ' ')
    SAMPLE_STATUSES=$(docker exec chitchat-mongodb mongosh chirp --eval "db.statuses.countDocuments()" --quiet | tr -d ' ')
    
    if [ "$SAMPLE_GROUPS" -gt 0 ]; then
        print_status "Sample group data found ($SAMPLE_GROUPS groups)"
    fi
    
    if [ "$SAMPLE_STATUSES" -gt 0 ]; then
        print_status "Sample status data found ($SAMPLE_STATUSES statuses)"
    fi
    
else
    print_error "Failed to connect to MongoDB"
    echo "Check logs with: docker logs chitchat-mongodb"
    exit 1
fi

# Test database indexes
print_header "Checking Database Indexes..."

echo "PostgreSQL Indexes:"
docker exec chitchat-postgres psql -U summitcodeworks -d chitchat -c "SELECT schemaname, tablename, indexname FROM pg_indexes WHERE schemaname = 'public' ORDER BY tablename, indexname;" | grep -E "^\s+\w+\s+\|\s+\w+\s+\|\s+\w+"

echo ""
echo "MongoDB Indexes:"
docker exec chitchat-mongodb mongosh chirp --eval "db.messages.getIndexes().forEach(function(index) { print('messages.' + index.name); });" --quiet
docker exec chitchat-mongodb mongosh chirp --eval "db.groups.getIndexes().forEach(function(index) { print('groups.' + index.name); });" --quiet
docker exec chitchat-mongodb mongosh chirp --eval "db.statuses.getIndexes().forEach(function(index) { print('statuses.' + index.name); });" --quiet

print_header "Database Verification Complete!"

echo ""
echo "Summary:"
echo "========"
echo "✅ PostgreSQL: Connected and $TABLES tables found"
echo "✅ MongoDB: Connected and $COLLECTIONS collections found"
echo "✅ Database initialization completed successfully"
echo ""
echo "You can now start the application services:"
echo "  ./start-services.sh"
