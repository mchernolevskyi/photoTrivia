package com.makswinner.phototrivia.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Dr Maksym Chernolevskyi
 */
@RestController
@Configuration
public class PhotoController implements WebMvcConfigurer {
    @Value(value = "${albums.path}")
    private String albumsPathRaw;

    @Value(value = "${rendering.bgcolor}")
    private String renderingBgcolor;

    @Value(value = "${rendering.linkcolor}")
    private String renderingLinkcolor;

    @Value(value = "${rendering.vlinkcolor}")
    private String renderingVLinkcolor;

    @Value(value = "${extensions.ignore}")
    private String ignoreExtensionsRaw;

    @Value(value = "${extensions.video}")
    private String videoExtensionsRaw;

    private String albumsPath;
    private String baseGalleryDir;
    private List<String> ignoreExtensions = new LinkedList<>();
    private List<String> videoExtensions = new LinkedList<>();

    @PostConstruct
    private void init() {
        albumsPath = albumsPathRaw + File.separator;
        baseGalleryDir = getBaseGalleryDir(albumsPathRaw);
        ignoreExtensions.addAll(
                Arrays.asList(ignoreExtensionsRaw.split(",")));
        videoExtensions.addAll(
                Arrays.asList(videoExtensionsRaw.split(",")));
    }

    private String getBaseGalleryDir(String albumsPath) {
        return albumsPath.substring(albumsPath.lastIndexOf(File.separator) + 1);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + baseGalleryDir + "/**")
                .addResourceLocations("file:///" + albumsPath);
    }

    @RequestMapping("/")
    public String showAll() {
        StringBuilder albumsDescription = new StringBuilder();
        albumsDescription.append(getHeader());
        Arrays.asList(findAlbums()).stream().sorted().forEach(
                album -> albumsDescription.append(getAlbumLink(album)));
        return albumsDescription.toString();
    }

    @RequestMapping("/album/{album}")
    public String showAlbum(@PathVariable("album") String album) {
        //TODO show list of photos
        String photo = findNextPhoto(album, null);
        return renderImage(album, photo);
    }

    @RequestMapping("/photo/{album}/{photo}")
    public String showPhoto(@PathVariable("album") String album,
                            @PathVariable("photo") String photo) {
        return renderImage(album, photo);
    }

    private String [] findAlbums() {
        //TODO nested folders
        File file = new File(albumsPath);
        String[] directories = file.list(
                (current, name) -> new File(current, name).isDirectory());
        return directories;
    }

    private String [] findAlbumPhotos(String album) {
        File dir = new File(albumsPath + album);
        String[] files = dir.list((current, name) -> {
            File imageFile = new File(current, name);
            return imageFile.isFile()
                    && !ignoreExtensions.contains(getFilenameExtensionLowerCase(name));
        });
        return files;
    }

    private int getOrientation(File imageFile) {
        int orientation = 0;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            //JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (directory != null) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (MetadataException | IOException | ImageProcessingException e) {
            //silently swallow
        }
        return orientation;
    }

    private String getFilenameExtensionLowerCase(String name) {
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    private String findNextPhoto(String album, String current) {
        String[] files = findAlbumPhotos(album);
        List<String> photos = Arrays.asList(files).stream().sorted().collect(Collectors.toList());
        if (current == null) {
            return photos.get(0);
        } else {
            Iterator<String> iterator = photos.iterator();
            for (;iterator.hasNext();) {
                String file = iterator.next();
                if (file.equals(current) && iterator.hasNext())
                    return iterator.next();
            }
            return photos.get(0);
        }
    }

    private String findPreviousPhoto(String album, String current) {
        String[] files = findAlbumPhotos(album);
        List<String> photos = Arrays.asList(files).stream().sorted().collect(Collectors.toList());
        if (current == null) {
            return photos.get(0);
        } else {
            Iterator<String> iterator = photos.iterator();
            String previous = photos.get(0);
            for (;iterator.hasNext();) {
                String file = iterator.next();
                if (file.equals(current) && iterator.hasNext())
                    return previous;
                previous = file;
            }
            return photos.get(0);
        }
    }

    //PHP style on!

    private String renderImage(String album, String photo) {
        String previousPhoto = findPreviousPhoto(album, photo);
        String nextPhoto = findNextPhoto(album, photo);
        File imageFile = new File(albumsPath + album + File.separator + photo);
        int orientation = getOrientation(imageFile);
        return getHeader()
                + getMediaHTML(album, photo, orientation)
                + getPreviousPhotoLink(album, previousPhoto)
                + get10Nbsp() + get10Nbsp() + get10Nbsp()
                + getAllAlbumsLink()
                + get10Nbsp() + get10Nbsp() + get10Nbsp()
                + getNextPhotoLink(album, nextPhoto)
                + getJavaScript(album, previousPhoto, nextPhoto);
    }

    private String get10Nbsp() {
        return "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
    }

    private String getHeader() {
        return "<html><head><title>photoTrivia</title><head>"
                + "<body bgcolor=" + renderingBgcolor
                + " link=" + renderingLinkcolor
                + " vlink=" + renderingVLinkcolor
                + " onload=init()><center><br>";
    }

    private String getJavaScript(String album, String previousPhoto, String nextPhoto) {
        return "<script>"
                + "function init() {\n"
                + "  document.body.addEventListener(\"keydown\", function ee(event) {\n"
                + "if (event.keyCode === 32) {\n"
                + "window.location.replace(\"" + getPhotoUrl(album, nextPhoto) + "\");\n"
                + "}\n"
                + "if (event.keyCode === 37) {\n"
                + "window.location.replace(\"" + getPhotoUrl(album, previousPhoto) + "\");\n"
                + "}\n"
                + "if (event.keyCode === 39) {\n"
                + "window.location.replace(\"" + getPhotoUrl(album, nextPhoto) + "\");\n"
                + "}\n"
                + "if (event.keyCode === 38) {\n"
                + "window.location.replace(\"/\");\n"
                + "}\n"
                + "  });\n"
                + "}"
                + "</script>";
    }

    private String getPhotoUrl(String album, String photo) {
        return "/photo/" + album + "/" + photo;
    }

    private String getAllAlbumsLink() {
        return createLink("/", "Albums");
    }

    private String getAlbumLink(String album) {
        return createLink("/album/" + album, album) + "<br>";
    }

    private String getNextPhotoLink(String album, String nextPhoto) {
        return createLink(getPhotoUrl(album, nextPhoto),"Next&nbsp;&nbsp;&nbsp;>");
    }

    private String getPreviousPhotoLink(String album, String previousPhoto) {
        return createLink(getPhotoUrl(album, previousPhoto),"<&nbsp;&nbsp;&nbsp;Previous");
    }

    private String createLink(String url, String label) {
        return "<a href=\"" + url + "\">" + label + "</a>";
    }

    private String getMediaRealUrl(String album, String photo) {
        return baseGalleryDir + "/" + album + "/" + photo;
    }

    private String getMediaHTML(String album, String photo, int orientation) {
        if (videoExtensions.contains(getFilenameExtensionLowerCase(photo))) {
            return "<video height=90% controls=controls>"
                    + "<source src=/" + getMediaRealUrl(album, photo) + " type=video/mp4>"
                    + "</video><br>";
        }
        else {
            return "<img src=/" + getMediaRealUrl(album, photo)
                    + (orientation == 6 ? "  style=\"transform: rotate(90deg) scale(.67);\"" : "")
                    + (orientation == 8 ? "  style=\"transform: rotate(-90deg) scale(.67);\"" : "")
                    + " height=90%><br>";
        }
    }
}
