package hu.koncsik.adapter;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import hu.koncsik.extension.CustomLocalDateTime;

public class UserItem {

    private String name;
    private String email;
    private Date lastActive;

    private List<UserItem> cheat;
    private List<UserItem> groupCheat;


    public UserItem() {
    }

    public UserItem(String name, String email, Date lastActive) {
        this.name = name;
        this.lastActive = lastActive;
        this.email = email;
        this.cheat = new LinkedList<>();
        this.groupCheat = new LinkedList<>();
    }

    public UserItem(String name, String email, Date lastActive, List<UserItem> cheat, List<UserItem> groupCheat) {
        this.name = name;
        this.email = email;
        this.lastActive = lastActive;
        this.cheat = cheat;
        this.groupCheat = groupCheat;
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

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public List<UserItem> getCheat() {
        return cheat;
    }

    public void setCheat(List<UserItem> cheat) {
        this.cheat = cheat;
    }

    public List<UserItem> getGroupCheat() {
        return groupCheat;
    }

    public void setGroupCheat(List<UserItem> groupCheat) {
        this.groupCheat = groupCheat;
    }
}


