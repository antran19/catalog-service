# catalog-service

Product/Category catalog service for Project Nexus — category & product CRUD, SKUs,
Postgres full-text search, and product discovery. Extracted from the original monorepo
into its own standalone project, per the course's polyrepo requirement (each microservice
its own repo, its own independent Spring Boot project).

## Prerequisites

This project resolves `com.nexus:common-*` (version `1.0.0`) from
[antran19/common-libs](https://github.com/antran19/common-libs)'s GitHub Packages
registry, declared in `pom.xml`'s `<repositories>` block. Reading GitHub Packages requires
authentication even for a public repo — add a GitHub Personal Access Token
(`read:packages` scope) to your own `~/.m2/settings.xml` once:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

(Alternative for local dev without a token: clone `common-libs` and run
`mvn clean install` there — Maven checks your local `~/.m2` cache before reaching out to
GitHub Packages, so that works too.)

## Build & test

```bash
mvn clean package
```

Runs the full test suite, including Testcontainers-backed integration tests — Docker must
be running.

## Run

```bash
java -jar target/catalog-service-0.1.0-SNAPSHOT.jar
```

Expects a running Eureka discovery-server to register with
(`eureka.client.serviceUrl.defaultZone` in `src/main/resources/application.yml`) and a
Postgres database — see the original monorepo's `docker-compose.yml` for a working local
setup (`discovery-server`, `api-gateway`, `postgres-catalog`, `kafka`) to run this service
against locally, until this project's own infrastructure/compose setup exists.

## Docker

`Dockerfile` copies a pre-built jar (`mvn clean package` first, then `docker build`) —
simpler than the old monorepo version, since there's no reactor to build from a root
context anymore. See the `infra` repo for the docker-compose setup that runs the whole
cluster.

## Follow-up (not done yet)

- Push a built image to a registry (e.g. GHCR) from CI, instead of building it fresh
  locally every time.
