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
  sbtix.buildSbtProgram {
    name = "{{ name }}";
    src = ./.;
    repo = [
      (import ./repo.nix)
      (import ./project/repo.nix)
      (import ./manual-repo.nix)
    ];
  }
