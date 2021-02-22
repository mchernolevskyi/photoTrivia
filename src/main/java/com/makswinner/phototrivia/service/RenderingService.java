package com.makswinner.phototrivia.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.makswinner.phototrivia.config.SecurityConfig.ROLE_ADMIN;
import static com.makswinner.phototrivia.config.SecurityConfig.ROLE_GUEST;

/**
 * @author Dr Maksym Chernolevskyi
 */
@Service
@Configuration
public class RenderingService implements WebMvcConfigurer {
    private static final String ROLE_PREFIX = "ROLE_";
    private static final int MEDIA_HEIGHT = 90;
    private static final int MEDIA_HEIGHT_FULLSCREEN = 100;
    //private static final String STYLE_TRANSFORM_ROTATE_SCALE = "transform: rotate(%sdeg) scale(%s);";

    private static final Map<String, List<String>> ALBUM_PHOTOS = new ConcurrentHashMap<>();

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

    //TODO @Value("#{'${extensions.video}'.split(',')}")
    @Value(value = "${extensions.video}")
    private String videoExtensionsRaw;

    @Value(value = "${albums.path}")
    private String albumsPathRaw;

    private String albumsPath;
    private String baseGalleryDir;
    private String photoTemplateWithHeader;
    private String albumsTemplateWithHeader;
    private String albumPhotosTemplateWithHeader;
    private final List<String> ignoreExtensions = new LinkedList<>();
    private final List<String> videoExtensions = new LinkedList<>();

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

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/" + baseGalleryDir + "/**")
                .addResourceLocations("file:///" + albumsPath);
    }

    private String getTemplateWithHeader(String templatePath) {
        return getTemplate(templatePath)
                .replace("%(title)", renderingTitle)
                .replace("%(bgcolor)", renderingBgcolor)
                .replace("%(linkcolor)", renderingLinkcolor)
                .replace("%(vlinkcolor)", renderingVLinkcolor);
    }

    private String getTemplate(String templatePath) {
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(templatePath).getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            return "";//silently swallow
        }
    }

    private String getTemplateLine(String data, String cycleVariable) {
        String[] splitData = data.split(System.getProperty("line.separator"));
        for (String eachSplit : splitData) {
            if (eachSplit.contains(cycleVariable)) {
                return eachSplit;
            }
        }
        return "";
    }

    public String renderAlbums() {
        String templateLine = getTemplateLine(albumsTemplateWithHeader, "%(cycle:albums)");
        String templateRow = templateLine.replace("%(cycle:albums)", "");
        return albumsTemplateWithHeader.replace(templateLine, getAlbumsDescription(templateRow));
    }

    private String getAlbumsDescription(String templateRow) {
        List<String> albums = getAlbums();
        StringBuilder description = new StringBuilder();
        albums.forEach(album -> description.append(getAlbumRow(templateRow, album)));
        return description.toString();
    }

    private String getAlbumRow(String templateRow, String album) {
        return templateRow.replace("%(album)", album) + "\n";
    }

    private List<String> getAlbums() {
        UserDetails userDetails =
                (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userRole = userDetails.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith(ROLE_PREFIX))
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse(ROLE_GUEST);
        String albumsRaw = userDetails.getAuthorities().stream()
                .filter(authority -> !authority.getAuthority().startsWith(ROLE_PREFIX))
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse("");
        List<String> allowedAlbums = Arrays.stream(albumsRaw.split(",")).collect(Collectors.toList());
        List<String> albums;
        if ((ROLE_ADMIN).equals(userRole)) {
            albums = findAllAlbums();
        } else if ((ROLE_GUEST).equals(userRole)) {
            albums = findAlbumsForUser(allowedAlbums);
        } else {
            albums = new ArrayList<>();
        }
        return albums;
    }

    public String renderAlbumPhotos(String album) {
        String templateLine = getTemplateLine(albumPhotosTemplateWithHeader, "%(cycle:photos)");
        String templateRow = templateLine.replace("%(cycle:photos)", "");
        return albumPhotosTemplateWithHeader.replace(templateLine, getAlbumPhotosDescription(templateRow, album));
    }

    private String getAlbumPhotosDescription(String templateRow, String album) {
        StringBuilder description = new StringBuilder();
        findAlbumPhotos(album).forEach(photo -> description.append(getPhotoLinkRow(templateRow, album, photo)));
        return description.toString();
    }

    private String getPhotoLinkRow(String templateRow, String album, String photo) {
        return templateRow
                .replace("%(photo)", photo)
                .replace("%(photoUrl)", getPhotoUrl(album, photo, false))
                + "\n";
    }

    public String renderPhoto(String album, String photo, boolean fullScreen, String urlAllAlbums) {
        int mediaHeight = fullScreen ? MEDIA_HEIGHT_FULLSCREEN : MEDIA_HEIGHT;
        boolean video = videoExtensions.contains(getFilenameExtensionLowerCase(photo));
        Object o = getExifInfo(album, photo);
        return photoTemplateWithHeader
                .replace("%(mediaRealUrl)", getMediaRealUrl(album, photo))
                .replace("%(mediaStyle)", "")//getMediaStyle(album, photo))
                .replace("%(mediaHeight)", "" + mediaHeight)
                .replace("%(previousMediaUrl)", getPhotoUrl(album, findPreviousPhoto(album, photo), fullScreen))
                .replace("%(mediaUrlFullScreen)", getPhotoUrl(album, photo, true))
                .replace("%(nextMediaUrl)", getPhotoUrl(album, findNextPhoto(album, photo), fullScreen))
                .replace("%(mediaUrlNoFullScreen)", getPhotoUrl(album, photo, false))
                .replace("%(albumPhotos)", getAlbumPhotosUrl(album))
                .replace("%(allAlbumsUrl)", urlAllAlbums)
                .replace("%(commentIfNotFullScreenJavaScript)", fullScreen ? "" : "//")
                .replace("%(commentIfFullScreenHtmlStart)", fullScreen ? "<!--" : "")
                .replace("%(commentIfFullScreenHtmlEnd)", fullScreen ? "-->" : "")
                .replace("%(commentIfVideoHtmlStart)", video ? "<!--" : "")
                .replace("%(commentIfVideoHtmlEnd)", video ? "-->" : "")
                .replace("%(commentIfPhotoHtmlStart)", !video ? "<!--" : "")
                .replace("%(commentIfPhotoHtmlEnd)", !video ? "-->" : "");
    }

    private String getAlbumPhotosUrl(String album) {
        return "/album/" + album + "?list=true";
    }

    //private String getMediaStyle(String album, String photo) {
//        int [] exifInfo = getExifInfo(album, photo);
//        int orientation = exifInfo[0];
//        float scale = getScale(orientation, exifInfo[1], exifInfo[2]);
//        if (orientation == 6)
//            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "90", "" + scale);
//        if (orientation == 8)
//            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "-90", "" + scale);
//        if (orientation == 3)
//            return String.format(STYLE_TRANSFORM_ROTATE_SCALE, "180", "1.0");
//        return "";
//    }

    private float getScale(int orientation, int width, int height) {
        float scale = 0.66f;
        if ((orientation == 6 || orientation == 8) && width > 0 && height > 0) {
            if (width > height)
                scale = (float) height / (float) width - 0.005f;
            else {
                scale = (float) width / (float) height - 0.005f;
            }
        }
        return scale;
    }

    private String getMediaRealUrl(String album, String photo) {
        return "/" + baseGalleryDir + "/" + album + "/" + photo;
    }

    private String getPhotoUrl(String album, String photo, boolean fullScreen) {
        return "/photo/" + album + "/" + photo + (fullScreen ? "?fullScreen=true" : "");
    }

    public static String encode(String url) {
        try {
            return URLEncoder.encode(url,"UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to encode");
        }
    }

    private List<String> findAllAlbums() {
        String[] directories = new File(albumsPath).list(
                (current, name) -> new File(current, name).isDirectory());
        return directories != null && directories.length > 0 ?
                Arrays.asList(directories).stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())
                : new LinkedList<>();
    }

    private List<String> findAlbumsForUser(List<String> allowedAlbums) {
        List<String> allAlbums = findAllAlbums();
        return allAlbums.stream()
                .filter(album -> matches(album, allowedAlbums))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    private boolean matches(String album, List<String> allowedAlbums) {
        for (String allowedAlbum : allowedAlbums) {
            if ("*".equals(allowedAlbum) || album.equalsIgnoreCase(allowedAlbum)) {
                return true;
            }
        }
        return false;
    }

    private List<String> findAlbumPhotos(String album) {
        List<String> photos = ALBUM_PHOTOS.get(album);
        if (photos == null) {
            File dir = new File(albumsPath + album);
            String[] files = dir.list((current, name) -> {
                File imageFile = new File(current, name);
                return imageFile.isFile()
                        && !ignoreExtensions.contains(getFilenameExtensionLowerCase(name));
            });
            photos = Arrays.asList(files).stream().sorted().map(RenderingService::encode).collect(Collectors.toList());
            ALBUM_PHOTOS.put(album, photos);
        }
        return photos;
    }

    public String findNextPhoto(String album, String current) {
        List<String> photos = findAlbumPhotos(album);
        if (current != null) {
            Iterator<String> iterator = photos.iterator();
            for (; iterator.hasNext(); ) {
                String file = iterator.next();
                if (file.equals(current) && iterator.hasNext())
                    return iterator.next();
            }
        }
        return photos.get(0);
    }

    private String findPreviousPhoto(String album, String current) {
        List<String> photos = findAlbumPhotos(album);
        if (current != null) {
            Iterator<String> iterator = photos.iterator();
            String previous = photos.size() > 1 ? photos.get(photos.size() - 1) : photos.get(0);
            for (; iterator.hasNext(); ) {
                String file = iterator.next();
                if (file.equals(current))
                    return previous;
                previous = file;
            }
        }
        return photos.get(0);
    }

    private String getFilenameExtensionLowerCase(String name) {
        return name.substring(name.lastIndexOf(".") + 1).toLowerCase();
    }

    private Object [] getExifInfo(String album, String photo) {
        File imageFile = new File(albumsPath + album + File.separator + photo);
        int orientation = 0;
        int width = 0;
        int height = 0;
        Map<Object,Object> allExifTags = new HashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
            for (Directory directory : metadata.getDirectories()) {
                allExifTags.putAll(directory.getTags().stream().collect(
                        Collectors.toMap(
                                tag -> tag.getTagName(),
                                tag -> Optional.ofNullable(directory.getString(tag.getTagType())).orElse(""),
                                (tag1, tag2) -> {
                                    System.out.println("Exif duplicate key found: " + tag1);
                                    return tag1;
                                }
                                )));
            }
            Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
            if (directory != null) {
                orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
            if (jpegDirectory != null) {
                width = jpegDirectory.getImageWidth();
                height = jpegDirectory.getImageHeight();
            }
        } catch (MetadataException | IOException | ImageProcessingException e) {
            //silently swallow
        }
        return new Object [] { orientation, width, height , allExifTags };
    }

    public void reset() {
        ALBUM_PHOTOS.clear();
    }
}
