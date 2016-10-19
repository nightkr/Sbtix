{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-private-auth";
        src = ./.;
        repo = import ./repo.nix;

        installPhase =
            ''
                ${sbtix.unshareify "sbt three/stage"}
                cp -r three/target/universal/stage $out
            '';
    }
