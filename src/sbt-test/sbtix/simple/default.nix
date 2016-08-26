{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbt2nix-simple";
        src = ./.;
        repo = import ./repo.nix;
        sbtOptions = "-Dplugin.version=0.1-SNAPSHOT";

        installPhase =
            ''
                sbt three/stage -Dplugin.version=0.1-SNAPSHOT
                cp -r three/target/universal/stage $out
            '';
    }
