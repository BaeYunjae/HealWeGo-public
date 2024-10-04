package com.example.healwego;

public class Constant {
    private static String userNameKey;

    public static void setUserNameKey(String userName){
        if(userName == null) return;
        userNameKey = userName;
    }

    public static String getUserNameKey(){
        return userNameKey;
    }
}
