with import <nixpkgs> {};

let

  sbtixRepo = stdenv.mkDerivation {
    name = "sbtix-repo-0.1";
  
    src = fetchurl {
      url = https://github.com/cessationoftime/Sbtix/releases/download/bootstrap/sbtix-ivyrepo-0.1.tar.gz;
      sha256 = "0cf01k265a6rz05wwzc3saqw5q9afk37zbyg832c6dbcrmjx7h4z";
    };

    phases = [ "unpackPhase" "installPhase" ];

    installPhase =''
        mkdir -p $out/plugin-repo
        cp ./* $out/plugin-repo -r
    '';
  };

  pluginsSbtix = writeText "plugins.sbt" ''
    resolvers += Resolver.file("Sbtix Plugin Repo", file("${sbtixRepo}/plugin-repo"))(Resolver.ivyStylePatterns)

    addSbtPlugin("se.nullable.sbtix" % "sbtix" % "0.1-SNAPSHOT")
  '';

  sbtixScript = writeScriptBin "sbtix" ''
     #! ${stdenv.shell}

     #the global plugins directory must be writeable
     SBTIX_GLBASE_DIR="$HOME/.sbtix"

     if [ ! -d "$SBTIX_GLBASE_DIR" ]; then
       mkdir -p "$SBTIX_GLBASE_DIR/plugins"
       ln -s ${pluginsSbtix} "$SBTIX_GLBASE_DIR/plugins/sbtix_plugin.sbt"
     fi

    #the sbt.global.base directory must be writable
    sbt -Dsbt.global.base=$SBTIX_GLBASE_DIR "$@"
  '';

  sbtixGenScript = writeScriptBin "sbtix-gen" ''
     #! ${stdenv.shell}

    sbtix genNix
  '';

in
stdenv.mkDerivation {
  name = "sbtix-0.1";
  
  src = ./.;

  phases = [ "installPhase" ];

  installPhase =''
    mkdir -p $out/bin
    ln -s ${sbtixScript}/bin/sbtix $out/bin/.
    ln -s ${sbtixGenScript}/bin/sbtix-gen $out/bin/.
    ln -s ${sbtixRepo}/plugin-repo $out
    ln -s ${pluginsSbtix} $out/sbtix_plugin.sbt
  '';
}