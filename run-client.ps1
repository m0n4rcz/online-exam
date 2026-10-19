Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  STARTING ONLINE EXAM CLIENT GUI APPLICATION" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan

if (-not (Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

$javaFiles = Get-ChildItem -Recurse -Filter *.java shared,server,client | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -cp ".;lib/*" -d bin $javaFiles

if ($LASTEXITCODE -eq 0) {
    Write-Host "Launching ClientApp GUI..." -ForegroundColor Green
    java -cp "bin;lib/*" client.ui.ClientApp
} else {
    Write-Host "Compilation failed with exit code $LASTEXITCODE" -ForegroundColor Red
}
