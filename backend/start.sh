#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="${SCRIPT_DIR}"
PROJECT_HOME="$(cd "${APP_HOME}/.." && pwd)"
ENV_FILE="${APP_HOME}/.env"
JAR_FILE="${APP_HOME}/anime-blog-backend-1.0.0.jar"
LOG_DIR="${PROJECT_HOME}/logs"
LOG_FILE="${LOG_DIR}/backend.log"
PID_FILE="${APP_HOME}/app.pid"
MODE="${1:-run}"

if [ ! -f "${ENV_FILE}" ]; then
  echo "Missing .env file: ${ENV_FILE}" >&2
  exit 1
fi

if [ ! -f "${JAR_FILE}" ]; then
  echo "Missing JAR file: ${JAR_FILE}" >&2
  exit 1
fi

mkdir -p "${LOG_DIR}"

set -a
source "${ENV_FILE}"
set +a

export SERVER_PORT="${SERVER_PORT:-8080}"
export APP_UPLOAD_DIR="${APP_UPLOAD_DIR:-${PROJECT_HOME}/storage/uploads}"

JAVA_BIN="${JAVA_BIN:-java}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx768m}"

if ! command -v "${JAVA_BIN}" >/dev/null 2>&1; then
  echo "Java not found: ${JAVA_BIN}" >&2
  exit 1
fi

read_pid() {
  if [ -f "${PID_FILE}" ]; then
    cat "${PID_FILE}" 2>/dev/null || true
  fi
}

is_running() {
  local pid
  pid="$(read_pid)"
  if [ -n "${pid}" ] && kill -0 "${pid}" >/dev/null 2>&1; then
    return 0
  fi
  return 1
}

cd "${APP_HOME}"

case "${MODE}" in
  run)
    exec "${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR_FILE}"
    ;;
  start)
    if is_running; then
      echo "Application is already running with PID $(read_pid)" >&2
      exit 1
    fi
    rm -f "${PID_FILE}"
    nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR_FILE}" >> "${LOG_FILE}" 2>&1 &
    APP_PID=$!
    echo "${APP_PID}" > "${PID_FILE}"
    echo "Started anime-blog-backend"
    echo "PID: ${APP_PID}"
    echo "Log: ${LOG_FILE}"
    ;;
  stop)
    if ! is_running; then
      echo "Application is not running"
      rm -f "${PID_FILE}"
      exit 0
    fi
    APP_PID="$(read_pid)"
    kill "${APP_PID}"
    rm -f "${PID_FILE}"
    echo "Stopped anime-blog-backend (PID: ${APP_PID})"
    ;;
  restart)
    if is_running; then
      APP_PID="$(read_pid)"
      kill "${APP_PID}"
      rm -f "${PID_FILE}"
      sleep 1
    fi
    nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${JAR_FILE}" >> "${LOG_FILE}" 2>&1 &
    APP_PID=$!
    echo "${APP_PID}" > "${PID_FILE}"
    echo "Restarted anime-blog-backend"
    echo "PID: ${APP_PID}"
    echo "Log: ${LOG_FILE}"
    ;;
  status)
    if is_running; then
      echo "Application is running with PID $(read_pid)"
    else
      echo "Application is not running"
      exit 1
    fi
    ;;
  *)
    echo "Usage: $0 [run|start|stop|restart|status]" >&2
    exit 1
    ;;
esac
