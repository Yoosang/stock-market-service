FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
# 1GB RAM 인스턴스에서 JVM이 무제한으로 메모리를 잡아 다른 컨테이너를 굶기지 않도록 힙을 제한한다.
ENTRYPOINT ["java", "-Xmx384m", "-jar", "app.jar"]
