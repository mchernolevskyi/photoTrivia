package com.makswinner.phototrivia.controller;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Dr Maksym Chernolevskyi
 */
@RestController
@Configuration
public class PhotoController implements WebMvcConfigurer {
    @Value(value = "${albums.path}")
    private String albumsPathRaw;

    @Value(value = "${rendering.title}")
    private String renderingTitle;

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

    private static final int MEDIA_HEIGHT = 93;
    private static final int MEDIA_HEIGHT_FULLSCREEN = 98;

    private static final String PHOTO_TEMPLATE = "photo";
    private static final String ALBUMS_TEMPLATE = "albums";

    private static final String URL_ALL_ALBUMS = "/";

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
        return renderAlbums(findAlbums());
    }

    @RequestMapping("/album/{album}")
    public String showAlbum(@PathVariable("album") String album, HttpServletResponse response) {
        //TODO show list of photos
        try {
            String photo = findNextPhoto(album, null);
            return renderImage(album, photo, false);
        } catch (Exception e) {
            try {
                response.sendRedirect("/");//fallback
            } catch (IOException e1) {
                //silently swallow
            }
            return null;
        }
    }

    @RequestMapping("/photo/{album}/{photo}")
    public String showPhoto(@PathVariable("album") String album,
                            @PathVariable("photo") String photo,
                            @RequestParam(value = "fullScreen", required = false) boolean fullScreen) {
        return renderImage(album, photo, fullScreen);
    }

    private List<String> findAlbums() {
        //TODO nested folders
        File file = new File(albumsPath);
        String[] directories = file.list(
                (current, name) -> new File(current, name).isDirectory());
        return directories != null && directories.length > 0 ?
                Arrays.asList(directories).stream().sorted().collect(Collectors.toList()) : new ArrayList<>();
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
        //TODO proper scaling, extract height width and divide
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

    private String renderAlbums(List<String> albums) {
        StringBuilder albumsDescription = new StringBuilder();
        albumsDescription.append(getHeader());
        albums.forEach(album -> albumsDescription.append(getAlbumLink(album)));
        return albumsDescription.toString();
    }

    private String renderImage(String album, String photo, boolean fullScreen) {
        String previousPhoto = findPreviousPhoto(album, photo);
        String nextPhoto = findNextPhoto(album, photo);
        File imageFile = new File(albumsPath + album + File.separator + photo);
        int orientation = getOrientation(imageFile);
        String mediaRealUrl = getMediaRealUrl(album, photo);
        int mediaHeight = fullScreen ? MEDIA_HEIGHT_FULLSCREEN : MEDIA_HEIGHT;
        String mediaStyle = getMediaStyle(orientation);

        boolean video = videoExtensions.contains(getFilenameExtensionLowerCase(photo));

        String template = getTemplate(PHOTO_TEMPLATE);
        return template
                .replace("%(title)", renderingTitle)
                .replace("%(bgcolor)", renderingBgcolor)
                .replace("%(linkcolor)", renderingLinkcolor)
                .replace("%(vlinkcolor)", renderingVLinkcolor)
                .replace("%(mediaRealUrl)", "/" + mediaRealUrl)
                .replace("%(mediaStyle)", mediaStyle)
                .replace("%(mediaHeight)", "" + mediaHeight)
                .replace("%(previousMediaUrl)", getPhotoUrl(album, previousPhoto, fullScreen))
                .replace("%(mediaUrlFullScreen)", getPhotoUrl(album, photo, true))
                .replace("%(allAlbumsUrl)", URL_ALL_ALBUMS)
                .replace("%(nextMediaUrl)", getPhotoUrl(album, nextPhoto, fullScreen))
                .replace("%(mediaUrlNoFullScreen)", getPhotoUrl(album, photo, false))
                .replace("%(commentNotFullScreen)", fullScreen ? "" : "//")
                .replace("%(commentFullScreenHtmlStart)", fullScreen ? "<!--" : "")
                .replace("%(commentFullScreenHtmlEnd)", fullScreen ? "-->" : "")
                .replace("%(commentIfVideoHtmlStart)", video ? "<!--" : "")
                .replace("%(commentIfVideoHtmlEnd)", video ? "-->" : "")
                .replace("%(commentIfPhotoHtmlStart)", !video ? "<!--" : "")
                .replace("%(commentIfPhotoHtmlEnd)", !video ? "-->" : "");
    }

    String getMediaStyle(int orientation) {
        if (orientation == 6)
            return "style=\"transform: rotate(90deg) scale(.67);\"";
        if (orientation == 8)
            return "style=\"transform: rotate(-90deg) scale(.67);\"";
        if (orientation == 3)
            return "style=\"transform: rotate(180deg);\"";
        return "";
    }

    private String getTemplate(String template) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource("template" + File.separator + template + ".html").getInputStream(),
                    Charset.defaultCharset());
        } catch (IOException e) {
            return "";//silently swallow
        }
    }

    private String getHeader() {
        return "<html><head>"
                + "<title>" + renderingTitle + "</title></head>"
                + "<body bgcolor=" + renderingBgcolor
                + " link=" + renderingLinkcolor
                + " vlink=" + renderingVLinkcolor
                + " onload=init()><center>\n";
    }

    private String getPhotoUrl(String album, String photo, boolean fullScreen) {
        return "/photo/" + album + "/" + photo + (fullScreen ? "?fullScreen=true" : "");
    }

    private String getAlbumLink(String album) {
        return createLink("/album/" + album, album) + "<br>";
    }

    private String createLink(String url, String label) {
        return "<a href=\"" + url + "\">" + label + "</a>";
    }

    private String getMediaRealUrl(String album, String photo) {
        return baseGalleryDir + "/" + album + "/" + photo;
    }

}
