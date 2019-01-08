# photoTrivia

photoTrivia  is a simple self-hosted photo gallery written in Java.
It was made to easily show your photos without heavy  processing especially on low-power machines like Raspberry Pi.

Prepare your folder with photos (base folder should contain single level folders with photos
with no spaces in file/directory names), set the base folder as *albums.path* in *application.properties* and run

`java -jar photoTrivia-1.0-SNAPSHOT.jar -Xmx32M`

to start photoTrivia on port 8888. 

Just set port forwarding on your router and let the world see your galleries :)

It consumes *90+* megs of memory on my Raspberry Pi 3B.
The gallery works only with the file system. It does not create thumbnails, does not scan exif on all photos in an album at once.
It only scans the directory you're in, and only scans exif of a single photo for rotation.

Use HOT KEYS to move through the gallery:

- *space* and *right/left arrow* keys to go forward/backwards
- *up arrow* to show albums
- *f* to show photo/video full screen (please use *F11* before), *Esc* to exit full screen
- *l* to list photos in the current album

Swipe support will help you on tablet/phone:

- right/left swipe to move forward/backwards
- up swipe to show albums
- swipe only works in full screen mode (press the link before)
