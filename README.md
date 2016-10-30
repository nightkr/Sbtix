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
 * `sbtix-gen-all` - gen build and plugin dependencies, produces `repo-build.nix` and `repo-plugins.nix`. Alias: `sbtix genNix "reload plugins" genNix`
 * `sbtix-gen-all2` - gen build, plugin and pluginplugin dependencies, produces `repo-build.nix`, `repo-plugins.nix`, and `repo-plugins-plugins.nix`. Alias: `sbtix genNix "reload plugins" genNix "reload plugins" genNix`

### Creating a build

 * create default.nix. As shown below.

```
{ pkgs ? import <nixpkgs> {} }: with pkgs;
let
    sbtix = pkgs.callPackage ./sbtix.nix {};
in
    sbtix.buildSbtProject {
        name = "sbtix-example";
        src = ./.;
        repo = [ ./repo-build.nix
                 ./repo-plugins.nix
               ];
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

Q: Why do I get an assertion error when I try to generate a `repo.nix` files?

```
java.lang.AssertionError: assertion failed: ArrayBuffer((Dependency(org.scala-sbt:scripted-plugin,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/scripted-plugin/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/scripted-plugin/0.13.12/scripted-plugin-0.13.12.pom)), (Dependency(org.scala-sbt:sbt,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/sbt/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/sbt/0.13.12/sbt-0.13.12.pom)))
        at scala.Predef$.assert(Predef.scala:179)
        at se.nullable.sbtix.CoursierArtifactFetcher.buildNixProject(CoursierArtifactFetcher.scala:29)
        at se.nullable.sbtix.NixPlugin$$anonfun$genNixProjectTask$1.apply(NixPlugin.scala:21)
        at se.nullable.sbtix.NixPlugin$$anonfun$genNixProjectTask$1.apply(NixPlugin.scala:12)
        at scala.Function1$$anonfun$compose$1.apply(Function1.scala:47)
        at sbt.$tilde$greater$$anonfun$$u2219$1.apply(TypeFunctions.scala:40)
        at sbt.std.Transform$$anon$4.work(System.scala:63)
        at sbt.Execute$$anonfun$submit$1$$anonfun$apply$1.apply(Execute.scala:228)
        at sbt.Execute$$anonfun$submit$1$$anonfun$apply$1.apply(Execute.scala:228)
        at sbt.ErrorHandling$.wideConvert(ErrorHandling.scala:17)
        at sbt.Execute.work(Execute.scala:237)
        at sbt.Execute$$anonfun$submit$1.apply(Execute.scala:228)
        at sbt.Execute$$anonfun$submit$1.apply(Execute.scala:228)
        at sbt.ConcurrentRestrictions$$anon$4$$anonfun$1.apply(ConcurrentRestrictions.scala:159)
        at sbt.CompletionService$$anon$2.call(CompletionService.scala:28)
        at java.util.concurrent.FutureTask.run(FutureTask.java:266)
        at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:511)
        at java.util.concurrent.FutureTask.run(FutureTask.java:266)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
        at java.lang.Thread.run(Thread.java:745)
[error](*:genNixProject) java.lang.AssertionError: assertion failed: ArrayBuffer((Dependency(org.scala-sbt:scripted-plugin,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/scripted-plugin/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/scripted-plugin/0.13.12/scripted-plugin-0.13.12.pom)), (Dependency(org.scala-sbt:sbt,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/sbt/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/sbt/0.13.12/sbt-0.13.12.pom)))
[error] genNixProject task did not complete Incomplete(node=Some(ScopedKey(Scope(Select(ProjectRef(file:/home/cessationoftime/workspace/sbtix/plugin/project/,project)),Global,Global,Global),genNixProject)), tpe=Error, msg=None, causes=List(), directCause=Some(java.lang.AssertionError: assertion failed: ArrayBuffer((Dependency(org.scala-sbt:scripted-plugin,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/scripted-plugin/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/scripted-plugin/0.13.12/scripted-plugin-0.13.12.pom)), (Dependency(org.scala-sbt:sbt,0.13.12,default(compile),Set(),Attributes(jar,),false,true),List(not found: /home/cessationoftime/.ivy2/local/org.scala-sbt/sbt/0.13.12/ivys/ivy.xml, not found: https://repo1.maven.org/maven2/org/scala-sbt/sbt/0.13.12/sbt-0.13.12.pom))))) for project ProjectRef(file:/home/cessationoftime/workspace/sbtix/plugin/project/,project)
```

A: You likely need to add additional resolvers to your `build.sbt` or `project/plugins.sbt` before you can generate Nix expressions for it. 
  * Once [this Coursier issue](https://github.com/alexarchambault/coursier/issues/292) is closed we can move the additional resolvers into the sbtix plugin's global configuration.

These resolvers are usually what is needed.

```
resolvers += Resolver.typesafeIvyRepo("releases")

resolvers += Resolver.sbtPluginRepo("releases")

// if using PlayFramework
resolvers += Resolver.url("sbt-plugins-releases", url("https://dl.bintray.com/playframework/sbt-plugin-releases"))(Resolver.ivyStylePatterns) 
```
