# Contributing to Luach

Bug reports, test cases, corrections to the halachic rules and new features are all welcome. Open an issue
first for anything larger than a small fix, so the design can be agreed before you write it.

## Contributor License Agreement

Every contribution is licensed to Esko Tech Inc. under the [Contributor License Agreement](CLA.md), which
keeps the licensing of the whole library in one place. Please read it before your first pull request.

Contributing in any way means you accept it.

The agreement covers your own work. For code from elsewhere, say so in the pull request, with its source
and license, and we will decide whether it can be included.

## Working on the code

- Everything lives in `src/commonMain` as common Kotlin, with `kotlinx-datetime` as the one dependency.
- The sources are pure code below the license header at the top of each file. Names and the pages
  under [`docs/`](docs/README.md) carry the explanation, so update the page that describes the code you changed.
- Follow the conventions of the surrounding code.

## Tests

```
./gradlew jvmTest
./gradlew allTests
```

`jvmTest` runs the unit tests and the snapshot tests. `./gradlew apiCheck` compares the public API with `api/luach.api`; after an intended API change run `./gradlew apiDump` and commit the dump. When a change alters results on purpose, regenerate the
snapshots with `./gradlew jvmTest -PupdateSnapshots`, review the diff and explain it in the pull request. CI
runs the tests on Linux, macOS, the iOS simulator and Windows for every pull request.

## Documentation

`python3 docs/generate.py` regenerates the generated reference pages and tables; the hand-written pages are
edited directly. The generated files are also refreshed automatically on the default branch, so you may
commit them or leave them to the workflow.
