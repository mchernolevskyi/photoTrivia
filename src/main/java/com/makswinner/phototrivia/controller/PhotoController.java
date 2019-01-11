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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private final List<String> ignoreExtensions = new LinkedList<>();
    private final List<String> videoExtensions = new LinkedList<>();
    private String photoTemplateWithHeader;
    private String albumsTemplateWithHeader;
    private String albumPhotosTemplateWithHeader;

    private static final int MEDIA_HEIGHT = 92;
    private static final int MEDIA_HEIGHT_FULLSCREEN = 98;
    private static final String URL_ALL_ALBUMS = "/";
    private static final String STYLE_TRANSFORM_ROTATE_SCALE = "style=\"transform: rotate(%sdeg) scale(%s);\"";
    private static final Map<String, List<String>> ALBUM_PHOTOS = new HashMap<>();

    @PostConstruct
    private void init() {
        albumsPath = albumsPathRaw + File.separator;
        baseGalleryDir = getBaseGalleryDir(albumsPathRaw);
        ignoreExtensions.addAll(Arrays.asList(ignoreExtensionsRaw.split(",")));
        videoExtensions.addAll(Arrays.asList(videoExtensionsRaw.split(",")));
        photoTemplateWithHeader = getTemplateWithHeader("template/photo.html");
        albumsTemplateWithHeader = getTemplateWithHeader("template/albums.html");
        albumPhotosTemplateWithHeader = getTemplateWithHeader("template/albumPhotos.html");
    }

    private String getBaseGalleryDir(String albumsPath) {
        return albumsPath.substring(albumsPath.lastIndexOf(File.separator) + 1);
    }

    private String getTemplate(String templatePath) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(templatePath).getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            return "";//silently swallow
        }
    }

    private String getTemplateWithHeader(String templatePath) {
        return getTemplate(templatePath)
                .replace("%(title)", renderingTitle)
                .replace("%(bgcolor)", renderingBgcolor)
                .replace("%(linkcolor)", renderingLinkcolor)
                .replace("%(vlinkcolor)", renderingVLinkcolor);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + baseGalleryDir + "/**")
                .addResourceLocations("file:///" + albumsPath);
    }

    @RequestMapping(URL_ALL_ALBUMS)
    public String showAll() {
        return renderAlbums();
    }

    @RequestMapping("/album/{album}")
    public String showAlbum(@PathVariable("album") String album,
                            @RequestParam(value = "list", required = false) boolean list,
                            HttpServletResponse response) {
        try {
            if (!list) {
                String photo = findNextPhoto(album, null);
                response.sendRedirect(getPhotoUrl(album, photo, false));
                return null;
            } else {
                return renderAlbumPhotos(album);
            }
        } catch (Exception e) {
            try {
                response.sendRedirect(URL_ALL_ALBUMS);//fallback
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
        return renderPhoto(album, photo, fullScreen);
    }

    private List<String> findAlbums() {
        //TODO nested folders
        String[] directories = new File(albumsPath).list(
                (current, name) -> new File(current, name).isDirectory());
        return directories != null && directories.length > 0 ?
                Arrays.asList(directories).stream().sorted().collect(Collectors.toList()) : new LinkedList<>();
    }

    private List<String> findAlbumPhotos(String album) {
        List<String> photos = ALBUM_PHOTOS.get(album);
        if (photos != null) {
            return photos;
        } else {
            File dir = new File(albumsPath + album);
            String[] files = dir.list((current, name) -> {
                File imageFile = new File(current, name);
                return imageFile.isFile()
                        && !ignoreExtensions.contains(getFilenameExtensionLowerCase(name));
            });
            photos = Arrays.asList(files).stream().sorted().collect(Collectors.toList());
            ALBUM_PHOTOS.put(album, photos);
            return photos;
        }
    }

    private int getOrientation(String album, String photo) {
        //TODO proper scaling, extract height width and divide
        File imageFile = new File(albumsPath + album + File.separator + photo);
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
        List<String> photos = findAlbumPhotos(album);
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
        List<String> photos = findAlbumPhotos(album);
        if (current == null) {
            return photos.get(0);
        } else {
            Iterator<String> iterator = photos.iterator();
            String previous = photos.get(0);
            for (;iterator.hasNext();) {
                String file = iterator.next();
                if (file.equals(current))
                    return previous;
                previous = file;
            }
            return photos.get(0);
        }
    }

    private String renderAlbums() {
        return albumsTemplateWithHeader.replace("%(albums)", getAlbumsDescription());
    }

    private String getAlbumsDescription() {
        StringBuilder description = new StringBuilder();
        findAlbums().forEach(album -> description.append(getAlbumLink(album)));
        return description.toString();
    }

    private String renderAlbumPhotos(String album) {
        return albumPhotosTemplateWithHeader.replace("%(photos)", getAlbumPhotosDescription(album));
    }

    private String getAlbumPhotosDescription(String album) {
        StringBuilder description = new StringBuilder();
        findAlbumPhotos(album).forEach(photo -> description.append(getPhotoLink(album, photo)));
        return description.toString();
    }

    private String renderPhoto(String album, String photo, boolean fullScreen) {
        int mediaHeight = fullScreen ? MEDIA_HEIGHT_FULLSCREEN : MEDIA_HEIGHT;
        boolean video = videoExtensions.contains(getFilenameExtensionLowerCase(photo));
        return photoTemplateWithHeader
                .replace("%(mediaRealUrl)", getMediaRealUrl(album, photo))
                .replace("%(mediaStyle)", getMediaStyle(getOrientation(album, photo)))
                .replace("%(mediaHeight)", "" + mediaHeight)
                .replace("%(previousMediaUrl)", getPhotoUrl(album, findPreviousPhoto(album, photo), fullScreen))
                .replace("%(mediaUrlFullScreen)", getPhotoUrl(album, photo, true))
                .replace("%(nextMediaUrl)", getPhotoUrl(album, findNextPhoto(album, photo), fullScreen))
                .replace("%(mediaUrlNoFullScreen)", getPhotoUrl(album, photo, false))
                .replace("%(albumPhotos)", getAlbumPhotosUrl(album))
                .replace("%(allAlbumsUrl)", URL_ALL_ALBUMS)
                .replace("%(commentIfNotFullScreenJavaScript)", fullScreen ? "" : "//")
                .replace("%(commentIfFullScreenHtmlStart)", fullScreen ? "<!--" : "")
                .replace("%(commentIfFullScreenHtmlEnd)", fullScreen ? "-->" : "")
                .replace("%(commentIfVideoHtmlStart)", video ? "<!--" : "")
                .replace("%(commentIfVideoHtmlEnd)", video ? "-->" : "")
                .replace("%(commentIfPhotoHtmlStart)", !video ? "<!--" : "")
                .replace("%(commentIfPhotoHtmlEnd)", !video ? "-->" : "");
    }

    private String getPhotoUrl(String album, String photo, boolean fullScreen) {
        return "/photo/" + album + "/" + photo + (fullScreen ? "?fullScreen=true" : "");
    }

    private String getAlbumPhotosUrl(String album) {
        return "/album/" + album + "?list=true";
    }

    private String getMediaRealUrl(String album, String photo) {
        return "/" + baseGalleryDir + "/" + album + "/" + photo;
    }

    private String getAlbumLink(String album) {
        return "<tr><th scope=\"row\"><a href=\"/album/" + album + "\">" + album + "</a></th></tr>\n";
    }

    private String getPhotoLink(String album, String photo) {
        return "<tr><th scope=\"row\"><a href=\"" + getPhotoUrl(album, photo, false)
                + "\">" + photo + "</a></th></tr>\n";
    }

    private String getMediaStyle(int orientation) {
        if (orientation == 6)
            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "90", ".67");
        if (orientation == 8)
            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "-90", ".67");
        if (orientation == 3)
            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "180", "1.0");
        return "";
    }
}
