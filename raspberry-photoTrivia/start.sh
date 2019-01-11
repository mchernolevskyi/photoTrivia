cd ..
sudo docker build raspberry-photoTrivia/ --tag makswinner/phototrivia:1.0-SNAPSHOT
cd raspberry-photoTrivia
sudo docker-compose up -d
