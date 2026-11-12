#!/bin/bash
set -e
echo "Checking Maven and compiling backend..."
mvn -f backend/pom.xml test-compile -DskipTests
echo "Backend test-compile SUCCESS"
