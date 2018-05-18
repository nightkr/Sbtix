let
  fetchNixpkgs = import ./fetchNixpkgs.nix;

  nixpkgs = fetchNixpkgs {
     rev          = "78d4a1e3ea294ef5f1dcba110825831c02f3a46c";
     sha256       = "0sv1f6bwbisijp1h0g3qvv43m5hq74xlwsnv5wm27mbi8lzgm057";
     outputSha256 = "1f4x4bkssclmcprd0lm52sjlnnyfmabgxff8cdf68y6dc1lkhz5z";
  };

  pinSBT = packagesNew: packagesOld: {
    sbt = packagesNew.callPackage ./sbt.nix { };
  };

in

{ pkgs ? import nixpkgs { overlays = [ pinSBT ]; } }:
pkgs.callPackage ./sbtix-tool.nix {}
