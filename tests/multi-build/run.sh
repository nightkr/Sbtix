#!/usr/bin/env bash

set -euxo pipefail
cd "$(dirname "$0")"

for f in {one,two,three}/{,project/}repo.nix; do
    if test -e $f; then
        rm "$f"
    fi
done

pushd one
sbtix-gen-all
popd

pushd two
sbtix-gen
popd

pushd three
sbtix-gen-all
nix-build
./result/bin/mb-three
popd