FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/javajobfit-backend-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
EXPOSE 8080
# Startup-tuned for Render free tier (0.1 CPU / 512MB):
#  - TieredStopAtLevel=1: C1-only JIT — much less compile work at boot, big startup win on
#    a throttled CPU (slightly lower peak throughput, irrelevant at this traffic level).
#  - MaxRAMPercentage: size the heap from the container limit instead of host RAM.
#  - Xss512k: smaller thread stacks, more headroom in 512MB.
#  - spring.jmx.enabled=false: skip JMX bean export at boot.
CMD ["java", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Xss512k", \
  "-Dspring.jmx.enabled=false", \
  "-jar", "app.jar"]
