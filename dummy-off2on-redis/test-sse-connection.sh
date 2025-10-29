#!/bin/bash

# Script to establish and maintain an SSE connection for testing
# This script will connect to SSE and display events in real-time

BASE_URL="http://localhost:8081"
USER_ID="${1:-test-user-$(date +%s)}"

echo "================================"
echo "SSE Connection Test"
echo "================================"
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${YELLOW}User ID: $USER_ID${NC}"
echo ""

# Step 1: Generate OTP
echo -e "${CYAN}Step 1: Generating OTP...${NC}"
response=$(curl -s -X POST "$BASE_URL/api/otp/generate" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"deviceInfo\":\"SSE Test Script\"}")

OTP=$(echo "$response" | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

if [ -z "$OTP" ]; then
    echo -e "${RED}Failed to generate OTP${NC}"
    echo "Response: $response"
    exit 1
fi

echo -e "${GREEN}✓ OTP Generated: $OTP${NC}"
echo ""

# Step 2: Connect to SSE
echo -e "${CYAN}Step 2: Connecting to SSE...${NC}"
echo -e "${YELLOW}Listening for events (Press Ctrl+C to stop)${NC}"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

curl -N -H "Accept: text/event-stream" \
    "$BASE_URL/api/sse/connect?userId=$USER_ID&otp=$OTP"

echo ""
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${YELLOW}Connection closed${NC}"

