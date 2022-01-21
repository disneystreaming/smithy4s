{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils, ... }@inputs:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        shellPackages = with pkgs; [
          jre
          sbt
          nodejs-14_x
          yarn
        ];
      in
      {
        devShell = pkgs.mkShell {
          buildInputs = shellPackages;
          welcomeMessage = ''

            Available packages: ${builtins.concatStringsSep "\n" (map (pkg : pkg.name) shellPackages)}
          '';

          shellHook = ''
            echo "$welcomeMessage"
          '';
        };
      }
    );
}
