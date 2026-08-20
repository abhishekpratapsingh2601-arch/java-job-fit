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
#  - FIXED -Xmx, never MaxRAMPercentage: Render's sandbox (gVisor) can hide the cgroup
#    memory limit from the JVM, so percentage-based sizing computes against HOST RAM and the
#    heap thinks it may grow to many GB. Evidence: a build that boots at 155MB RSS under a
#    local docker -m 512m was exit-137'd 45s into boot on Render (20 Aug, mid-Hibernate init)
#    with MaxRAMPercentage=45 — impossible unless the percentage was misresolved. Every 137
#    since 5 Aug fits this: percentage caps (75/50/45) only held when boot allocation happened
#    to stay under 512MB naturally. A hard -Xmx cannot be misread by any runtime.
#  - Worst-case RSS budget: 192m heap + 128m metaspace + 64m code cache + ~25m thread stacks
#    + JVM/native overhead ~= 460MB in a 512MB cgroup. App served 8 analyses + 3 extracts in
#    175MB total RSS locally, so 192m heap is ample at beta traffic.
#  - Xss512k: smaller thread stacks, more headroom in 512MB.
#  - spring.jmx.enabled=false: skip JMX bean export at boot.
CMD ["java", \
  "-XX:TieredStopAtLevel=1", \
  "-Xms64m", \
  "-Xmx192m", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-Xss512k", \
  "-Dspring.jmx.enabled=false", \
  "-jar", "app.jar"]
