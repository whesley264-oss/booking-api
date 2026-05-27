#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}   Booking API Setup Script${NC}"
echo -e "${GREEN}=======================================${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found. Please install JDK 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
echo -e "${GREEN}Java version: ${JAVA_VERSION}${NC}"

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo -e "${YELLOW}Maven not found. Using Maven wrapper...${NC}"
    MVN="./mvnw"
    if [ ! -f "$MVN" ]; then
        echo -e "${YELLOW}Downloading Maven wrapper...${NC}"
        curl -fsSL https://raw.githubusercontent.com/takari/maven-wrapper/master/mvnw -o mvnw
        chmod +x mvnw
    fi
else
    MVN="mvn"
fi

# Build
echo -e "\n${GREEN}Building project...${NC}"
$MVN clean package -DskipTests

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Build successful!${NC}"
else
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi

# Run
echo -e "\n${GREEN}Starting application...${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}\n"

$MVN spring-boot:run
