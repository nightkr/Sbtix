{sbtix}:
sbtix.buildSbtLibrary {
    name = "sbtix-multibuild-one";
    src = ./.;
    repo = [ (import ./manual-repo.nix)
             (import ./repo.nix)
             (import ./project/repo.nix)
           ];
}
