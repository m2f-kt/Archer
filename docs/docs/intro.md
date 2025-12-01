---
sidebar_position: 1
---

# Getting Started with Archer

Welcome to **Archer** - a lightweight framework for **Functional Clean Architecture** (FCA) built on top of Arrow.

[![Coverage Status](https://coveralls.io/repos/github/m2f-kt/Archer/badge.svg?branch=main)](https://coveralls.io/github/m2f-kt/Archer?branch=main)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/com.m2f-kt/archer-core?color=4caf50&label=latest%20release)](https://central.sonatype.com/artifact/com.m2f-kt/archer-core)

## What is Archer?

Archer is designed to reduce the boilerplate typically associated with Clean Architecture while maintaining its core benefits:

- **Abstraction** - Clear separation of concerns
- **Reusability** - Composable components
- **Scalability** - Easy to extend and maintain

### The Problem with Traditional Clean Architecture

Clean Architecture offers great benefits, but it comes with significant tradeoffs:

- **Excessive Boilerplate** - Multiple interfaces and implementations for each layer
- **Maintainability Issues** - More code to maintain and update

A typical Clean Architecture implementation requires:
- UseCase interface + implementation
- Repository interface + implementation
- Multiple DataSource interfaces + implementations (network, local, etc.)
- Multiple Mappers for data transformation

### Archer's Solution

Archer provides **contractual DataSources and Repositories** that eliminate the need for creating all this boilerplate, while still maintaining the architectural benefits. You only need to implement the mapping logic - Archer handles the rest.

## Installation

Add Archer to your project:

```kotlin title="build.gradle.kts"
dependencies {
    implementation("com.m2f-kt:archer-core:{version}")
}
```

Replace `{version}` with the latest version from [Maven Central](https://central.sonatype.com/artifact/com.m2f-kt/archer-core).

## Quick Example

Here's a simple example of using Archer:

```kotlin
// Define a remote data source
val remoteDataSource = getDataSource<Int, String> { param: Int ->
    "$param"
}

// Define a local storage data source
val store: StoreDataSource<Int, String> = InMemoryDataSource()

// Create a repository strategy with caching
val repositoryStrategy = remoteDataSource cacheWith store expiresIn 5.minutes

// Use the repository with different result wrappers
val resultIce = ice {
    repositoryStrategy.get(StoreSync, 0)
}
// resultIce: Ice.Content("0")

val resultEither = either {
    repositoryStrategy.get(StoreSync, 0)
}
// resultEither: Either.Right("0")

val resultNullable = nullable {
    repositoryStrategy.get(StoreSync, 0)
}
// resultNullable: "0"
```

## Result Types

Archer supports multiple result types through Arrow:

- **Ice** (Idle | Content | Error) - Three-state result type
- **Either** - Classic functional error handling
- **Nullable** - Simple null-based error handling

## Next Steps

- [Core Concepts](/docs/core-concepts/clean-architecture) - Understand Clean Architecture with Archer
- [DataSources](/docs/usage/datasources) - Learn about data sources
- [Repositories](/docs/usage/repositories) - Explore repository patterns
- [Examples](/docs/examples/basic-usage) - See practical examples
