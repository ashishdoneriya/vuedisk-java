# VueDisk Java

VueDisk Java is a Spring Boot based file server.

## Download latest release

The latest JAR and platform binaries built from the `main` branch are available here:

- [Download latest vuedisk.jar](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk.jar)
- [Download latest vuedisk-linux-x64](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-linux-x64)
- [Download latest vuedisk-linux-arm64](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-linux-arm64)
- [Download latest vuedisk-windows-x64.exe](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-windows-x64.exe)
- [Download latest vuedisk-windows-arm64.exe](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-windows-arm64.exe)
- [Download latest vuedisk-macos-silicon](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-macos-silicon)
- [Download latest vuedisk-macos-m1](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-macos-m1)
- [Download latest vuedisk-macos-intel](https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk-macos-intel)

These links are updated automatically by GitHub Actions whenever new code is pushed to `main`.

## Build locally

```bash
mvn clean package
```

The generated JAR is written to the `target` directory.

To build the static Linux x64 binary, use the GraalVM musl container:

```bash
docker run --rm \
  --entrypoint sh \
  -v "$PWD:/workspace" \
  -v "$HOME/.m2:/root/.m2" \
  -w /workspace \
  ghcr.io/graalvm/native-image-community:25-muslib \
  -lc "chmod +x mvnw && ./mvnw -B -Pnative -DskipTests native:compile"
```

The generated binary is written to `target/vuedisk`.
