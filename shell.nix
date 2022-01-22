# For compatibility with non-flake-enabled Nix versions
{ system ? builtins.currentSystem, ... }:
(
  import
    (
      fetchTarball {
        url = "https://github.com/edolstra/flake-compat/archive/b7547d3eed6f32d06102ead8991ec52ab0a4f1a7.tar.gz";
        sha256 = "09ln95rvvjxjsnmvzrvyc2ji2l5lz17s671z51f9z8cl4m23ndp2";
      }
    )
    { src = ./.; }
).shellNix
