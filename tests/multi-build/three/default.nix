{pkgs ? import <nixpkgs> {}, sbtix ? pkgs.callPackage ../../../plugin/nix-exprs/sbtix.nix {}}:
let
    one = pkgs.callPackage ../one/one.nix {
        inherit sbtix;
    };
    two = pkgs.callPackage ../two/two.nix {
        inherit sbtix one;
    };
    three = pkgs.callPackage ./three.nix {
        inherit sbtix two;
    };
in three
