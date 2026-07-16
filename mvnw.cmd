@echo off
setlocal
set "BASE_DIR=%~dp0"
for /f "tokens=1,* delims==" %%A in ('findstr /b "distributionUrl=" "%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"') do set "DIST_URL=%%B"
for %%A in ("%DIST_URL%") do set "MAVEN_VERSION=%%~nA"
set "MAVEN_VERSION=%MAVEN_VERSION:-bin=%"
set "WRAPPER_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%WRAPPER_HOME%\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo Maven is not installed by this lightweight wrapper. Please run mvnw on a POSIX host or install Maven 3.9.16.
  exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
