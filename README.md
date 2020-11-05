# photoTrivia

photoTrivia  is a simple SELF-HOSTED photo gallery written in Java.
It was made to easily show your photos and videos without heavy preprocessing (creating lots of thumbnails, etc.)
especially on low-power machines like Raspberry Pi.

Prepare your folder with photos (base folder should contain single level folders with photos and videos
with no spaces in file/directory names), set the base folder as *albums.path* in *application.properties* and run

`java -jar photoTrivia-1.2-SNAPSHOT.jar -Xmx32M`

to start photoTrivia on port 8888. 

Just set port forwarding on your router and let the world see your galleries :)

It consumes *50+* megs of memory on my Raspberry Pi 3B using Oracle JDK 8.
The gallery works only with the file system. It does not create thumbnails,
does not scan exif on all photos in an album at once.
It only scans the directory you're in, and only scans exif of a single photo for rotation.

Use HOT KEYS to move through the gallery:

- *space/backspace* and *right/left arrow* keys to go forward/backwards
- *up arrow* to show all albums
- *f* to show photo/video fullscreen (please use *F11* before), *Esc* or *g* to exit fullscreen
- *l* to list photos in the current album.

SWIPE support will help you on tablet/phone:

- *right/left swipe* to move forward/backwards
- *up swipe* to show all albums
- swipe only works in fullscreen mode (please press the button before using swipe).
