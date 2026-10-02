# Runtime only: do not add application JAR, credentials or source environment.
FROM docker.io/library/eclipse-temurin@sha256:af3852c35f8339ab7beba6f17b7fc15c4aca4d72979c5f09776c9ff74385058f

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 \
    && test -x /opt/java/openjdk/bin/java \
    && ln -sf /opt/java/openjdk/bin/java /usr/bin/java \
    && test -x /usr/bin/python3 \
    && install -d -o 999 -g 982 -m 0700 /var/lib/pocketstats /home/app \
    && rm -rf /var/lib/apt/lists/*

LABEL org.opencontainers.image.title="PocketStats private Java17 Python runtime"
USER 999:982
WORKDIR /var/lib/pocketstats
ENTRYPOINT ["/usr/bin/python3", "-I"]
