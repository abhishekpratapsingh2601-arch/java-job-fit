FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/javajobfit-backend-0.0.1-SNAPSHOT.jar app.jar
ENV PORT=8080
# glibc malloc arenas default to 8x host cores; on a big Render node that bloats RSS in a
# 512MB cgroup. Two arenas is plenty for this thread count.
ENV MALLOC_ARENA_MAX=2
EXPOSE 8080
# Startup-tuned for Render free tier (0.1 CPU / 512MB):
#  - TieredStopAtLevel=1: C1-only JIT — much less compile work at boot, big startup win on
#    a throttled CPU (slightly lower peak throughput, irrelevant at this traffic level).
#  - MaxRAMPercentage=45 + explicit metaspace/code-cache caps: heap must leave room for
#    off-heap (metaspace, code cache, thread stacks, GC structures). 75% (384MB heap) was
#    SIGKILLed (exit 137) on wake-up on 5 Aug; 50% with uncapped metaspace still crept past
#    the 512MB cgroup limit once after ~13 days of wake/sleep cycles (20 Aug, exit 137).
#    45% heap (~230MB) + MaxMetaspaceSize=128m + ReservedCodeCacheSize=64m bounds worst-case
#    RSS near ~460MB. 64m code cache is ample for C1-only compilation.
#  - Xss512k: smaller thread stacks, more headroom in 512MB.
#  - spring.jmx.enabled=false: skip JMX bean export at boot.
CMD ["java", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:MaxRAMPercentage=45.0", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-Xss512k", \
  "-Dspring.jmx.enabled=false", \
  "-jar", "app.jar"]
