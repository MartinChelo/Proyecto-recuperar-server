@echo off
where gradle >nul 2>nul
if errorlevel 1 (
  echo Gradle was not found on PATH.
  echo Open this folder in IntelliJ IDEA as a Gradle project, or install Gradle 8.x and Java 21.
  pause
  exit /b 1
)
gradle build
if errorlevel 1 exit /b 1
echo Build complete. See build\libs\mobstatstweaks-1.0.0.jar
pause
