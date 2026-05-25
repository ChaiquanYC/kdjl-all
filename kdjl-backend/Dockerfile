FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY kdjl-common/pom.xml kdjl-common/
COPY kdjl-server/pom.xml kdjl-server/
COPY kdjl-admin/pom.xml kdjl-admin/
COPY kdjl-common/src kdjl-common/src
COPY kdjl-server/src kdjl-server/src
COPY kdjl-admin/src kdjl-admin/src
RUN mvn package -DskipTests -pl kdjl-common,kdjl-server,kdjl-admin

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/kdjl-server/target/*.jar server.jar
COPY --from=build /build/kdjl-admin/target/*.jar admin.jar
EXPOSE 8080 8081
CMD ["java", "-jar", "server.jar"]
