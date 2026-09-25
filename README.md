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

## Follow-up (not done yet)

- Set up this repo's own CI/CD pipeline (build, test, Docker image, push to a registry) —
  if it runs `mvn`, give the workflow `permissions: packages: read` and wire up
  `server-id: github` in its `actions/setup-java` step so it can resolve `common-libs`
  from GitHub Packages, same as `common-libs`' own `publish.yml` does for publishing.
- `Dockerfile` in this repo can be simplified since this is no longer a multi-module
  reactor — a plain single-project Docker build context now works.
