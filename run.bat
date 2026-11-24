@echo off
rem Builds and runs the app with plain javac/java (no Maven needed).
rem Downloads the MySQL JDBC driver into lib\ the first time.
setlocal
cd /d "%~dp0"

set CONNECTOR_VERSION=9.1.0
set CONNECTOR=lib\mysql-connector-j-%CONNECTOR_VERSION%.jar

if not exist "%CONNECTOR%" (
    echo Downloading MySQL Connector/J %CONNECTOR_VERSION%...
    if not exist lib mkdir lib
    curl -fL -o "%CONNECTOR%" "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/%CONNECTOR_VERSION%/mysql-connector-j-%CONNECTOR_VERSION%.jar"
    if errorlevel 1 (
        echo Download failed. Download the jar manually and place it at %CONNECTOR%
 
        exit /b 1
    )
)

if exist build rmdir /s /q build
mkdir build

echo Compiling...
javac -d build -encoding UTF-8 -sourcepath src\main\java src\main\java\com\hotel\Main.java
if errorlevel 1 (
    echo Compilation failed.
    exit /b 1
)

echo Starting Hotel Reservation System...
java -cp "build;src\main\resources;%CONNECTOR%" com.hotel.Main
