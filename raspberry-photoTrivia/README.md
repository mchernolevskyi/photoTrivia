# photoTrivia on Raspberry Pi in 2 minutes

We will now build docker image on Raspberry Pi and run it using docker compose.

1. Examine *appilcation.properties* file and change passwords in the last section,
do NOT change *albums.path* entry in this file
2. *chmod 755 start.sh*
3. Examine *docker-compose.yml* and change the FIRST part of the FIRST volume row to
the folder with your albums.
4. *./start.sh* - this will build docker image and start docker compose in detached mode
5. If for some reason you have carriage returns in your *start.sh* file (you will see some strange errors with \r)
then create the proper file with the following command:
*tr -d '\r' < start.sh > startFixed.sh* and then of course run *./startFixed.sh*

6. Point your browser to raspberry:8888, enjoy :)

P.S. jar and appilcation.properties files in this folder might be a bit outdated,
so please copy over from *target* folder after Maven build. Just wanted to make sure those who want to run it 
within 2 minutes are able to do it without JDK and Maven installed, all you need is docker and docker-compose.