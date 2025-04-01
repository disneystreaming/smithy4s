{
  inputs.nixpkgs.url = "github:nixos/nixpkgs";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { nixpkgs, flake-utils, ... }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };
        shellPackages = with pkgs; [
          temurin-bin-17
          nodejs-18_x
          yarn
          (pkgs.sbt.override { jre = pkgs.temurin-bin-17; })
        ];
        protobuf = pkgs.protobuf3_21;
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = shellPackages;
          nativeBuildInputs = [ pkgs.openssl pkgs.zlib protobuf ];
          welcomeMessage = ''
            Welcome to the smithy4s Nix shell! 👋
            Available packages:
            ${builtins.concatStringsSep "\n" (map (n : "- ${n.name}") shellPackages)}
          '';

          shellHook = ''
            echo "$welcomeMessage"
          '';
          PROTOC_PATH = pkgs.lib.getExe protobuf;
        };
      }
    );
}
