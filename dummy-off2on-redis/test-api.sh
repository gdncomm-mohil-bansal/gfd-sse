#!/bin/bash

# Test script for dummy-off2on-redis service
# This script tests the OTP and SSE functionality

BASE_URL="http://localhost:8081"
USER_ID="test-user-$(date +%s)"

echo "================================"
echo "Dummy Off2On Redis - API Tests"
echo "================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Function to print info
print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

echo "Test User ID: $USER_ID"
echo ""

# Test 1: Health Check
echo "Test 1: Health Check"
response=$(curl -s "$BASE_URL/api/health")
if echo "$response" | grep -q '"status":"UP"'; then
    print_success "Health check passed"
    echo "Response: $response"
else
    print_error "Health check failed"
    echo "Response: $response"
    exit 1
fi
echo ""

# Test 2: Welcome Endpoint
echo "Test 2: Welcome Endpoint"
response=$(curl -s "$BASE_URL/api/")
if echo "$response" | grep -q '"message"'; then
    print_success "Welcome endpoint working"
    echo "Response: $response"
else
    print_error "Welcome endpoint failed"
    echo "Response: $response"
fi
echo ""

# Test 3: Generate OTP
echo "Test 3: Generate OTP"
response=$(curl -s -X POST "$BASE_URL/api/otp/generate" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"deviceInfo\":\"Test Script\"}")

echo "Response: $response"

if echo "$response" | grep -q '"success":true'; then
    print_success "OTP generated successfully"
    
    # Extract OTP from response
    OTP=$(echo "$response" | grep -o '"otp":"[^"]*' | cut -d'"' -f4)
    print_info "Generated OTP: $OTP"
else
    print_error "OTP generation failed"
    exit 1
fi
echo ""

# Test 4: Validate OTP
echo "Test 4: Validate OTP"
response=$(curl -s -X POST "$BASE_URL/api/otp/validate" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"otp\":\"$OTP\"}")

echo "Response: $response"

if echo "$response" | grep -q '"valid":true'; then
    print_success "OTP validation passed"
else
    print_error "OTP validation failed"
    exit 1
fi
echo ""

# Test 5: Validate Invalid OTP
echo "Test 5: Validate Invalid OTP (should fail)"
response=$(curl -s -X POST "$BASE_URL/api/otp/validate" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"otp\":\"000000\"}")

echo "Response: $response"

if echo "$response" | grep -q '"valid":false'; then
    print_success "Invalid OTP correctly rejected"
else
    print_error "Invalid OTP was incorrectly accepted"
fi
echo ""

# Test 6: Check Connection Status (should be not connected)
echo "Test 6: Check Connection Status (before connection)"
response=$(curl -s "$BASE_URL/api/sse/status/$USER_ID")
echo "Response: $response"

if echo "$response" | grep -q "Not connected"; then
    print_success "Connection status correct (not connected)"
else
    print_error "Connection status incorrect"
fi
echo ""

# Test 7: Get Active Connection Count
echo "Test 7: Get Active Connection Count"
response=$(curl -s "$BASE_URL/api/sse/connections/count")
echo "Active Connections: $response"
print_success "Retrieved active connection count"
echo ""

# Test 8: SSE Connection (Note: This will hang, so we'll timeout after 5 seconds)
echo "Test 8: Test SSE Connection (will run for 5 seconds to check for events)"
print_info "Connecting to SSE endpoint..."
print_info "You should see: CONNECTION_ESTABLISHED event and heartbeat messages"
print_info ""

timeout 5s curl -N -H "Accept: text/event-stream" \
    "$BASE_URL/api/sse/connect?userId=$USER_ID&otp=$OTP" 2>/dev/null

echo ""
echo ""
print_success "SSE connection test completed"
echo ""

# Test 9: Check Connection Status (should be connected or recently disconnected)
echo "Test 9: Check Connection Status (after connection)"
response=$(curl -s "$BASE_URL/api/sse/status/$USER_ID")
echo "Response: $response"
print_info "Connection may be disconnected if timeout occurred"
echo ""

# Test 10: Try to reuse OTP (should fail)
echo "Test 10: Try to Reuse OTP (should fail as OTP is one-time use)"
response=$(curl -s -X POST "$BASE_URL/api/otp/validate" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"otp\":\"$OTP\"}")

echo "Response: $response"

if echo "$response" | grep -q '"valid":false'; then
    print_success "OTP correctly invalidated after use"
else
    print_error "OTP was not invalidated (security issue)"
fi
echo ""

echo "================================"
echo "All Tests Completed!"
echo "================================"
echo ""
print_info "To test end-to-end with events:"
print_info "1. Keep dummy-off2on-redis SSE connection open in one terminal:"
print_info "   ./test-sse-connection.sh"
print_info ""
print_info "2. In another terminal, trigger events from dummy-off2on:"
print_info "   curl -X POST http://localhost:8080/api/cart/add \\"
print_info "     -H 'Content-Type: application/json' \\"
print_info "     -d '{\"userId\":\"test-user\",\"productId\":1,\"quantity\":2}'"
print_info ""
print_info "3. Watch events appear in the SSE connection terminal!"

