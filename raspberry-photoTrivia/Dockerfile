FROM jsurf/rpi-java
RUN mkdir -p /photos
COPY photoTrivia-1.0-SNAPSHOT.jar app.jar
COPY application.properties application.properties
ENTRYPOINT ["java","-Xmx32m","-XX:+UseG1GC","-jar","/app.jar"]
