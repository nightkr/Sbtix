{sbtix, one}:
sbtix.buildSbtLibrary {
    name = "sbtix-multibuild-two";
    src = ./.;
    repo = [ (import ./manual-repo.nix)
             (import ./repo.nix)
           ];
    sbtixBuildInputs = [ one ];
}
