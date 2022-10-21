# Build jar
FROM gradle:7.0.0-jdk8 AS build-env
ADD --chown=gradle:gradle . /app
WORKDIR /app
RUN \
    gradle assembleShadowDist; \
    mv ./build/libs/*.jar ./build/libs/reader.jar

FROM amazoncorretto:8u332-alpine3.14-jre
# Install base packages
RUN \
    # apk update; \
    # apk upgrade; \
    # Add CA certs tini tzdata
    apk add --no-cache ca-certificates tini tzdata; \
    update-ca-certificates; \
    # Clean APK cache
    rm -rf /var/cache/apk/*;

# 时区
ENV TZ=Asia/Shanghai

EXPOSE 9080
ENTRYPOINT ["/sbin/tini", "--"]
COPY --from=build-env /app/build/libs/reader.jar /app/bin/reader.jar
CMD ["java", "-jar", "/app/bin/reader.jar" ]
