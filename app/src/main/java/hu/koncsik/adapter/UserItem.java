package hu.koncsik.adapter;

import java.time.LocalDateTime;
import java.util.Date;

public class UserItem {

    private String name;
    private String email;
    private LocalDateTime lastActive;


    public UserItem() {
    }

    public UserItem(String name, String email, LocalDateTime lastActive) {
        this.name = name;
        this.lastActive = lastActive;


        this.email = email;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getLastActive() {
        return lastActive;
    }

    public void setLastActive(LocalDateTime lastActive) {
        this.lastActive = lastActive;
    }
}


