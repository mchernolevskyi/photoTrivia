# photoTrivia
Simple few class self-hosted photo gallery using Spring Boot (Java)

Simple and easy to use expecially on low-power machines like Raspberry Pi.
Type

java -jar photoTrivia-1.0-SNAPSHOT.jar -Xmx32M

to start server. 

It consumes 90+ megs of memory on my RPi 3B. It does not create thumbnails, does not scan exif on all photos in an album.
It only scans the directory you're in, and only scans exif of a single photo for rotation.
