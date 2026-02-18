@REM Maven Wrapper for Windows
@REM Downloads and runs Maven automatically

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar"

if not exist "%MAVEN_PROJECTBASEDIR%.mvn\wrapper" mkdir "%MAVEN_PROJECTBASEDIR%.mvn\wrapper"

if not exist %MAVEN_WRAPPER_JAR% (
    echo Downloading Maven Wrapper...
    powershell -Command "Invoke-WebRequest -Uri %WRAPPER_URL% -OutFile %MAVEN_WRAPPER_JAR%"
)

set MAVEN_HOME=
for /f "tokens=*" %%a in ('type "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties" ^| findstr "distributionUrl"') do set %%a
set MAVEN_DIST_URL=%distributionUrl%

set MAVEN_USER_HOME=%USERPROFILE%\.m2\wrapper
set MAVEN_DIST_DIR=%MAVEN_USER_HOME%\dists

for %%I in (%MAVEN_DIST_URL%) do set MAVEN_DIST_NAME=%%~nI
set MAVEN_HOME_DIR=%MAVEN_DIST_DIR%\%MAVEN_DIST_NAME%

if not exist "%MAVEN_HOME_DIR%\bin\mvn.cmd" (
    echo Downloading Maven distribution...
    set MAVEN_ZIP=%MAVEN_DIST_DIR%\%MAVEN_DIST_NAME%.zip
    if not exist "%MAVEN_DIST_DIR%" mkdir "%MAVEN_DIST_DIR%"
    powershell -Command "Invoke-WebRequest -Uri '%MAVEN_DIST_URL%' -OutFile '%MAVEN_DIST_DIR%\%MAVEN_DIST_NAME%.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_DIST_DIR%\%MAVEN_DIST_NAME%.zip' -DestinationPath '%MAVEN_DIST_DIR%' -Force"
)

"%MAVEN_HOME_DIR%\bin\mvn.cmd" %*
