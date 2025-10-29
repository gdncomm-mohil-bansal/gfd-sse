#!/bin/bash

# Off2On Service API Test Script
# This script tests all the main endpoints of the Off2On service

BASE_URL="http://localhost:8080"
USER_ID="test-user-$(date +%s)"

echo "======================================"
echo "Off2On Service API Test Script"
echo "======================================"
echo "Base URL: $BASE_URL"
echo "Test User ID: $USER_ID"
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test header
print_test() {
    echo -e "${YELLOW}➤ $1${NC}"
}

# Function to print success
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# Function to print error
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# Test 1: Health Check - Products
print_test "Test 1: Products Health Check"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/products/health")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Products service is healthy"
else
    print_error "Products health check failed (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 2: Get All Products
print_test "Test 2: Get All Products"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/products")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    product_count=$(echo "$response" | head -n-1 | grep -o '"id"' | wc -l)
    print_success "Retrieved products (count: $product_count)"
else
    print_error "Failed to get products (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 3: Get Product by ID
print_test "Test 3: Get Product by ID (ID=1)"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/products/1")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Retrieved product with ID 1"
else
    print_error "Failed to get product (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 4: Health Check - Cart
print_test "Test 4: Cart Health Check"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/cart/health")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Cart service is healthy"
else
    print_error "Cart health check failed (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 5: Add Product to Cart (Laptop)
print_test "Test 5: Add Laptop to Cart"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/cart/add" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"productId\":1,\"quantity\":2}")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Added Laptop to cart (quantity: 2)"
else
    print_error "Failed to add product to cart (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 6: Add Another Product to Cart (Mouse)
print_test "Test 6: Add Wireless Mouse to Cart"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/cart/add" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"productId\":2,\"quantity\":1}")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    print_success "Added Wireless Mouse to cart (quantity: 1)"
else
    print_error "Failed to add product to cart (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 7: Get Cart
print_test "Test 7: Get Cart Contents"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/cart/$USER_ID")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    cart_items=$(echo "$response" | head -n-1 | grep -o '"productId"' | wc -l)
    print_success "Retrieved cart (items: $cart_items)"
else
    print_error "Failed to get cart (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 8: Checkout
print_test "Test 8: Checkout Cart"
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/cart/checkout" \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"$USER_ID\",\"paymentMethod\":\"credit_card\",\"shippingAddress\":\"123 Test St, Test City, TC 12345\"}")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    order_id=$(echo "$response" | head -n-1 | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
    print_success "Checkout successful (Order ID: $order_id)"
else
    print_error "Checkout failed (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 9: Verify Cart is Empty After Checkout
print_test "Test 9: Verify Cart is Empty After Checkout"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/cart/$USER_ID")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    cart_items=$(echo "$response" | head -n-1 | grep -o '"productId"' | wc -l)
    if [ "$cart_items" -eq 0 ]; then
        print_success "Cart is empty as expected"
    else
        print_error "Cart still has items after checkout"
        exit 1
    fi
else
    print_error "Failed to get cart (HTTP $http_code)"
    exit 1
fi
echo ""

# Test 10: Get Products by Category
print_test "Test 10: Get Products by Category (Electronics)"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/products/category/Electronics")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" = "200" ]; then
    product_count=$(echo "$response" | head -n-1 | grep -o '"id"' | wc -l)
    print_success "Retrieved Electronics products (count: $product_count)"
else
    print_error "Failed to get products by category (HTTP $http_code)"
    exit 1
fi
echo ""

echo "======================================"
echo -e "${GREEN}All tests passed successfully! ✓${NC}"
echo "======================================"
echo ""
echo "Note: Check Redis logs to see the published events:"
echo "  redis-cli"
echo "  SUBSCRIBE cart-events"

