#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SERVICE_NAME="amneziabot"

usage() {
  cat <<EOF
Usage:
  bash scripts/prod/cicd.sh <command> [args]

Commands:
  install-deps                 Install server dependencies (run as root)
  build                        Build admin UI and backend jar
  run                          Run built jar with deploy/.env
  install-service [repo user]  Install/refresh systemd service
  deploy [repo ref]            CI/CD deploy: fetch/reset, build, restart service

Examples:
  sudo bash scripts/prod/cicd.sh install-deps
  bash scripts/prod/cicd.sh build
  sudo bash scripts/prod/cicd.sh install-service /opt/amneziabot ubuntu
  bash scripts/prod/cicd.sh deploy /opt/amneziabot main
EOF
}

require_root() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "This command requires root. Run with sudo."
    exit 1
  fi
}

run_with_heartbeat() {
  local label="$1"
  shift

  "$@" &
  local cmd_pid=$!

  while kill -0 "${cmd_pid}" 2>/dev/null; do
    echo "[heartbeat] ${label} still running at $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
    sleep 20
  done

  wait "${cmd_pid}"
}

cmd_install_deps() {
  require_root

  local target_user="${SUDO_USER:-root}"

  apt-get update -y
  apt-get install -y ca-certificates curl gnupg

  local need_node_install="true"
  if command -v node >/dev/null 2>&1; then
    local node_major
    node_major="$(node -v | sed -E 's/^v([0-9]+).*/\1/')"
    if [[ "${node_major}" =~ ^[0-9]+$ ]] && [[ "${node_major}" -ge 18 ]]; then
      need_node_install="false"
    fi
  fi

  if [[ "${need_node_install}" == "true" ]]; then
    echo "Installing Node.js 20 LTS..."
    echo "Removing legacy Node.js packages if present..."
    apt-get remove -y nodejs npm libnode-dev nodejs-doc || true
    apt-get -f install -y || true

    mkdir -p /etc/apt/keyrings
    curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key \
      | gpg --dearmor --yes -o /etc/apt/keyrings/nodesource.gpg
    echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_20.x nodistro main" \
      > /etc/apt/sources.list.d/nodesource.list
    apt-get update -y
    apt-get install -y nodejs
  fi

  apt-get install -y \
    git \
    openjdk-17-jre-headless \
    maven

  if command -v docker >/dev/null 2>&1; then
    usermod -aG docker "${target_user}" || true
  else
    echo "[warn] docker not found. Install Docker because backend uses docker exec/restart."
  fi

  echo "Dependencies installed. Re-login for docker group changes."
}

cmd_build() {
  cd "${ROOT_DIR}"

  echo "[build] started at $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  run_with_heartbeat "npm install" npm install
  run_with_heartbeat "npm run admin:build" npm run admin:build
  run_with_heartbeat "maven package" mvn -B --no-transfer-progress -DskipTests clean package

  mkdir -p deploy
  local jar_file
  jar_file="$(ls -1 target/amneziabot-*.jar | grep -v '\.original$' | head -n1)"
  cp "${jar_file}" deploy/amneziabot.jar
  echo "[build] finished at $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  echo "Build completed: ${ROOT_DIR}/deploy/amneziabot.jar"
}

cmd_run() {
  cd "${ROOT_DIR}"

  local env_file="${ENV_FILE:-${ROOT_DIR}/deploy/.env}"
  local jar_file="${JAR_FILE:-${ROOT_DIR}/deploy/amneziabot.jar}"

  if [[ ! -f "${env_file}" ]]; then
    echo "Missing env file: ${env_file}"
    echo "Copy deploy/.env.example -> deploy/.env and fill values"
    exit 1
  fi

  if [[ ! -f "${jar_file}" ]]; then
    echo "Missing jar file: ${jar_file}"
    echo "Run: bash scripts/prod/cicd.sh build"
    exit 1
  fi

  set -a
  source "${env_file}"
  set +a

  exec java ${JAVA_OPTS:-"-Xms256m -Xmx512m"} -jar "${jar_file}"
}

