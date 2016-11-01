{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-simple";
        src = ./.;
        repo = [ ./repo-build.nix
                 ./repo-plugins.nix
               ];

        installPhase =
            ''
                sbt three/stage
                cp -r three/target/universal/stage $out
            '';
    }
