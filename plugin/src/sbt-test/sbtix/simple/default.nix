{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
    isTravis = builtins.getEnv "TRAVIS" == "true";
in
    sbtix.buildSbtProject {
        name = "sbtix-simple";
        src = ./.;
        repo = import ./repo.nix;

        installPhase =
            ''
                ${pkgs.lib.optionalString isTravis "unshare -n -- "}sbt three/stage
                cp -r three/target/universal/stage $out
            '';
    }
