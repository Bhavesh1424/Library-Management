$javaHome = [System.Environment]::GetEnvironmentVariable('JAVA_HOME', 'Process')
if (-not $javaHome -or -not (Test-Path "$javaHome\bin\java.exe")) {
    if (Test-Path "C:\Program Files\Java\jdk-21\bin\java.exe") {
        [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-21', 'Process')
    } elseif (Test-Path "C:\Program Files\Java\jdk-23\bin\java.exe") {
        [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Java\jdk-23', 'Process')
    }
}

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$mvnHome = Join-Path $scriptDir ".mvn\apache-maven-3.9.6"
$mvnCmd = Join-Path $mvnHome "bin\mvn.cmd"

if (Test-Path $mvnCmd) {
    & $mvnCmd $args
} else {
    Write-Error "Maven not found in $mvnHome."
    exit 1
}
