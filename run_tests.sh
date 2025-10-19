#!/usr/bin/env bash
set -euo pipefail

CLASSPATH="out"
SRC_DIR="."
MAIN_CLASS="CouncilMember"
ADMIN_CLASS="Admin"
CONFIG="network.config"
LOG_DIR="logs"
MEMBERS=(M1 M2 M3 M4 M5 M6 M7 M8 M9)
PROFILES_RELIABLE=(reliable reliable reliable reliable reliable reliable reliable reliable reliable)
PROFILES_MIXED=(reliable latent failure standard standard standard standard standard standard)

free_ports() {
  for p in {9001..9009}; do
    pid="$(lsof -ti tcp:$p)"
    if [ -n "$pid" ]; then
      kill "$pid" 2>/dev/null || true
      sleep 0.2
      kill -9 "$pid" 2>/dev/null || true
      echo "[FREE] port $p (pid $pid)"
    fi
  done
}
cleanup() {
  echo "[CLEANUP] stopping members..."
  if [[ -f .pids ]]; then
    xargs kill < .pids 2>/dev/null || true
    sleep 0.5
    xargs kill -9 < .pids 2>/dev/null || true
    rm -f .pids
  fi
}
trap cleanup EXIT

prepare() {
  mkdir -p "$LOG_DIR" "$CLASSPATH"
  echo "[BUILD] javac -d $CLASSPATH $SRC_DIR/*.java"
  javac -d "$CLASSPATH" "$SRC_DIR"/*.java
  [[ -f "$CONFIG" ]] || {
    echo " Can not find  $CONFIG please put it in root "; exit 1;
  }
}

start_members() {
  profiles_var="$1"
  echo "start"
  rm -f .pids
  i=0
  for mid in "${MEMBERS[@]}"; do
    profile=$(eval "echo \${${profiles_var}[$i]}")
    nohup java -cp "$CLASSPATH" "$MAIN_CLASS" "$mid" --profile "$profile" --config "$CONFIG" \
      > "$LOG_DIR/$mid.log" 2>&1 &
    echo $! >> .pids
    echo "  + $mid ($profile) PID=$! log=$LOG_DIR/$mid.log"
    i=$((i+1))
    sleep 0.1
  done
  sleep 1
}

admin_propose() {
  from="$1"; to="$2"; cand="$3"
  echo "[ADMIN] $from -> $to propose $cand"
  java -cp "$CLASSPATH" "$ADMIN_CLASS" "$from" "$to" "$cand" --config "$CONFIG"
}

admin_propose_bg() { ( admin_propose "$1" "$2" "$3" ) & }

tail_logs() {
  echo "================== LOG TAILS =================="
  for m in "${MEMBERS[@]}"; do
    echo "----- $m -----"
    tail -n 30 "$LOG_DIR/$m.log" || true
  done
  echo "==============================================="
}

wait_for() { sleep "${1:-3}"; }

scenario_1() {
  echo
  echo "===== Scenario 1: All reliable）—— M4 --> M5） ====="
  start_members "PROFILES_RELIABLE"
  admin_propose M4 M4 M5
  wait_for 3
  tail_logs
  cleanup
}

scenario_2() {
  echo
  echo "===== Scenario 2: All reliable, Concurrent proposal M1->M1 and  M8->M8 ====="
  start_members "PROFILES_RELIABLE"
  admin_propose_bg M1 M1 M1
  admin_propose_bg M8 M8 M8
  wait_for 4
  tail_logs
  cleanup
}

scenario_3() {
  echo
  echo "===== Scenario 3: M1=reliable, M2=latent, M3=failure, other standard  ====="
  start_members "PROFILES_MIXED"

  echo "[3a] M4 ---> M5"
  admin_propose M4 M4 M5
  wait_for 4
  tail_logs

  echo "[3b] M2 (High Latency) initiates the nomination of M2"
  admin_propose M2 M2 M2
  wait_for 5
  tail_logs

  echo "[3c] After M3 was initiated, it crashed and M4 continued instead"
  admin_propose M3 M3 M3
  cleanup
  start_members "PROFILES_MIXED"
  admin_propose M4 M4 M5
  wait_for 5
  tail_logs

  cleanup
}

prepare

echo "=== Test Scenario 1 ==="; scenario_1
echo "=== Test Scenario 2 ==="; scenario_2
echo "=== Test Scenario 3 ==="; scenario_3

echo "All finish please at  $LOG_DIR/*.log"
