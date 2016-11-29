# Sbtix

[![Build Status](https://travis-ci.org/teozkr/Sbtix.svg?branch=master)](https://travis-ci.org/teozkr/Sbtix)

## What?

Sbtix generates a Nix definition that represents your SBT project's dependencies. It then uses this to build a Maven repo containing the stuff your project needs, and feeds it back to your SBT build.

## Why?

Currently, this should mean that you won't have to redownload the world for each rebuild.

Additionally, this means that Nix can do a better job of enforcing purity where required. Ideally the build script itself should not communicate with the outer world at all, since otherwise Nix does not allow proxy settings to propagate.

* Private (password-protected) Maven stores are supported.
* Ivy repositories and plugins are cached by Sbtix.

## Why not? (caveats)

* Alpha quality, beware (and please report any issues!)
* Nix file for SBT compiler interface dependencies currently must be created manually.
* You must use the Coursier dependency resolver instead of Ivy (because SBT's Ivy resolver does not report the original artifact URLs)

## How?

To install sbtix clone the sbtix git repo and run the following:
```
cd Sbtix
nix-env -f . -i sbtix
```

Sbtix provides a script which will connect your project to the sbtix global plugin and launch sbt, it does this by setting the `sbt.global.base` directory to `$HOME/.sbtix`.  

### Sbtix commands

 * `sbtix` - loads the sbtix global plugin and launches sbt. Sets `sbt.global.base` directory to `$HOME/.sbtix`.
 * `sbtix-gen` - gen build dependencies only, produces `repo.nix`. Alias: `sbtix genNix`.
 * `sbtix-gen-all` - gen build and plugin dependencies, produces `repo.nix` and `project/repo.nix`. Alias: `sbtix genNix "reload plugins" genNix`
 * `sbtix-gen-all2` - gen build, plugin and pluginplugin dependencies, produces `repo.nix`, `project/repo.nix`, and `project/project/repo.nix`. Alias: `sbtix genNix "reload plugins" genNix "reload plugins" genNix`

### Creating a build

 * create default.nix. As shown below. Edit as necessary.

```
{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-example";
        src = ./.;
        repo = [ (import ./manual-repo.nix)
                 (import ./repo.nix)
                 (import ./project/repo.nix)
               ];

        installPhase =''
          sbt publish-local
          mkdir -p $out/
          cp ./.ivy2/local/* $out/ -r
        '';
    }
```

 * copy `manual-repo.nix` and `sbtix.nix` from the root of the sbtix git repository
 * generate your repo.nix files with one of the commands listed above. `sbtix-gen-all` is recommended.
 * check the generated nix files into your source control. 
 * finally, run `nix-build` to build!
 * any additional missing dependencies that `nix-build` encounters should be fetched with `nix-prefetch-url` and added to `manual-repo.nix`.

### Authentication

In order to use a private repository, add your credentials to `coursierCredentials`. Note that the key should be the name of the repository, see `plugin/src/sbt-test/sbtix/private-auth/build.sbt` for an example! Also, you must currently set the credentials for each project, `in ThisBuild` doesn't work currently. This is for consistency with Coursier-SBT.

### FAQ

Q: Why I am getting errors trying to generate `repo.nix` files when using the PlayFramework?

A: You probably need to add the following resolver to your project for Sbtix to find.

```
// if using PlayFramework
resolvers += Resolver.url("sbt-plugins-releases", url("https://dl.bintray.com/playframework/sbt-plugin-releases"))(Resolver.ivyStylePatterns) 
```

Q: When I `nix-build` it sbt complains `java.io.IOException: Cannot run program "git": error=2, No such file or directory`

A: You are likely depending on a project via git.  This isn't recommended usage for sbtix since it leads to non-deterministic builds. However you can enable this by making two small changes to sbtix.nix, in order to make git a buildInput.

top of sbtix.nix with git as buildinput
```
{ runCommand, fetchurl, lib, stdenv, jdk, sbt, writeText, git }:
```

bottom of sbtix.nix with git as buildinput
```
buildInputs = [ jdk sbt git ] ++ buildInputs;
```


