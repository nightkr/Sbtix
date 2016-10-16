{ runCommand, fetchurl, lib, stdenv, jdk, sbt, writeText }:
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

            #eventually this repo list should only contain the nix repo
            sbtixRepos = writeText "sbtixRepos" ''
              [repositories]
                nix: file://${mvn}
                local
                maven-local
                scala-tools-releases
                typesafe-ivy-releases: http://repo.typesafe.com/typesafe/ivy-releases/, [organization]/[module]/[revision]/[type]s/[artifact](-[classifier]).[ext], bootOnly
                maven-central
                sbt-plugin-releases: http://dl.bintray.com/sbt/sbt-plugin-releases/, [organization]/[module]/(scala_[scalaVersion]/)(sbt_[sbtVersion]/)[revision]/[type]s/[artifact](-[classifier]).[ext]
                sonatype-oss-releases'';

            #set environment variable to affect all SBT commands
            SBT_OPTS = ''
             -Dsbt.ivy.home=./.ivy2/
             -Dsbt.boot.directory=./.sbt/boot/
             -Dsbt.override.build.repos=true
             -Dsbt.repository.config=${sbtixRepos}
             ${sbtOptions}'';
            

            buildPhase = ''sbt compile'';
        } // args // {
            repo = null;
            buildInputs = [ jdk sbt ] ++ buildInputs;
        });
}
