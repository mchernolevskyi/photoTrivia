FROM jsurf/rpi-java
RUN mkdir -p /photos
COPY photoTrivia-1.1-SNAPSHOT.jar app.jar
COPY application.properties application.properties
ENTRYPOINT ["java","-Xmx32m","-jar","/app.jar"]
