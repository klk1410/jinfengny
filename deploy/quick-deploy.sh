#!/usr/bin/env bash
# Bash 快速部署（WSL/Linux/macOS/Git Bash）
# 在仓库根「环保油」下：bash deploy/quick-deploy.sh
# 复制 deploy.env.example → deploy/deploy.env 并填写。

set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

ENV_FILE="$ROOT/deploy/deploy.env"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "缺少 $ENV_FILE，请复制 deploy/deploy.env.example"; exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${DEPLOY_HOST:?请在 deploy.env 设置 DEPLOY_HOST，如 root@1.2.3.4}"
SSH_PORT="${SSH_PORT:-22}"
REMOTE_JAR_ADMIN="${REMOTE_JAR_ADMIN:?}"
REMOTE_JAR_APP="${REMOTE_JAR_APP:?}"
REMOTE_STATIC_ADMIN="${REMOTE_STATIC_ADMIN:?}"
REMOTE_STATIC_APP="${REMOTE_STATIC_APP:?}"

SSH_ARGS=( -p "$SSH_PORT" -o StrictHostKeyChecking=accept-new )
SCP_ARGS=( -P "$SSH_PORT" -o StrictHostKeyChecking=accept-new )
if [[ -n "${SSH_IDENTITY:-}" ]]; then
  SSH_ARGS+=( -i "$SSH_IDENTITY" )
  SCP_ARGS+=( -i "$SSH_IDENTITY" )
fi

SKIP_COMMIT=0
SKIP_FRONT=0
SKIP_BACK=0
COMMIT_MSG=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-commit) SKIP_COMMIT=1 ;;
    --skip-frontend) SKIP_FRONT=1 ;;
    --skip-backend) SKIP_BACK=1 ;;
    --msg)
      COMMIT_MSG="$2"
      shift
      ;;
    *)
      echo "未知参数: $1"; exit 1
      ;;
  esac
  shift
done

if [[ "$SKIP_COMMIT" -eq 0 ]]; then
  git add -A
  if [[ -n "$(git status --porcelain)" ]]; then
    MSG="${COMMIT_MSG:-chore: quick-deploy $(date '+%Y-%m-%d %H:%M:%S')}"
    git commit -m "$MSG"
    echo "已提交: $MSG"
  else
    echo "工作区干净，跳过 commit。"
  fi
fi

if [[ "$SKIP_BACK" -eq 0 ]]; then
  echo ">>> Maven admin-backend (clean package)"
  (cd "$ROOT/admin-backend" && mvn -q clean package -DskipTests)
  echo ">>> Maven app-backend (clean package)"
  (cd "$ROOT/app-backend" && mvn -q clean package -DskipTests)
fi

if [[ "$SKIP_FRONT" -eq 0 ]]; then
  (cd "$ROOT/admin-frontend" && npm install --cache .npm-cache && npm run build)
  (cd "$ROOT/app-frontend" && npm install --cache .npm-cache && npm run build)
fi

ADMIN_DIST="$ROOT/admin-frontend/dist-admin"
APP_DIST="$ROOT/app-frontend/dist-app"

if [[ "$SKIP_BACK" -eq 0 ]]; then
  ADIR="$(dirname "$REMOTE_JAR_ADMIN")"
  PDIR="$(dirname "$REMOTE_JAR_APP")"
  ssh "${SSH_ARGS[@]}" "$DEPLOY_HOST" "mkdir -p '$ADIR' '$PDIR'"
  scp "${SCP_ARGS[@]}" "$ROOT/admin-backend/target/admin-backend.jar" "$DEPLOY_HOST:$REMOTE_JAR_ADMIN"
  scp "${SCP_ARGS[@]}" "$ROOT/app-backend/target/app-backend.jar" "$DEPLOY_HOST:$REMOTE_JAR_APP"
fi

if [[ "$SKIP_FRONT" -eq 0 ]]; then
  ssh "${SSH_ARGS[@]}" "$DEPLOY_HOST" "mkdir -p '$REMOTE_STATIC_ADMIN' '$REMOTE_STATIC_APP'"
  scp "${SCP_ARGS[@]}" -r "$ADMIN_DIST/." "$DEPLOY_HOST:$REMOTE_STATIC_ADMIN/"
  scp "${SCP_ARGS[@]}" -r "$APP_DIST/." "$DEPLOY_HOST:$REMOTE_STATIC_APP/"
fi

if [[ -n "${REMOTE_CMD:-}" ]]; then
  ssh "${SSH_ARGS[@]}" "$DEPLOY_HOST" "$REMOTE_CMD"
fi

echo "完成。"
