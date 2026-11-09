# lucida-servlet (Java)

A `jakarta.servlet.Filter` for the Lucida Java SDK. For each request it opens a `LucidaRequestContext`, so `capture` calls made while
handling it pick up the browser's distinct and session ids (`X-Lucida-Distinct-Id` / `X-Lucida-Session-Id`, sent by the browser SDK),
the URL, method, path, IP and user agent. Built against the Apache-2.0 `tomcat-servlet-api`.

## Install

From GitHub, [lucidaanalytics/lucida-java-servlet](https://github.com/lucidaanalytics/lucida-java-servlet), release `v0.1.0` (JitPack builds the tag; it needs the core SDK too):

```kotlin
// build.gradle.kts
repositories { maven("https://jitpack.io") }
dependencies {
    implementation("com.github.lucidaanalytics:lucida-java:v0.1.0")
    implementation("com.github.lucidaanalytics:lucida-java-servlet:v0.1.0") {
        exclude(group = "io.github.lucidaanalytics", module = "lucida") // the core comes from JitPack above
    }
}
```

In Maven, add the JitPack repository, both dependencies and an `<exclusion>` of `io.github.lucidaanalytics:lucida` on the servlet one. Maven Central (`io.github.lucidaanalytics:lucida-servlet`) is coming.

## Use

Register `dev.lucida.servlet.LucidaFilter` once, in front of your handlers (`FilterRegistrationBean`, `web.xml` or `@WebFilter`):

```java
@Bean
FilterRegistrationBean<LucidaFilter> lucidaFilter() {
    return new FilterRegistrationBean<>(new LucidaFilter(false)); // true only behind a proxy that overwrites X-Forwarded-For
}
```

`new LucidaFilter(trustProxy)`: with `trustProxy` the first `X-Forwarded-For` hop is used for `$ip`. The tracing headers are
client-controlled: use them to stitch analytics, never for authentication.

---

This repository is a read-only mirror of the Lucida monorepo (synced by a script, never edited by hand); releases and provenance attestations are built here by GitHub Actions. Issues are welcome, changes land upstream.
