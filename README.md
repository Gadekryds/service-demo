# Build

``` cmd
mvn -Pnative spring-boot:build-image

```

# Run

``` cmd
docker run --rm -p 8080:8080 docker.io/library/myproject:0.0.1-SNAPSHOT
```
