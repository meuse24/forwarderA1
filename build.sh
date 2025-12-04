#!/usr/bin/env bash
#
# Forwarder A1 - Build Script
# Ensures correct JAVA_HOME is set for Gradle compilation
#
# Usage:
#   ./build.sh                    # Compile Debug Kotlin
#   ./build.sh assembleDebug      # Build Debug APK
#   ./build.sh assembleRelease    # Build Release APK
#   ./build.sh installDebug       # Install Debug APK to device
#   ./build.sh clean              # Clean build
#   ./build.sh test               # Run tests
#   ./build.sh [any gradle task]  # Run any Gradle task
#

set -e  # Exit on error

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Forwarder A1 Build Script ===${NC}"
echo ""

# Detect environment and set JAVA_HOME
if [ -n "$JAVA_HOME" ]; then
    echo -e "${YELLOW}Current JAVA_HOME: $JAVA_HOME${NC}"
fi

# Android Studio JDK path (works in Git Bash on Windows)
ANDROID_STUDIO_JDK="C:\Program Files\Android\Android Studio\jbr"

if [ -d "$ANDROID_STUDIO_JDK" ]; then
    export JAVA_HOME="$ANDROID_STUDIO_JDK"
    echo -e "${GREEN}✓ Using Android Studio JDK: $JAVA_HOME${NC}"
elif [ -x "/c/Program Files/Android/Android Studio/jbr/bin/java.exe" ]; then
    export JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
    echo -e "${GREEN}✓ Using Android Studio JDK (Git Bash): $JAVA_HOME${NC}"
else
    echo -e "${RED}✗ ERROR: Android Studio JDK not found!${NC}"
    echo -e "${YELLOW}Expected location: C:\Program Files\Android\Android Studio\jbr${NC}"
    exit 1
fi

# Default task is compileDebugKotlin if no arguments provided
GRADLE_TASK="${1:-compileDebugKotlin}"

echo ""
echo -e "${GREEN}Running: ./gradlew $GRADLE_TASK${NC}"
echo ""

# Run Gradle with all arguments
./gradlew "$@" || {
    echo ""
    echo -e "${RED}✗ Build failed!${NC}"
    exit 1
}

echo ""
echo -e "${GREEN}✓ Build successful!${NC}"
