{sbtix, two}:
sbtix.buildSbtProgram {
    name = "sbtix-multibuild-three";
    src = ./.;
    repo = [ (import ./manual-repo.nix)
             (import ./repo.nix)
             (import ./project/repo.nix)
           ];
    sbtixBuildInputs = [ two ];
}
