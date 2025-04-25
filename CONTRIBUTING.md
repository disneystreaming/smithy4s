## Development environment

For the easiest setup of development tools, use [Nix](https://nixos.org).

The recommended way is to use `nix develop` (requires Flakes support, available since Nix 2.4 - read on if you don't use that experimental feature):

```bash
nix develop
```

This will load all required packages into your shell. Run `exit` or press `ctrl+d` to clear it.

If you're a [direnv](https://github.com/nix-community/nix-direnv) user, we have that too.

If you don't have Flakes support:

```bash
nix-shell
```

## Note for metals/bloop users

Smithy4s is a complex project with a heavy build-matrix. In order to ease development, we've elected to only enable bloop-config generation
for the `JVM/Scala 2.13` combo of build axes, by default.

If you find yourself developing for another combination of build axes, it is possible to tweak the default by adding a `user.sbt` file in the root directory of your clone of this project, and fill it by following this example :

```scala
ThisBuild / bloopAllowedCombos := Seq(
  Seq(
    VirtualAxis.jvm,
    VirtualAxis.scalaABIVersion("3.3.0")
  )
)
```

## Note for .sbtopts

You usually should use `.sbtopts` to add some more memory for `sbt`, as Smithy4s is complex. You copy the `.sbtopts.example` to `.sbtopts` and adjust the values to your needs:

```bash
cp .sbtopts.example .sbtopts
```

## Forward-porting PRs to the latest series

At some times, we'll have more than one "main" branches. At the time of writing, `series/0.18` is the "current" branch, where we merge all the changes that don't break backward compatibility. We also have a `series/0.19` branch, where breaking changes are allowed - until `0.19.0` gets released.

In order to make sure all changes from 0.18.x end up on the 0.19.x branch, we have a Mergify rule that creates forward-port PRs. The rule is configured in [.mergify.yml](./.mergify.yml), at the time of writing it gets triggered on all **merged** PRs that have the `forwardport-0.19` label.
