#!/bin/bash
echo "==========================================="
echo "Running Login Test via Maven"
echo "==========================================="
echo ""

mvn clean compile exec:java -Dexec.mainClass="com.skilora.TestLogin" -Dexec.classpathScope=compile
