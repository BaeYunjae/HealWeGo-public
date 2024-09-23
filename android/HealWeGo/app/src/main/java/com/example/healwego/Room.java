package com.example.healwego;

public class Room {
    private String roomId;
    private String roomName;
    private String theme;
    private String locName;
    private int numUsers;
    private String gender;

    // 생성자
    public Room(String roomId, String roomName, String theme, String locName, int numUsers, String gender) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.theme = theme;
        this.locName = locName;
        this.numUsers = numUsers;
        this.gender = gender;
    }

    // Getter 메소드
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getTheme() { return theme; }
    public String getLocName() { return locName; }
    public int getNumUsers() { return numUsers; }
    public String getGender() { return gender; }
}
