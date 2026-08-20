@echo off
rem Launcher for jpm. Resolves the fat JAR next to this script (..\lib) and falls
rem back to the Maven build output (..\target) during development.
setlocal

set "JPM_DIR=%~dp0"
set "JPM_JAR=%JPM_DIR%..\lib\jpm.jar"
if not exist "%JPM_JAR%" set "JPM_JAR=%JPM_DIR%..\target\jpm.jar"

if not exist "%JPM_JAR%" (
  echo jpm: jpm.jar not found ^(looked in ..\lib and ..\target^)>&2
  exit /b 1
)

set "JPM_JAVA=java"
if defined JAVA_HOME set "JPM_JAVA=%JAVA_HOME%\bin\java.exe"

"%JPM_JAVA%" -jar "%JPM_JAR%" %*
exit /b %ERRORLEVEL%
