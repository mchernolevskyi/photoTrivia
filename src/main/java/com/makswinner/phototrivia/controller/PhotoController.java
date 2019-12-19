package com.makswinner.phototrivia.controller;

import com.makswinner.phototrivia.service.RenderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

import static com.makswinner.phototrivia.config.SecurityConfig.*;

/**
 * @author Dr Maksym Chernolevskyi
 */
@RestController
public class PhotoController {
    private static final String URL_ALL_ALBUMS = "/";

    @Autowired
    private RenderingService renderingService;

    @RequestMapping(URL_ALL_ALBUMS)
    public String showAll() {
        return renderingService.renderAlbums();
    }

    @RequestMapping("/album/{album}")
    public String showAlbum(@PathVariable("album") String album,
                            @RequestParam(value = "list", required = false) boolean list,
                            HttpServletResponse response) {
        try {
            if (!list) {
                response.sendRedirect(
                        "/photo/" + album + "/" + renderingService.findNextPhoto(album, null));
                return null;
            } else {
                return renderingService.renderAlbumPhotos(album);
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

    private List<String> findAllAlbums() {
        //TODO nested folders
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
            if ("*".equals(allowedAlbum) || album.equals(allowedAlbum)) {
                return true;
            }
        }
        return false;
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

    private int [] getExifInfo(String album, String photo) {
        File imageFile = new File(albumsPath + album + File.separator + photo);
        int orientation = 0;
        int width = 0;
        int height = 0;
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
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
        return new int [] { orientation, width, height };
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
        UserDetails userDetails =
                (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String userRole = userDetails.getAuthorities().stream()
                .filter(authority -> authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse(ROLE_GUEST);
        String albumsRaw = userDetails.getAuthorities().stream()
                .filter(authority -> !authority.getAuthority().startsWith("ROLE_"))
                .map(authority -> authority.getAuthority())
                .findFirst()
                .orElse("");
        List<String> allowedAlbums = Arrays.stream(albumsRaw.split(",")).collect(Collectors.toList());
        List<String> albums = new LinkedList<>();
        if ((ROLE_ADMIN).equals(userRole)) {
            albums = findAllAlbums();
        } else if ((ROLE_GUEST).equals(userRole)) {
            albums = findAlbumsForUser(allowedAlbums);
        }
        StringBuilder description = new StringBuilder();
        albums.forEach(album -> description.append(getAlbumLink(album)));
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
                .replace("%(mediaStyle)", getMediaStyle(album, photo))
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

    @RequestMapping("/reset")
    public void reset() {
        renderingService.reset();
    }

}
