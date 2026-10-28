# Publishing runbook

How the library gets from this repository to a consumer's build. The hard facts (coordinates, plugin, signing condition, workflow steps) are generated into [`../build-release.md`](../build-release.md); this page is the procedure.

## Coordinates and artifacts

- Maven coordinates `com.eskotech:luach:<version>`. The version is set once in `build.gradle.kts` (`version = "..."`) and mirrored by `Luach.VERSION` in `src/commonMain/kotlin/com/eskotech/luach/Luach.kt`; `VersionTest` (run by `jvmTest`) fails when they differ, so bump both.
- The root artifact `luach` carries Gradle module metadata that points at one artifact per target: `luach-jvm`, `luach-iosarm64`, `luach-iossimulatorarm64`, `luach-iosx64`, `luach-macosarm64`, `luach-macosx64`, `luach-linuxx64`, `luach-linuxarm64`, `luach-mingwx64`. A consumer declares only the root coordinate; Gradle resolves the variant.
- Each publication has a sources jar and an empty javadoc jar (`JavadocJar.Empty()`, since the sources are pure code), as Maven Central requires both.
- Publishing is configured by the `com.vanniktech.maven.publish` plugin; the POM name, description, license (GPL 3.0 only, with the App Store permission in `ADDITIONAL-PERMISSIONS.md`), developer and SCM blocks are in `build.gradle.kts`. The project and SCM URLs point at `https://github.com/Esko-Tech/Luach`.

## Local publishing

```
./gradlew publishToMavenLocal
ls ~/.m2/repository/com/eskotech/luach/
```

Signing is optional: `signAllPublications()` is applied only when the Gradle property `signingInMemoryKey` is present. On Linux or Windows the Apple targets publish metadata only; a macOS host publishes every target. A consumer resolves the local copy with `mavenLocal()` in its repositories.

## Maven Central setup (once)

1. Create an account on the Central Portal and claim the namespace `com.eskotech` (verified by a DNS TXT record on `eskotech.com`).
2. Generate a Portal user token: its username and password become the secrets `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`.
3. Create a GPG key for signing and publish its public key to a key server (`keys.openpgp.org` or `keyserver.ubuntu.com`). Export the private key in ASCII armor (`gpg --armor --export-secret-keys <key id>`); the whole text becomes the secret `SIGNING_KEY`, the key id (the last 8 or 16 hex digits) `SIGNING_KEY_ID`, and the passphrase `SIGNING_KEY_PASSWORD`.
4. Add the five secrets to the GitHub repository (Settings → Secrets and variables → Actions).

For publishing from a workstation instead, put the same values in `~/.gradle/gradle.properties` as `mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, `signingInMemoryKeyId` and `signingInMemoryKeyPassword` (the workflow passes them as `ORG_GRADLE_PROJECT_*` environment variables, which Gradle reads the same way).

## Releasing a version

1. Set the new version in `build.gradle.kts` and in `Luach.VERSION`; update the version in the README installation snippet.
2. Run `./gradlew allTests` locally (on macOS to cover the Apple simulators) and make sure CI is green.
3. Commit, tag `vX.Y.Z` and push the tag. `.github/workflows/release.yml` runs on the tag on a macOS runner: `./gradlew allTests`, then `./gradlew publishAndReleaseToMavenCentral --no-configuration-cache`, which uploads every target's artifacts, signs them, and releases the deployment on the Portal without manual confirmation.
4. The artifacts appear on Maven Central within a short time; the Portal shows the deployment's status. `workflow_dispatch` reruns the same job by hand.

A release runs on macOS because only a macOS host compiles the iOS and macOS targets.

## Consuming

```kotlin
// Kotlin Multiplatform
kotlin { sourceSets { commonMain.dependencies { implementation("com.eskotech:luach:0.1.0") } } }

// JVM or Android
dependencies { implementation("com.eskotech:luach:0.1.0") }
```

Transitive: `kotlinx-datetime` (declared `api`, so consumers get `LocalDate`, `DayOfWeek` and `TimeZone` on their compile classpath). The JVM artifact targets JVM 17 bytecode. On Android the JVM artifact is used; `kotlinx-datetime` relies on `java.time` there, so follow its Android requirements (core library desugaring below API level 26).
