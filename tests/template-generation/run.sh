#!/bin/sh

set -euxo pipefail

for f in {,project/}repo.nix; do
  if [[ -e "$f" ]]; then
    rm "$f"
  fi
done

sbtix-gen-all
nix-build
./result/bin/tmpl-gen-test
