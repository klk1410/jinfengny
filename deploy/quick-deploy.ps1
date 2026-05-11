<#
.SYNOPSIS
  快速部署：可选 git 提交 → Maven 打 admin/app 后端 jar → 构建两个前端 → SCP 到服务器。

.USAGE
  1) Copy deploy\deploy.env.example to deploy\deploy.env and fill in.
  2) From repo root 环保油:
     .\deploy\quick-deploy.ps1
  可选:
     .\deploy\quick-deploy.ps1 -SkipCommit    # 跳过 git commit
     .\deploy\quick-deploy.ps1 -CommitMsg "fix: xxx"
     .\deploy\quick-deploy.ps1 -SkipFrontend # 仅 jar
     .\deploy\quick-deploy.ps1 -SkipBackend  # 仅静态
     .\deploy\quick-deploy.ps1 -GitPush       # 结束前推送到 origin（需网络）

Requires: JDK+Maven、Node+npm；本机已安装 OpenSSH 客户端（scp/ssh）。
#>
param(
  [switch]$SkipCommit,
  [string]$CommitMsg = "",
  [switch]$SkipFrontend,
  [switch]$SkipBackend,
  [switch]$GitPush
)

$ErrorActionPreference = "Stop"
# deploy\quick-deploy.ps1 → 仓库根目录为上一级
$RepoRoot = Split-Path $PSScriptRoot -Parent
if (-not (Test-Path (Join-Path $RepoRoot "admin-backend\pom.xml"))) {
  throw "无法在 $RepoRoot 定位 admin-backend\pom.xml，请在「环保油」仓库根目录调用本脚本。"
}
Set-Location $RepoRoot

$EnvFile = Join-Path $PSScriptRoot "deploy.env"
if (-not (Test-Path $EnvFile)) {
  Write-Host "缺少 $EnvFile，请复制 deploy.env.example 为 deploy.env 并填写。" -ForegroundColor Red
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
if (-not $hostSpec) { throw "deploy.env 需设置 DEPLOY_HOST（如 root@1.2.3.4）" }
$sshPort = if ($env:SSH_PORT) { $env:SSH_PORT } else { "22" }
$identity = $env:SSH_IDENTITY

$remoteJarAdmin = $env:REMOTE_JAR_ADMIN
$remoteJarApp = $env:REMOTE_JAR_APP
$remoteStaticAdmin = $env:REMOTE_STATIC_ADMIN
$remoteStaticApp = $env:REMOTE_STATIC_APP
$remoteCmd = $env:REMOTE_CMD

if (-not $SkipBackend) {
  if (-not $remoteJarAdmin -or -not $remoteJarApp) { throw "REMOTE_JAR_ADMIN / REMOTE_JAR_APP 未设置" }
}
if (-not $SkipFrontend) {
  if (-not $remoteStaticAdmin -or -not $remoteStaticApp) { throw "REMOTE_STATIC_ADMIN / REMOTE_STATIC_APP 未设置" }
}

function Invoke-Scp {
  param([string[]]$ScpArgs)
  $exe = "scp"
  & $exe @ScpArgs
  if ($LASTEXITCODE -ne 0) { throw "scp 失败: $exe $($ScpArgs -join ' ')" }
}

function Build-ScpBaseArgs {
  $args = @("-P", $sshPort, "-o", "StrictHostKeyChecking=accept-new")
  if ($identity) { $args += @("-i", $identity) }
  return $args
}

# --- 1) Git commit ---
if (-not $SkipCommit) {
  git add -A
  $msg = if ($CommitMsg) { $CommitMsg } else { "chore: quick-deploy $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" }
  $dirty = git status --porcelain
  if ($dirty) {
    git commit -m $msg
    if ($LASTEXITCODE -ne 0) {
      Write-Host "git commit 失败，请检查后重试（本次未中断）。" -ForegroundColor Red
    } else {
      Write-Host "已提交: $msg" -ForegroundColor Green
    }
  } else {
    Write-Host "工作区干净，跳过 commit。" -ForegroundColor Yellow
  }
}

# --- 2) Backend ---
if (-not $SkipBackend) {
  Write-Host ">>> Maven admin-backend" -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot "admin-backend")
  mvn -q package -DskipTests
  Pop-Location
  Write-Host ">>> Maven app-backend" -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot "app-backend")
  mvn -q package -DskipTests
  Pop-Location
}

