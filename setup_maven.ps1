$mvnTarget = Join-Path -Path $PSScriptRoot -ChildPath ".mvn\apache-maven-3.9.6"
if (Test-Path $mvnTarget) {
    Write-Host "Maven already installed at $mvnTarget"
    exit 0
}

$zipTarget = Join-Path -Path $PSScriptRoot -ChildPath ".mvn\maven.zip"
New-Item -ItemType Directory -Path (Join-Path -Path $PSScriptRoot -ChildPath ".mvn") -Force | Out-Null

Write-Host "Downloading Apache Maven 3.9.6..."
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip" -OutFile $zipTarget

Write-Host "Extracting Apache Maven..."
Expand-Archive -Path $zipTarget -DestinationPath (Join-Path -Path $PSScriptRoot -ChildPath ".mvn") -Force
Remove-Item -Path $zipTarget -Force
Write-Host "Maven ready at $mvnTarget"
