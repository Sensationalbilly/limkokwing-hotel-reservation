#!/usr/bin/env sh
# Builds and runs the app with plain javac/java (no Maven needed). macOS / Linux.
set -e
cd "$(dirname "$0")"

CONNECTOR_VERSION=9.1.0
CONNECTOR="lib/mysql-connector-j-$CONNECTOR_VERSION.jar"

if [ ! -f "$CONNECTOR" ]; then
    echo "Downloading MySQL Connector/J $CONNECTOR_VERSION..."
    mkdir -p lib
    curl -fL -o "$CONNECTOR" "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$CONNECTOR_VERSION/mysql-connector-j-$CONNECTOR_VERSION.jar"
fi

rm -rf build && mkdir build
echo "Compiling..."
javac -d build -encoding UTF-8 -sourcepath src/main/java src/main/java/com/hotel/Main.java

echo "Starting Hotel Reservation System..."
java -cp "build:src/main/resources:$CONNECTOR" com.hotel.Main
