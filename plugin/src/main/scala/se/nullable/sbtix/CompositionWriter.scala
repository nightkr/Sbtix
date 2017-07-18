package se.nullable.sbtix

object CompositionWriter {
  def apply(compositionType: String, buildName: String): String = stripWhitespace(
    s"""
       |# this file originates from SBTix
       |{ pkgs ? import <nixpkgs> {} }:
       |
       |with pkgs;
       |
       |let
       |  sbtix = callPackage ./sbtix.nix {};
       |in
       |  sbtix.buildSbt${compositionType.capitalize} {
       |    name = "$buildName";
       |    src = ./.;
       |    repo = [
       |      (import ./repo.nix)
       |      (import ./project/repo.nix)
       |      (import ./manual-repo.nix)
       |    ];
       |  }
     """.stripMargin
  )

  private def stripWhitespace(t:String) = t.split("\n") map { (l:String) => l.replaceAll("^\\s+$", "") } mkString "\n"
}
