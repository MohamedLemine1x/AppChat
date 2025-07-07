@echo off
echo Compiling VibeApp...
javac -cp "lib/*;." -d bin src/Main.java
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Running VibeApp...
java -cp "lib/*;bin" Main
pause