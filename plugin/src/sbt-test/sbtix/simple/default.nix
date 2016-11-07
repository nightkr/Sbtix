{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
    isTravis = builtins.getEnv "TRAVIS" == "true";
in
    sbtix.buildSbtProject {
        name = "sbtix-simple";
        src = ./.;
        repo = [ (import ./repo.nix)
                 (import ./project/repo.nix)
                 (import ./manual-repo.nix)
               ];

        installPhase =
            ''
                ${sbtix.unshareify "sbt three/stage"}
                cp -r three/target/universal/stage $out
            '';
    }
