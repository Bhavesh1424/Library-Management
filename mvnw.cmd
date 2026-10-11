@echo off
setlocal

rem Validate JAVA_HOME or auto-heal to installed JDK 21 or 23
if not exist "%JAVA_HOME%\bin\java.exe" (
    if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    ) else if exist "C:\Program Files\Java\jdk-23\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-23"
    )
)

set "DIR=%~dp0"
set "MVN_HOME=%DIR%.mvn\apache-maven-3.9.6"
if exist "%MVN_HOME%\bin\mvn.cmd" (
    call "%MVN_HOME%\bin\mvn.cmd" %*
) else (
    echo Maven not found in %MVN_HOME%.
    exit /b 1
)
