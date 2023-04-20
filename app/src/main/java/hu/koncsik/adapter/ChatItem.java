package hu.koncsik.adapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ChatItem implements Serializable{
    private String id;
    private String name;
    private List<String> members;
    private List<Message_1> messages;
    private boolean group;

    public ChatItem(){}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public List<Message_1> getMessages() {
        return messages;
    }

    public void setMessages(List<Message_1> messages) {
        this.messages = messages;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isGroup() {
        return group;
    }

    public void setGroup(boolean group) {
        this.group = group;
    }

    public ChatItem(String loggedInUserEmail, String otherUserEmail) {
        this.members = new ArrayList<>();
        this.members.add(loggedInUserEmail);
        this.members.add(otherUserEmail);
        this.messages = new ArrayList<>();
        this.group = false;
    }
}




