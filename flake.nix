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
          "nodejs-18_x"
          "yarn"
        ];
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = map (pkgName: pkgs.${pkgName}) shellPackages;
          nativeBuildInputs = [ pkgs.openssl pkgs.zlib pkgs.protobuf3_21 ];
          welcomeMessage = ''
            Welcome to the smithy4s Nix shell! 👋
            Available packages:
            ${builtins.concatStringsSep "\n" (map (n : "- ${n}") shellPackages)}
          '';

          shellHook = ''
            echo "$welcomeMessage"
            # TODO use pkgs.lib.getExe once nixpkgs are updated
            export PROTOC_PATH=${pkgs.protobuf3_21}/bin/protoc
          '';
        };
      }
    );
}
