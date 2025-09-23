#!/bin/bash

echo "Setting up ChitChat Backend Databases..."

# Set PostgreSQL password environment variable
export PGPASSWORD='8ivhaah8'

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

# Using remote MongoDB and PostgreSQL installations
print_status "Using remote MongoDB and PostgreSQL installations"

# Wait for databases to be ready
print_status "Waiting for databases to be ready..."
sleep 30

# Check PostgreSQL connection
print_status "Testing PostgreSQL connection..."
if psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
    print_status "PostgreSQL is ready!"
else
    print_warning "PostgreSQL might not be ready yet. Waiting a bit more..."
    sleep 10
    if psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "SELECT 1;" > /dev/null 2>&1; then
        print_status "PostgreSQL is ready!"
    else
        print_error "Failed to connect to PostgreSQL. Please check your connection to:"
        echo "  Host: ec2-65-1-185-194.ap-south-1.compute.amazonaws.com"
        echo "  Port: 5432"
        echo "  Database: chitchat"
        echo "  Username: summitcodeworks"
        exit 1
    fi
fi

# Check MongoDB connection
print_status "Testing MongoDB connection..."
if mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.runCommand('ping')" --quiet > /dev/null 2>&1; then
    print_status "MongoDB is ready!"
else
    print_warning "MongoDB might not be ready yet. Waiting a bit more..."
    sleep 10
    if mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.runCommand('ping')" --quiet > /dev/null 2>&1; then
        print_status "MongoDB is ready!"
    else
        print_error "Failed to connect to MongoDB. Check connection details and network access"
        echo "MongoDB Host: ec2-65-1-185-194.ap-south-1.compute.amazonaws.com"
        echo "MongoDB Port: 27017"
        echo "Username: summitcodeworks"
        exit 1
    fi
fi

# Verify PostgreSQL tables
print_status "Verifying PostgreSQL tables..."
TABLES=$(psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | tr -d ' ')
if [ "$TABLES" -gt 0 ]; then
    print_status "PostgreSQL tables created successfully! ($TABLES tables found)"
    psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "\dt"
else
    print_warning "No tables found in PostgreSQL. The initialization script might not have run."
fi

# Initialize MongoDB collections
print_status "Initializing MongoDB collections..."
if [ -f "database/init-mongodb.js" ]; then
    mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --file database/init-mongodb.js
    if [ $? -eq 0 ]; then
        print_status "MongoDB initialization script executed successfully!"
    else
        print_warning "MongoDB initialization script completed with warnings (collections may already exist)."
        print_status "Continuing with normal procedure..."
    fi
else
    print_warning "MongoDB initialization script not found at database/init-mongodb.js"
fi

# Verify MongoDB collections
print_status "Verifying MongoDB collections..."
COLLECTIONS=$(mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.getCollectionNames().length" --quiet | tr -d ' ')
if [ "$COLLECTIONS" -gt 0 ]; then
    print_status "MongoDB collections created successfully! ($COLLECTIONS collections found)"
    mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.getCollectionNames()"
else
    print_warning "No collections found in MongoDB. The initialization script might not have run."
fi

# Check admin user
print_status "Checking default admin user..."
ADMIN_COUNT=$(psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -t -c "SELECT COUNT(*) FROM admin_users WHERE username = 'admin';" | tr -d ' ')
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
echo "  Host: ec2-65-1-185-194.ap-south-1.compute.amazonaws.com"
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
