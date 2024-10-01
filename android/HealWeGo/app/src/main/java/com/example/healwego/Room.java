package com.example.healwego;

public class Room {
    private String roomId;
    private String roomName;
    private String theme;
    private String locName;
    private String time;
    private int numUsers;
    private String gender;
    private int option;

    // 생성자
    public Room(String roomId, String roomName, String theme, String locName, String time, int numUsers, String gender, int option) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.theme = theme;
        this.locName = locName;
        this.time = time;
        this.numUsers = numUsers;
        this.gender = gender;
        this.option = option;
    }

    // Getter 메소드
    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public String getTheme() { return theme; }
    public String getLocName() { return locName; }
    public String getTime() { return time; }
    public String getNumUsers() { return String.valueOf(numUsers); }
    public String getGender() { return gender; }
    public int getOption() { return option; }
}
