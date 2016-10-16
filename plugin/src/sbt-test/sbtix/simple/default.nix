{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-simple";
        src = ./.;
        repo = import ./repo.nix;
        sbtOptions = "-Dplugin.version=0.1-SNAPSHOT";

        installPhase =
            ''
                sbt three/stage
                cp -r three/target/universal/stage $out
            '';
    }
