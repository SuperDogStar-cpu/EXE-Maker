@echo off
chcp 65001 >nul
cd /d "%~dp0"

set "JAVAC=javac"
set "JAVA=java"
rem Set this to your own JDK folder if javac is not in PATH
set "JDK=C:\Users\XG\tools\zulu21.52.203-ca-jdk21.0.12.1-win_x64"

if not exist "%JDK%\bin\javac.exe" if defined JAVA_HOME set "JDK=%JAVA_HOME%"
if exist "%JDK%\bin\javac.exe" (
    set "JAVAC=%JDK%\bin\javac.exe"
    set "JAVA=%JDK%\bin\java.exe"
)

where javac >nul 2>nul
if errorlevel 1 if not exist "%JDK%\bin\javac.exe" (
    echo JDK not found. Please edit the JDK path inside run.bat.
    pause
    exit /b 1
)

if not exist "out" mkdir "out"

"%JAVAC%" -encoding UTF-8 -d out -sourcepath src src\appcreator\AppCreator.java
if errorlevel 1 goto fail

start "" "%JAVA%" -cp out appcreator.AppCreator
exit /b 0

:fail
echo.
echo Build failed. Please check the error messages above.
pause
exit /b 1
