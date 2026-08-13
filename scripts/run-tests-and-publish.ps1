#Requires -Version 5.1
<#
.SYNOPSIS
  Backend + Frontend の全テストを実行し、Web 確認用レポートを frontend/public/test-reports へ出力する。
.EXAMPLE
  .\scripts\run-tests-and-publish.ps1
#>
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "==> Backend tests + JaCoCo + publish" -ForegroundColor Cyan
Push-Location (Join-Path $Root "backend")
try {
  if (Test-Path ".\gradlew.bat") {
    & .\gradlew.bat test jacocoTestReport publishTestReports --console=plain
  } else {
    throw "backend/gradlew.bat not found"
  }
  if ($LASTEXITCODE -ne 0) { throw "Backend tests failed with exit $LASTEXITCODE" }
} finally {
  Pop-Location
}

Write-Host "==> Frontend Vitest (+ coverage)" -ForegroundColor Cyan
Push-Location (Join-Path $Root "frontend")
try {
  New-Item -ItemType Directory -Force -Path "public\test-reports\frontend\vitest" | Out-Null
  & npm run test:coverage
  if ($LASTEXITCODE -ne 0) { throw "Frontend tests failed with exit $LASTEXITCODE" }
} finally {
  Pop-Location
}

Write-Host "==> Generate manifest.json" -ForegroundColor Cyan
& node (Join-Path $Root "scripts\generate-report-manifest.js")

Write-Host ""
Write-Host "Done. Open http://localhost:3000/test-reports after npm run dev" -ForegroundColor Green
Write-Host "  - Backend Surefire: /test-reports/backend/surefire/index.html"
Write-Host "  - Backend JaCoCo:   /test-reports/backend/jacoco/index.html"
Write-Host "  - Frontend Vitest:  /test-reports/frontend/vitest/index.html"
