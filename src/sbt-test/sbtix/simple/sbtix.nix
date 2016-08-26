{ runCommand, fetchurl, lib, stdenv, jdk, sbt }:
rec {
    mkMavenRepo = name: repo: runCommand name {}
        (let
            slashify = builtins.replaceStrings ["."] ["/"];
            linkArtifact = organization: module: version: type: urlAttrs:
                "ln -fsn ${fetchurl urlAttrs} $out/${slashify organization}/${module}/${version}/${module}-${version}.${type}";
            linkVersion = organization: module: version: versionAttrs:
                [ "mkdir -p $out/${slashify organization}/${module}/${version}/" ]
                ++ lib.mapAttrsToList (linkArtifact organization module version) versionAttrs;
            linkModule = organization: module: moduleAttrs:
                lib.concatLists (lib.mapAttrsToList (linkVersion organization module) moduleAttrs);
            linkOrganization = organization: organizationAttrs:
                lib.concatLists (lib.mapAttrsToList (linkModule organization) organizationAttrs);
            linkProject = projectName: project:
                lib.concatLists (lib.mapAttrsToList linkOrganization project);
        in
            lib.concatStringsSep "\n" (lib.concatLists (lib.mapAttrsToList linkProject repo)));

    buildSbtProject = args@{repo, name, buildInputs ? [], sbtOptions ? "", ...}:
        stdenv.mkDerivation (rec {
            mvn = mkMavenRepo "${name}-repo" repo;
            configurePhase = ''echo "externalResolvers in ThisBuild := Seq(\"nix\" at \"file://${mvn}\")" >> build.sbt'';
            buildPhase = ''sbt compile ${sbtOptions}'';
        } // args // {
            repo = null;
            buildInputs = [ jdk sbt ] ++ buildInputs;
        });
}