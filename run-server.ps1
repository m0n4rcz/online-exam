Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  STARTING ONLINE EXAM TCP SOCKET SERVER (PORT 8888)" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Compiling source files..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Recurse -Filter *.java shared,server,client | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp ".;lib/*" -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful. Launching ServerCore..." -ForegroundColor Green
    java -cp "bin;lib/*" server.core.ServerCore 8888
} else {
    Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
