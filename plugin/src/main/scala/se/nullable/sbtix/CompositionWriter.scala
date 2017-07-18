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
       |  sbtix =
       |    let
       |      sbtixDir = fetchFromGitHub {
       |        owner = "teozkr";
       |        repo = "Sbtix";
       |        rev = "5277d96745afcc04a0873102f4a5f80cfc68fa23";
       |        sha256 = "17h2ijb50q76al72hpggv35bbqiayyzvybmfwyx1cr5xzlpvzcqh";
       |      };
       |    in
       |      callPackage "$${sbtixDir}/sbtix.nix" {};
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
