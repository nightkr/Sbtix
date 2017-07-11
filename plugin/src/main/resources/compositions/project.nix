# this file originates from SBTix
{ pkgs ? import <nixpkgs> {} }:

with pkgs;

let
  sbtix =
    let
      sbtixDir = fetchFromGitHub {
        owner = "teozkr";
        repo = "Sbtix";
        rev = "5277d96745afcc04a0873102f4a5f80cfc68fa23";
        sha256 = "17h2ijb50q76al72hpggv35bbqiayyzvybmfwyx1cr5xzlpvzcqh";
      };
    in
      callPackage "${sbtixDir}/sbtix.nix" {};
in
  sbtix.buildSbtProject {
    name = "{{ name }}";
    src = ./.;
    repo = [
      (import ./repo.nix)
      (import ./project/repo.nix)
      (import ./manual-repo.nix)
    ];

    installPhase = ''
      # `buildSbtProject` is the simplest derivation provided by SBTix
      # you can specify which steps to be done in the `installPhase` directly
    '';
  }