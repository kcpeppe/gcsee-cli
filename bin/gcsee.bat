@echo off
setlocal

rem gcsee.bat - launcher for gcsee-cli on Windows
rem Install layout:
rem   <install>\bin\gcsee.bat   <- this script
rem   <install>\lib\gcsee-cli.jar

set "BIN_DIR=%~dp0"
rem Strip trailing backslash
if "%BIN_DIR:~-1%"=="\" set "BIN_DIR=%BIN_DIR:~0,-1%"
for %%I in ("%BIN_DIR%\..") do set "APP_HOME=%%~fI"

if defined GCSEE_JAR (
    set "JAR=%GCSEE_JAR%"
) else (
    set "JAR=%APP_HOME%\lib\gcsee-cli.jar"
)

if not exist "%JAR%" (
    echo gcsee: cannot find jar at %JAR% 1>&2
    echo        set GCSEE_JAR to override. 1>&2
    exit /b 1
)

if defined JAVA_HOME (
    set "JAVA=%JAVA_HOME%\bin\java.exe"
) else (
    set "JAVA=java"
)

"%JAVA%" %GCSEE_OPTS% -jar "%JAR%" %*
exit /b %ERRORLEVEL%
