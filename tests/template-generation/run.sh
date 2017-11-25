#!/usr/bin/env bash

set -euxo pipefail

cd "$(dirname "$0")"
for f in {,project/}repo.nix; do
  if [[ -e "$f" ]]; then
    rm "$f"
  fi
done

sbtix-gen-all2
nix-build
