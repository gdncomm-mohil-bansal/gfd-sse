#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
REPLICA_PORT=9082
BASE_URL="http://localhost:${REPLICA_PORT}/api/replica"
USER_ID="test-user-123"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  Replica Service Testing Script${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to print section headers
print_section() {
    echo ""
    echo -e "${YELLOW}$1${NC}"
    echo -e "${YELLOW}$(printf '=%.0s' {1..50})${NC}"
}

# Function to check if service is running
check_service() {
    print_section "Checking if Replica Service is Running..."
    
    if curl -s "http://localhost:${REPLICA_PORT}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Replica service is running on port ${REPLICA_PORT}${NC}"
        return 0
    else
        echo -e "${RED}✗ Replica service is NOT running on port ${REPLICA_PORT}${NC}"
        echo -e "${YELLOW}  Please start the service first:${NC}"
        echo -e "  cd dummy-off2on-redis-replica"
        echo -e "  ./mvnw spring-boot:run"
        return 1
    fi
}

# Function to test connection count
test_connection_count() {
    print_section "Test 1: Get Connection Count"
    
    echo "Request: GET ${BASE_URL}/connections/count"
    response=$(curl -s "${BASE_URL}/connections/count")
    echo "Response: $response"
    
    count=$(echo "$response" | grep -o '"data":[0-9]*' | grep -o '[0-9]*')
    echo -e "${GREEN}✓ Active connections on replica pod: ${count}${NC}"
}

# Function to test connection status
test_connection_status() {
    print_section "Test 2: Check User Connection Status"
    
    echo "Request: GET ${BASE_URL}/status/${USER_ID}"
    response=$(curl -s "${BASE_URL}/status/${USER_ID}")
    echo "Response: $response"
    
    if echo "$response" | grep -q "NOT connected"; then
        echo -e "${YELLOW}✓ User ${USER_ID} is NOT connected to this pod (expected)${NC}"
    else
        echo -e "${GREEN}✓ User ${USER_ID} IS connected to this pod${NC}"
    fi
}

# Function to simulate same-pod event
test_same_pod_event() {
    print_section "Test 3: Trigger Event (Same-Pod Scenario)"
    
    echo -e "${YELLOW}Note: This test will only work if user is connected to this pod${NC}"
    echo ""
    echo "Request: POST ${BASE_URL}/checkout/${USER_ID}"
    response=$(curl -s -X POST "${BASE_URL}/checkout/${USER_ID}")
    echo "Response: $response"
    
    if echo "$response" | grep -q '"success":true'; then
        echo -e "${GREEN}✓ Event sent successfully - User was connected to this pod${NC}"
    else
        echo -e "${YELLOW}✓ Event NOT sent - User is NOT connected to this pod (expected if no SSE connection)${NC}"
    fi
}

# Function to provide SSE connection instructions
provide_sse_instructions() {
    print_section "Test 4: SSE Connection (Manual Test Required)"
    
    echo -e "${YELLOW}To test SSE connection, open a new terminal and run:${NC}"
    echo ""
    echo -e "${BLUE}curl -N \"${BASE_URL}/connect?userId=${USER_ID}\"${NC}"
    echo ""
    echo -e "${YELLOW}You should see:${NC}"
    echo -e "  1. A CONNECTION_ESTABLISHED event"
    echo -e "  2. The connection stays open"
    echo ""
    echo -e "${YELLOW}Then, in another terminal, trigger an event:${NC}"
    echo ""
    echo -e "${BLUE}curl -X POST \"${BASE_URL}/checkout/${USER_ID}\"${NC}"
    echo ""
    echo -e "${YELLOW}You should see the CHECKOUT_INITIATED event in the SSE stream!${NC}"
}

# Function to test disconnect
test_disconnect() {
    print_section "Test 5: Disconnect User"
    
    echo "Request: POST ${BASE_URL}/disconnect/${USER_ID}"
    response=$(curl -s -X POST "${BASE_URL}/disconnect/${USER_ID}")
    echo "Response: $response"
    echo -e "${GREEN}✓ Disconnect request completed${NC}"
}

# Main test execution
main() {
    echo -e "${BLUE}Testing replica service on port ${REPLICA_PORT}${NC}"
    echo -e "${BLUE}User ID: ${USER_ID}${NC}"
    echo ""
    
    # Check if service is running
    if ! check_service; then
        exit 1
    fi
    
    # Run tests
    test_connection_count
    test_connection_status
    test_same_pod_event
    provide_sse_instructions
    test_disconnect
    
    # Summary
    print_section "Testing Complete"
    echo -e "${GREEN}✓ All API tests completed${NC}"
    echo ""
    echo -e "${YELLOW}For comprehensive cross-pod testing, see:${NC}"
    echo -e "  CROSS_POD_TESTING_GUIDE.md"
    echo ""
}

# Run main function
main