# --- 3) Frontend ---
$adminDist = Join-Path $RepoRoot "admin-frontend\dist-admin"
$appDist = Join-Path $RepoRoot "app-frontend\dist-app"
if (-not $SkipFrontend) {
  Write-Host ">>> npm admin-frontend" -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot "admin-frontend")
  npm install --cache .npm-cache
  npm run build
  Pop-Location
  Write-Host ">>> npm app-frontend" -ForegroundColor Cyan
  Push-Location (Join-Path $RepoRoot "app-frontend")
  npm install --cache .npm-cache
  npm run build
  Pop-Location
}

# --- 4) Upload ---
$base = Build-ScpBaseArgs
if (-not $SkipBackend) {
  $adminJar = Join-Path $RepoRoot "admin-backend\target\admin-backend.jar"
  $appJar = Join-Path $RepoRoot "app-backend\target\app-backend.jar"
  if (-not (Test-Path $adminJar)) { throw "找不到 $adminJar" }
  if (-not (Test-Path $appJar)) { throw "找不到 $appJar" }
  Write-Host ">>> 确保远端 JAR 父目录存在" -ForegroundColor Cyan
  $dirAdmin = Split-Path $remoteJarAdmin -Parent
  $dirApp = Split-Path $remoteJarApp -Parent
  $sshPrep = @("-p", $sshPort, "-o", "StrictHostKeyChecking=accept-new")
  if ($identity) { $sshPrep += @("-i", $identity) }
  ssh @sshPrep $hostSpec "mkdir -p '$dirAdmin' '$dirApp'"
  Write-Host ">>> 上传 JAR" -ForegroundColor Cyan
  Invoke-Scp ($base + @($adminJar, "${hostSpec}:$remoteJarAdmin"))
  Invoke-Scp ($base + @($appJar, "${hostSpec}:$remoteJarApp"))
}

if (-not $SkipFrontend) {
  if (-not (Test-Path $adminDist)) { throw "找不到 $adminDist，请先构建 admin-frontend" }
  if (-not (Test-Path $appDist)) { throw "找不到 $appDist，请先构建 app-frontend" }
  Write-Host ">>> 确保远端静态目录存在" -ForegroundColor Cyan
  $sshMk = @("-p", $sshPort, "-o", "StrictHostKeyChecking=accept-new")
  if ($identity) { $sshMk += @("-i", $identity) }
  $sshMk += @($hostSpec, "mkdir -p '$remoteStaticAdmin' '$remoteStaticApp'")
  ssh @sshMk
  if ($LASTEXITCODE -ne 0) { throw "ssh mkdir 失败" }

  Write-Host ">>> 上传静态资源 (递归覆盖)" -ForegroundColor Cyan
  # 使用 /. 同步目录内容，避免多套一层 dist 目录名
  Invoke-Scp ($base + @("-r", "${adminDist}/.", "${hostSpec}:$remoteStaticAdmin/"))
  Invoke-Scp ($base + @("-r", "${appDist}/.", "${hostSpec}:$remoteStaticApp/"))
}

if ($remoteCmd) {
  Write-Host ">>> SSH 执行远端命令" -ForegroundColor Cyan
  $sshArgs = @("-p", $sshPort, "-o", "StrictHostKeyChecking=accept-new")
  if ($identity) { $sshArgs += @("-i", $identity) }
  $sshArgs += @($hostSpec, $remoteCmd)
  ssh @sshArgs
  if ($LASTEXITCODE -ne 0) { throw "远端命令失败" }
}

if ($GitPush) {
  Write-Host ">>> git push" -ForegroundColor Cyan
  git push
  if ($LASTEXITCODE -ne 0) { Write-Host "git push 失败（可稍后手动推送）。" -ForegroundColor Yellow }
}

Write-Host "完成。" -ForegroundColor Green
