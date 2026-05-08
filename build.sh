#!/bin/sh
set -e

echo "🔨 Building SunTray.app..."

# Clean previous build
./gradlew clean

# Build the .app bundle via jpackage
./gradlew jpackage

echo "✅ Build complete: build/jpackage/SunTray.app"
