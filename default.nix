{ pkgs ? (import ./pkgs.nix) {}}: with pkgs;

let
  version = "0.2";
  versionSnapshotSuffix = "-SNAPSHOT";

  sbtix = pkgs.callPackage ./sbtix.nix {};

  sbtixPluginRepo = sbtix.buildSbtProject {
        name = "sbtix-plugin";

        src = ./plugin;
        repo = [ (import ./plugin/repo.nix)
                 (import ./plugin/project/repo.nix)
                 (import ./manual-repo.nix)
               ];

        installPhase =''
          sbt publish-local
          mkdir -p $out/plugin-repo
          cp ./.ivy2/local/* $out/plugin-repo -r
        '';
  };

  pluginsSbtix = writeText "plugins.sbt" ''
    resolvers += Resolver.file("Sbtix Plugin Repo", file("${sbtixPluginRepo}/plugin-repo"))(Resolver.ivyStylePatterns)

    addSbtPlugin("se.nullable.sbtix" % "sbtix" % "${version}${versionSnapshotSuffix}")
  '';

  sbtixScript = writeScriptBin "sbtix" ''
    #! ${stdenv.shell}

    # remove the ivy cache of sbtix so sbt retrieves from the sbtix nix repo. 
    # without this your version of sbtix may be overriden by the local ivy cache.
    echo "Deleting any cached sbtix plugins in '~/.ivy'. So the most recent version from nix is used."
    find ~/.ivy2 -name 'se.nullable.sbtix' -type d -exec rm -rf {} \; > /dev/null 2>&1

    #the global plugins directory must be writeable
    SBTIX_GLBASE_DIR="$HOME/.sbtix"

    # if the directory doesn't exist then create it
    if ! [ -d "$SBTIX_GLBASE_DIR" ]; then
      echo "Creating $HOME/.sbtix, sbtix global configuration directory"
      mkdir -p "$SBTIX_GLBASE_DIR/plugins"
    fi

    # if sbtix_plugin.sbt is a link or does not exist then update the link. If it is a regular file do not replace it.
    if [ -L "$SBTIX_GLBASE_DIR/plugins/sbtix_plugin.sbt" ] || [ ! -f "$SBTIX_GLBASE_DIR/plugins/sbtix_plugin.sbt" ]; then
      echo "Updating $HOME/.sbtix/plugins/sbtix_plugin.sbt symlink"
      ln -sf ${pluginsSbtix} "$SBTIX_GLBASE_DIR/plugins/sbtix_plugin.sbt"
    fi


    #the sbt.global.base directory must be writable
    sbt -Dsbt.global.base=$SBTIX_GLBASE_DIR "$@"
  '';

  sbtixGenScript = writeScriptBin "sbtix-gen" ''
    #! ${stdenv.shell}

    sbtix genNix
  '';

  sbtixGenallScript = writeScriptBin "sbtix-gen-all" ''
    #! ${stdenv.shell}

    sbtix genNix "reload plugins" genNix
  '';

  sbtixGenall2Script = writeScriptBin "sbtix-gen-all2" ''
    #! ${stdenv.shell}

    sbtix genNix "reload plugins" genNix "reload plugins" genNix
  '';

in
stdenv.mkDerivation {
  name = "sbtix-${version}";
  
  src = ./.;

  phases = [ "installPhase" ];

  installPhase =''
    mkdir -p $out/bin
    ln -s ${sbtixScript}/bin/sbtix $out/bin/.
    ln -s ${sbtixGenScript}/bin/sbtix-gen $out/bin/.
    ln -s ${sbtixGenallScript}/bin/sbtix-gen-all $out/bin/.
    ln -s ${sbtixGenall2Script}/bin/sbtix-gen-all2 $out/bin/.
    ln -s ${sbtixPluginRepo}/plugin-repo $out
    ln -s ${pluginsSbtix} $out/sbtix_plugin.sbt
  '';
}
