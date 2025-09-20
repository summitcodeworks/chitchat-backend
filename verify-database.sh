#!/bin/bash

echo "Verifying ChitChat Backend Database Setup..."

# Set PostgreSQL password environment variable
export PGPASSWORD='8ivhaah8'

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

# PostgreSQL is now remote - no local container needed
print_status "Using remote PostgreSQL server: ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:5432"

# MongoDB is now remote - check if it's accessible
if mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.runCommand('ping')" --quiet > /dev/null 2>&1; then
    print_status "Remote MongoDB is accessible"
else
    print_error "Remote MongoDB is not accessible"
    echo "Check connection to: ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017"
    echo "Username: summitcodeworks"
    exit 1
fi

# Test PostgreSQL connection and tables
print_header "Verifying PostgreSQL Database..."

if psql -h ec2-13-233-106-55.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
    print_status "PostgreSQL connection successful"
    
    # Check tables
    TABLES=$(psql -h ec2-13-233-106-55.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
    print_status "Found $TABLES tables in PostgreSQL"
    
    # List all tables
    echo "PostgreSQL Tables:"
    psql -h ec2-13-233-106-55.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "\dt" | grep -E "^\s+\w+\s+\|\s+table"
    
    # Check admin user
    ADMIN_COUNT=$(psql -h ec2-13-233-106-55.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM admin_users WHERE username = 'admin';" | tr -d ' ')
    if [ "$ADMIN_COUNT" -gt 0 ]; then
        print_status "Default admin user exists"
    else
        print_warning "Default admin user not found"
    fi
    
else
    print_error "Failed to connect to PostgreSQL"
    echo "Check your connection to: ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:5432"
    exit 1
fi

# Test MongoDB connection and collections
print_header "Verifying MongoDB Database..."

if mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.runCommand('ping')" --quiet > /dev/null 2>&1; then
    print_status "MongoDB connection successful"
    
    # Check collections
    COLLECTIONS=$(mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.getCollectionNames().length" --quiet | tr -d ' ')
    print_status "Found $COLLECTIONS collections in MongoDB"
    
    # List all collections
    echo "MongoDB Collections:"
    mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.getCollectionNames()" --quiet
    
    # Check sample data
    SAMPLE_GROUPS=$(mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.groups.countDocuments()" --quiet | tr -d ' ')
    SAMPLE_STATUSES=$(mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.statuses.countDocuments()" --quiet | tr -d ' ')
    
    if [ "$SAMPLE_GROUPS" -gt 0 ]; then
        print_status "Sample group data found ($SAMPLE_GROUPS groups)"
    fi
    
    if [ "$SAMPLE_STATUSES" -gt 0 ]; then
        print_status "Sample status data found ($SAMPLE_STATUSES statuses)"
    fi
    
else
    print_error "Failed to connect to MongoDB"
    echo "Check connection to: ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017"
    echo "Username: summitcodeworks"
    exit 1
fi

# Test database indexes
print_header "Checking Database Indexes..."

echo "PostgreSQL Indexes:"
psql -h ec2-13-233-106-55.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "SELECT schemaname, tablename, indexname FROM pg_indexes WHERE schemaname = 'public' ORDER BY tablename, indexname;" | grep -E "^\s+\w+\s+\|\s+\w+\s+\|\s+\w+"

echo ""
echo "MongoDB Indexes:"
mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.messages.getIndexes().forEach(function(index) { print('messages.' + index.name); });" --quiet
mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.groups.getIndexes().forEach(function(index) { print('groups.' + index.name); });" --quiet
mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.statuses.getIndexes().forEach(function(index) { print('statuses.' + index.name); });" --quiet

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
