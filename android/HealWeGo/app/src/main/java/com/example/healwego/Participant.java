package com.example.healwego;

public class Participant {
    private String name;
    private String role; // 방장 여부 등 역할
    private boolean isCurrentUser; // 현재 사용자인지 여부
    private boolean isReady; // READY 상태
    private String id;

    public Participant(String name, String role, boolean isCurrentUser, boolean isReady, String id) {
        this.name = name;
        this.role = role;
        this.isCurrentUser = isCurrentUser;
        this.isReady = isReady;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public boolean isCurrentUser() {
        return isCurrentUser;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public String getId() {
        return id;
    };

}
