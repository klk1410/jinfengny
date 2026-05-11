<#
.SYNOPSIS
  Optional git commit, Maven jars, npm builds, scp to server.
.USAGE
  Copy deploy\deploy.env.example to deploy\deploy.env, then from repo root:
    .\deploy\quick-deploy.ps1
  Options: -SkipCommit -CommitMsg "msg" -SkipFrontend -SkipBackend -GitPush
Requires: JDK+Maven, Node+npm, OpenSSH (ssh/scp).
#>
param(
  [switch]$SkipCommit,
  [string]$CommitMsg = "",
  [switch]$SkipFrontend,
  [switch]$SkipBackend,
  [switch]$GitPush
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $RepoRoot 'admin-backend\pom.xml'))) {
  throw "Run from repo root (parent of deploy/). RepoRoot=$RepoRoot"
}
Set-Location $RepoRoot

$EnvFile = Join-Path $PSScriptRoot 'deploy.env'
if (-not (Test-Path $EnvFile)) {
  Write-Host "Missing $EnvFile - copy deploy.env.example to deploy.env" -ForegroundColor Red
  exit 1
}

Get-Content $EnvFile | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
  $pair = $_ -split '=', 2
  if ($pair.Length -eq 2) {
    $k = $pair[0].Trim()
    $v = $pair[1].Trim()
    [Environment]::SetEnvironmentVariable($k, $v, "Process")
  }
}

$hostSpec = $env:DEPLOY_HOST
if (-not $hostSpec) { throw 'deploy.env: set DEPLOY_HOST (e.g. root@1.2.3.4)' }
$sshPort = if ($env:SSH_PORT) { $env:SSH_PORT } else { '22' }
$identity = $env:SSH_IDENTITY

$remoteJarAdmin = $env:REMOTE_JAR_ADMIN
$remoteJarApp = $env:REMOTE_JAR_APP
$remoteStaticAdmin = $env:REMOTE_STATIC_ADMIN
$remoteStaticApp = $env:REMOTE_STATIC_APP
$remoteCmd = $env:REMOTE_CMD

if (-not $SkipBackend) {
  if (-not $remoteJarAdmin -or -not $remoteJarApp) { throw 'REMOTE_JAR_ADMIN / REMOTE_JAR_APP not set' }
}
if (-not $SkipFrontend) {
  if (-not $remoteStaticAdmin -or -not $remoteStaticApp) { throw 'REMOTE_STATIC_* not set' }
}

function Invoke-Scp {
  param([string[]]$ScpArgs)
  $exe = 'scp'
  & $exe @ScpArgs
  if ($LASTEXITCODE -ne 0) { throw "scp failed: $exe $($ScpArgs -join ' ')" }
}

function Build-ScpBaseArgs {
  $scpOpts = @('-P', $sshPort, '-o', 'StrictHostKeyChecking=accept-new')
  if ($identity) { $scpOpts += @('-i', $identity) }
  return $scpOpts
}

if (-not $SkipCommit) {
  git add -A
  $msg = if ($CommitMsg) { $CommitMsg } else { "chore: quick-deploy $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" }
  $dirty = git status --porcelain
  if ($dirty) {
    git commit -m $msg
    if ($LASTEXITCODE -ne 0) {
      Write-Host 'git commit failed (continuing).' -ForegroundColor Red
    } else {
      Write-Host "Committed: $msg" -ForegroundColor Green
    }
  } else {
    Write-Host 'Nothing to commit.' -ForegroundColor Yellow
  }
}

if (-not $SkipBackend) {
  Write-Host '>>> Maven admin-backend' -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot 'admin-backend')
  mvn -q package -DskipTests
  Pop-Location
  Write-Host '>>> Maven app-backend' -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot 'app-backend')
  mvn -q package -DskipTests
  Pop-Location
}

$adminDist = Join-Path $RepoRoot 'admin-frontend\dist-admin'
$appDist = Join-Path $RepoRoot 'app-frontend\dist-app'
if (-not $SkipFrontend) {
  Write-Host '>>> npm admin-frontend' -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot 'admin-frontend')
  npm install --cache .npm-cache
  npm run build
  Pop-Location
  Write-Host '>>> npm app-frontend' -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot 'app-frontend')
  npm install --cache .npm-cache
  npm run build
  Pop-Location
}

$base = Build-ScpBaseArgs
if (-not $SkipBackend) {
  $adminJar = Join-Path $RepoRoot 'admin-backend\target\admin-backend.jar'
  $appJar = Join-Path $RepoRoot 'app-backend\target\app-backend.jar'
  if (-not (Test-Path $adminJar)) { throw "Missing $adminJar" }
  if (-not (Test-Path $appJar)) { throw "Missing $appJar" }
  Write-Host '>>> mkdir remote jar dirs' -ForegroundColor Cyan
  $dirAdmin = Split-Path $remoteJarAdmin -Parent
  $dirApp = Split-Path $remoteJarApp -Parent
  $sshPrep = @('-p', $sshPort, '-o', 'StrictHostKeyChecking=accept-new')
  if ($identity) { $sshPrep += @('-i', $identity) }
  ssh @sshPrep $hostSpec "mkdir -p '$dirAdmin' '$dirApp'"
  Write-Host '>>> scp jars' -ForegroundColor Cyan
  Invoke-Scp ($base + @($adminJar, "${hostSpec}:$remoteJarAdmin"))
  Invoke-Scp ($base + @($appJar, "${hostSpec}:$remoteJarApp"))
}

if (-not $SkipFrontend) {
  if (-not (Test-Path $adminDist)) { throw "Missing $adminDist" }
  if (-not (Test-Path $appDist)) { throw "Missing $appDist" }
  Write-Host '>>> mkdir remote static dirs' -ForegroundColor Cyan
  $sshMk = @('-p', $sshPort, '-o', 'StrictHostKeyChecking=accept-new')
  if ($identity) { $sshMk += @('-i', $identity) }
  $sshMk += @($hostSpec, "mkdir -p '$remoteStaticAdmin' '$remoteStaticApp'")
  ssh @sshMk
  if ($LASTEXITCODE -ne 0) { throw 'ssh mkdir failed' }

  Write-Host '>>> scp static (recursive)' -ForegroundColor Cyan
  Invoke-Scp ($base + @('-r', "${adminDist}/.", "${hostSpec}:$remoteStaticAdmin/"))
  Invoke-Scp ($base + @('-r', "${appDist}/.", "${hostSpec}:$remoteStaticApp/"))
}

if ($remoteCmd) {
  Write-Host '>>> ssh remote command' -ForegroundColor Cyan
  $sshArgs = @('-p', $sshPort, '-o', 'StrictHostKeyChecking=accept-new')
  if ($identity) { $sshArgs += @('-i', $identity) }
  $sshArgs += @($hostSpec, $remoteCmd)
  ssh @sshArgs
  if ($LASTEXITCODE -ne 0) { throw 'remote command failed' }
}

if ($GitPush) {
  Write-Host '>>> git push' -ForegroundColor Cyan
  git push
  if ($LASTEXITCODE -ne 0) { Write-Host 'git push failed (ignore or retry).' -ForegroundColor Yellow }
}

Write-Host 'Done.' -ForegroundColor Green
