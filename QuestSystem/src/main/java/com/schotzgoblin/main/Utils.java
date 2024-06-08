package com.schotzgoblin.main;

public class Utils {
    public static String getTimeStringFromSecs(int time){
        int hours = time / 3600;
        int minutes = (time % 3600) / 60;
        int secondsRemaining = time % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secondsRemaining);

    }
}
