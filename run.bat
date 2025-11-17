@echo off
echo Compiling Memory Game...
echo.

REM Set paths
set JAVAFX_LIB=lib\javafx-sdk-25.0.1\lib
set MYSQL_CONNECTOR=lib\mysql-connector-j-9.5.0.jar
set SRC_DIR=src
set OUT_DIR=out

REM Create output directory
if not exist %OUT_DIR% mkdir %OUT_DIR%

REM Compile Java files
javac --module-path %JAVAFX_LIB% --add-modules javafx.controls,javafx.fxml,javafx.media -d %OUT_DIR% -cp "%MYSQL_CONNECTOR%" %SRC_DIR%\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Compilation successful!
echo.
echo Running Memory Game...
echo.

REM Run the application
java --module-path %JAVAFX_LIB% --add-modules javafx.controls,javafx.fxml,javafx.media -cp "%OUT_DIR%;%MYSQL_CONNECTOR%" MemoryGameApp

pause

