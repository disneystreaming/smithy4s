{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils, ... }@inputs:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        shellPackages = [
          "jre"
          "sbt"
          "nodejs"
          "yarn"
        ];
      in
      {
        devShell = pkgs.mkShell {
          buildInputs = map (pkgName: pkgs.${pkgName}) shellPackages;
          welcomeMessage = ''
            Welcome to the smithy4s Nix shell! 👋
            Available packages:
            ${builtins.concatStringsSep "\n" (map (n : "- ${n}") shellPackages)}
          '';

          shellHook = ''
            echo "$welcomeMessage"
          '';
        };
      }
    );
}
