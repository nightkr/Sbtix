{ pkgs ? (import ./pkgs.nix) {}}: with pkgs;

let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-simple";
        src = ./.;
        repo = [ (import ./repo.nix)
                 (import ./project/repo.nix)
                 (import ./manual-repo.nix)
               ];

        installPhase = ''
          sbt three/stage
          cp -r three/target/universal/stage $out
        '';
    }
