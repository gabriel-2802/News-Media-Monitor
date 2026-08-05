#!/bin/sh
# Runs the existing, idempotent setup.sh automatically in the background (it
# already waits for the management API to come up itself — see
# rabbitmq/setup.sh), then execs into the image's real entrypoint as PID 1.
# setup.sh only ever PUTs vhosts/exchanges/queues/bindings, so re-running it
# (container restart, or the existing `make rabbitmq-setup`) is always safe.
set -e

sh /setup.sh &

exec docker-entrypoint.sh "$@"
