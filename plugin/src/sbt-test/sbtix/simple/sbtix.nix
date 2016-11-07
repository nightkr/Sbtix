{ runCommand, fetchurl, lib, stdenv, utillinux, jdk, sbt, writeText }:
rec {
    unshareify = cmd:
        let
            useUnshare = builtins.getEnv "TRAVIS" == "true";
            unsharePrefix = "${utillinux}/bin/unshare -n -- ";
        in
            lib.optionalString useUnshare unsharePrefix + cmd;

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
          artifacts = mergeAttr "artifacts" repo;
          repos = mergeAttr "repos" repo;
          nixrepo = mkRepo "${name}-repo" artifacts;
          repoDefs = repoConfig repos nixrepo;
      in stdenv.mkDerivation (rec {
            
            dontPatchELF      = true;
            dontStrip         = true;

            # SBT Launcher Configuration
            # http://www.scala-sbt.org/0.13.5/docs/Launcher/Configuration.html
            sbtixRepos = writeText "sbtixRepos" ''
              [repositories]
              # name: url(, pattern)(,descriptorOptional)(,skipConsistencyCheck)
                ${repoDefs}
            '';

            # set environment variable to affect all SBT commands
            SBT_OPTS = ''
             -Dsbt.ivy.home=./.ivy2/
             -Dsbt.boot.directory=./.sbt/boot/
             -Dsbt.override.build.repos=true
             -Dsbt.repository.config=${sbtixRepos}
             ${sbtOptions}'';


            buildPhase = unshareify "sbt compile";
        } // args // {
            repo = null;
            buildInputs = [ jdk sbt ] ++ buildInputs;
        });
}
