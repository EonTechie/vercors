#!/usr/bin/env bash
set -uo pipefail

real_vercors="${REAL_VERCORS:?REAL_VERCORS is not set}"
limit="${VERCORS_CALL_TIMEOUT_SECONDS:-3900}"

set +e
timeout --signal=TERM --kill-after=300s "${limit}s" "$real_vercors" "$@"
status=$?
set -e

if [[ "$status" -eq 124 || "$status" -eq 137 ]]; then
  echo "VERCORS_PROCESS_TIMEOUT: stopped after ${limit} seconds" >&2
fi

exit "$status"
