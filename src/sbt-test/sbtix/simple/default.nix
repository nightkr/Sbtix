{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    repo = import ./repo.nix;
    mkMavenEnv = name: repo: runCommand name {}
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
            #builtins.trace (builtins.toJSON (lib.concatLists (lib.mapAttrsToList linkProject repo))) "touch $out");
            ''
                ${lib.concatStringsSep "\n" (lib.concatLists (lib.mapAttrsToList linkProject repo))}
            '');
in
    stdenv.mkDerivation rec {
        name = "sbt2nix-simple";
        src = ./.;
        mvn = mkMavenEnv "${name}-env" repo;
        buildInputs = [ jdk sbt ];
        configurePhase =
            ''
                echo "externalResolvers in ThisBuild := Seq(\"nix\" at \"file://${mvn}\")" >> build.sbt
            '';
        buildPhase =
            ''
                sbt three/stage -Dplugin.version=0.1-SNAPSHOT
            '';
        installPhase =
            ''
                cp -r three/target/universal/stage $out
            '';
    }
