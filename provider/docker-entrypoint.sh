#!/bin/sh
# Resolves the Docker/Swarm secrets "_FILE" convention: for any FOO_FILE env
# var pointing at a mounted file, export FOO with the file's contents, then
# hand off to the real command. Inert when no *_FILE vars are set (plain
# docker-compose, local dev) — this only matters for the Swarm stack, which
# mounts secrets as files rather than plain env vars.
set -e

for name in $(env | cut -d= -f1); do
  case "$name" in
    *_FILE)
      base_var=${name%_FILE}
      file_path=$(eval echo "\$$name")
      if [ -n "$file_path" ] && [ -f "$file_path" ]; then
        export "$base_var"="$(cat "$file_path")"
      fi
      ;;
  esac
done

exec "$@"
