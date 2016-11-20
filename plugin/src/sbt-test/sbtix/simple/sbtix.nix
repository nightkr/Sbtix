{ runCommand, fetchurl, lib, stdenv, jdk, sbt, writeText }:
with stdenv.lib;
with import <nixpkgs> {};

let sbtTemplate = repoDefs: versioning:
    let
        buildSbt = writeText "build.sbt" ''
          scalaVersion := "${versioning.scalaVersion}"
        '';

        mainScala = writeText "Main.scala" ''
          object Main extends App {
            println("hello nix")
          }
        '';

        buildProperties = writeText "build.properties" ''
          sbt.version=${versioning.sbtVersion}
        '';

        # SBT Launcher Configuration
        # http://www.scala-sbt.org/0.13.5/docs/Launcher/Configuration.html
        sbtixRepos = writeText "sbtixRepos" ''
        [repositories]
        ${repoDefs}
        local
        '';
    in stdenv.mkDerivation (rec {
            
            name = "sbt-setup-template";

            dontPatchELF      = true;
            dontStrip         = true;

            # set environment variable to affect all SBT commands
            SBT_OPTS = ''
             -Dsbt.ivy.home=./.ivy2/
             -Dsbt.boot.directory=./.sbt/boot/
             -Dsbt.global.staging=./.staging
             -Dsbt.override.build.repos=true
             -Dsbt.repository.config=${sbtixRepos}
            '';

            unpackPhase = ''
              ln -s ${buildSbt}  ./build.sbt
              ln -s ${mainScala} ./Main.scala

              mkdir -p ./project

              ln -s ${buildProperties} ./project/build.properties
            '';
            
            buildInputs = [ jdk sbt ];

            buildPhase = ''sbt compile:compileIncremental'';
            
            installPhase =''
              mkdir -p $out
              # Copy the hidden ivy lock files. Only keep ivy cache folder, not ivy local. local might be empty now but I want to be sure it is not polluted in the future. 
              rm -rf ./.ivy2/local
              cp -r ./.ivy2 $out/ivy
              cp -r ./.sbt $out/sbt 
            '';
    });

  mergeSbtTemplates = templates: runCommand "merge-sbt-template" {}
        (let
            copyTemplate = template:
                [ "cp -rns ${template}/ivy $out"
                  "cp -rns ${template}/sbt $out"
                  "chmod -R u+rw $out"
                ];
        in
            lib.concatStringsSep "\n" (["mkdir -p $out"] ++ lib.concatLists (map copyTemplate templates)) 
        );

in rec {
    mkRepo = name: artifacts: runCommand name {}
        (let
            parentDirs = filePath: 
                concatStringsSep "/" (init (splitString "/" filePath));
            linkArtifact = outputPath: urlAttrs:
                [ "mkdir -p \"$out/${parentDirs outputPath}\""
                  "ln -fsn \"${fetchurl urlAttrs}\" \"$out/${outputPath}\""
                ];
        in
            lib.concatStringsSep "\n" (lib.concatLists (lib.mapAttrsToList linkArtifact artifacts)));
    
    repoConfig = repos: nixrepo:
    let
        repoPatternOptional = repoPattern:
            optionalString (repoPattern != "") ", ${repoPattern}";
        repoPath = repoName: repoPattern:
            [ "${repoName}: file://${nixrepo}/${repoName}${repoPatternOptional repoPattern}" ];
    in
        lib.concatStringsSep "\n  " (lib.concatLists (lib.mapAttrsToList repoPath repos));

    mergeAttr = attr: repo:
        fold (a: b: a // b) {} (catAttrs attr repo);

    buildSbtProject = args@{repo, name, buildInputs ? [], sbtOptions ? "", ...}:
      let
          versionings = unique (flatten (catAttrs "versioning" repo));
          artifacts = mergeAttr "artifacts" repo;
          repos = mergeAttr "repos" repo;
          nixrepo = mkRepo "${name}-repo" artifacts;
          repoDefs = repoConfig repos nixrepo;
          sbtSetupTemplate = mergeSbtTemplates(map (sbtTemplate repoDefs) versionings);

          # SBT Launcher Configuration
          # http://www.scala-sbt.org/0.13.5/docs/Launcher/Configuration.html
          sbtixRepos = writeText "sbtixRepos" ''
            [repositories]
            ${repoDefs}
            local
            '';

      in stdenv.mkDerivation (rec {
            
            dontPatchELF      = true;
            dontStrip         = true;

            # COURSIER_CACHE env variable is needed if one wants to use non-sbtix repositories in the below repo list, which is sometimes useful.
            COURSIER_CACHE = "./.coursier/cache/v1";

            configurePhase = ''
              cp -Lr ${sbtSetupTemplate}/ivy ./.ivy2
              cp -Lr ${sbtSetupTemplate}/sbt ./.sbt
              chmod -R 755 ./.ivy2/
              chmod -R 755 ./.sbt/
            '';

            # set environment variable to affect all SBT commands
            SBT_OPTS = ''
             -Dsbt.ivy.home=./.ivy2/
             -Dsbt.boot.directory=./.sbt/boot/
             -Dsbt.global.staging=./.staging
             -Dsbt.override.build.repos=true
             -Dsbt.repository.config=${sbtixRepos}
             ${sbtOptions}'';
            

            buildPhase = ''sbt compile'';
        } // args // {
            repo = null;
            buildInputs = [ jdk sbt ] ++ buildInputs;
        });
}
