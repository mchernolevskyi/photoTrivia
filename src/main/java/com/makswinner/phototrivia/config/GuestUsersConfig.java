package com.makswinner.phototrivia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Dr Maksym Chernolevskyi
 */

@Component
@ConfigurationProperties("guest")
public class GuestUsersConfig {

    private List<UserAlbumAccess> users;

    public List<UserAlbumAccess> getUsers() {
        return users;
    }

    public void setUsers(List<UserAlbumAccess> users) {
        this.users = users;
    }

    public static class UserAlbumAccess {
        private String name;
        private String password;
        private String albums;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getAlbums() {
            return albums;
        }

        public void setAlbums(String albums) {
            this.albums = albums;
        }
    }

}
