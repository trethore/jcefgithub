FROM debian:bookworm-slim
RUN apt-get -q update && apt-get -q install -y --no-install-recommends \
    ca-certificates curl jq zip unzip git openjdk-17-jdk maven python3 gnupg && \
    rm -rf /var/lib/apt/lists/*
WORKDIR /workspace
COPY packaging packaging
COPY library library
COPY pom.xml LICENSE ./
RUN chmod +x packaging/scripts/*.sh