cmd_install_service() {
  require_root

  local repo_dir="${1:-/opt/amneziabot}"
  local run_user="${2:-root}"
  local service_file="/etc/systemd/system/${SERVICE_NAME}.service"

  cat > "${service_file}" <<EOF
[Unit]
Description=AmneziaBot backend
After=network-online.target docker.service
Wants=network-online.target

[Service]
Type=simple
User=${run_user}
Group=${run_user}
WorkingDirectory=${repo_dir}
EnvironmentFile=${repo_dir}/deploy/.env
ExecStart=/usr/bin/env bash ${repo_dir}/scripts/prod/cicd.sh run
Restart=always
RestartSec=3
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

  systemctl daemon-reload
  systemctl enable "${SERVICE_NAME}"
  systemctl restart "${SERVICE_NAME}"
  systemctl --no-pager status "${SERVICE_NAME}" || true
}

cmd_deploy() {
  local repo_dir="${1:-/opt/amneziabot}"
  local git_ref="${2:-main}"

  cd "${repo_dir}"

  echo "[deploy] sync start $(date -u +'%Y-%m-%dT%H:%M:%SZ')"
  git fetch origin
  git reset --hard "origin/${git_ref}"
  echo "[deploy] sync done $(date -u +'%Y-%m-%dT%H:%M:%SZ')"

  if [[ "${DEPLOY_REEXEC:-0}" != "1" ]]; then
    echo "[deploy] re-exec with updated script"
    export DEPLOY_REEXEC=1
    exec bash "${repo_dir}/scripts/prod/cicd.sh" deploy "${repo_dir}" "${git_ref}"
  fi

  local missing_tools=()
  local need_runtime_fix="false"
  for tool in npm mvn java git; do
    if ! command -v "${tool}" >/dev/null 2>&1; then
      missing_tools+=("${tool}")
    fi
  done

  if command -v node >/dev/null 2>&1; then
    local node_major
    node_major="$(node -v | sed -E 's/^v([0-9]+).*/\1/')"
    if [[ ! "${node_major}" =~ ^[0-9]+$ ]] || [[ "${node_major}" -lt 18 ]]; then
      need_runtime_fix="true"
      echo "Node.js is too old: $(node -v) (required >= v18)"
    fi
  else
    need_runtime_fix="true"
  fi

  if [[ ${#missing_tools[@]} -gt 0 ]] || [[ "${need_runtime_fix}" == "true" ]]; then
    if [[ ${#missing_tools[@]} -gt 0 ]]; then
      echo "Missing required tools: ${missing_tools[*]}"
    fi
    if command -v sudo >/dev/null 2>&1; then
      echo "Installing dependencies automatically..."
      sudo -n bash "${repo_dir}/scripts/prod/cicd.sh" install-deps
    elif [[ "${EUID}" -eq 0 ]]; then
      bash "${repo_dir}/scripts/prod/cicd.sh" install-deps
    else
      echo "Cannot install dependencies automatically (no sudo/root)."
      echo "Run manually: sudo bash scripts/prod/cicd.sh install-deps"
      exit 1
    fi

    hash -r
    echo "Runtime versions after install:"
    command -v node >/dev/null 2>&1 && node -v || true
    command -v npm >/dev/null 2>&1 && npm -v || true
  fi

  bash scripts/prod/cicd.sh build

  if [[ -f "/etc/systemd/system/${SERVICE_NAME}.service" ]]; then
    if grep -q "scripts/prod/start.sh" "/etc/systemd/system/${SERVICE_NAME}.service"; then
      sudo -n sed -i "s#ExecStart=.*#ExecStart=/usr/bin/env bash ${repo_dir}/scripts/prod/cicd.sh run#" "/etc/systemd/system/${SERVICE_NAME}.service"
      sudo -n systemctl daemon-reload
    fi
  fi

  sudo -n systemctl restart "${SERVICE_NAME}"
  sudo -n systemctl --no-pager status "${SERVICE_NAME}" || true
}

main() {
  local command="${1:-}"
  case "${command}" in
    install-deps)
      shift
      cmd_install_deps "$@"
      ;;
    build)
      shift
      cmd_build "$@"
      ;;
    run)
      shift
      cmd_run "$@"
      ;;
    install-service)
      shift
      cmd_install_service "$@"
      ;;
    deploy)
      shift
      cmd_deploy "$@"
      ;;
    -h|--help|help|"")
      usage
      ;;
    *)
      echo "Unknown command: ${command}"
      usage
      exit 1
      ;;
  esac
}

main "$@"
