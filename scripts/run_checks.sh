#!/bin/bash
set -euo pipefail

echo "=== [1/4] Backend verify (Spotless, ArchUnit, Message Language, Unit & Integration Tests) ==="
mvn -f backend/pom.xml --batch-mode verify

echo "=== [2/4] Frontend unit tests ==="
npm --prefix frontend test

echo "=== [3/4] Frontend ESLint (FSD boundaries & code quality) ==="
npm --prefix frontend run lint

echo "=== [4/4] Frontend production build ==="
npm --prefix frontend run build

echo "ALL QROS QUALITY GATES PASSED!"
