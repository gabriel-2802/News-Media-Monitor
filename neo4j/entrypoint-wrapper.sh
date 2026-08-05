#!/bin/bash
# Runs the existing, idempotent migrate.sh automatically in the background
# (it already waits for Neo4j to accept connections itself — see
# neo4j/migrate.sh), then execs into the image's real entrypoint as PID 1
# (kept under tini — see Dockerfile ENTRYPOINT) so signal handling/zombie
# reaping is unaffected. migrate.sh tracks applied versions as
# (:__Migration) nodes, so re-running it (container restart, or the
# existing `make migrate`) only ever applies newly-added migration files.
set -e

# Derive the password from NEO4J_AUTH ("neo4j/<password>") and pass it only
# to this one backgrounded subprocess (not exported) — migrate.sh reads
# NEO4J_PASSWORD directly. It must NOT be set as a persistent env var on the
# container itself: the official image maps every NEO4J_* env var to a
# neo4j.conf setting, and "PASSWORD" isn't a valid config key, so doing that
# makes the real server refuse to start under strict config validation.
NEO4J_PASSWORD="${NEO4J_AUTH#*/}" bash /migrate.sh &

exec /startup/docker-entrypoint.sh "$@"
