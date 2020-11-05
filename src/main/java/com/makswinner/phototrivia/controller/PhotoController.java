package com.makswinner.phototrivia.controller;

import com.makswinner.phototrivia.service.RenderingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author Dr Maksym Chernolevskyi
 */
@RestController
public class PhotoController {
    private static final String URL_ALL_ALBUMS = "/";

    @Autowired
    private RenderingService renderingService;

    @RequestMapping(URL_ALL_ALBUMS)
    public String showAllAlbums() {
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
        return renderingService.renderPhoto(album, RenderingService.encode(photo), fullScreen, URL_ALL_ALBUMS);
    }

    @RequestMapping("/reset")
    public void reset() {
        renderingService.reset();
    }

}
