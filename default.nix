{ pkgs ? import <nixpkgs> {} }:
pkgs.callPackage ./sbtix-tool.nix {}
