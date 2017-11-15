package com.pulseapp.android.models;

import com.pulseapp.android.util.AppLibrary;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by deepankur on 9/13/16.
 */

public class MediaDownloadDetails {

    public HashMap<String, Integer> moments;
    public HashMap<String, Integer> rooms;
    public int status;
    public String url;
    public long createdAt;
    public HashMap<String,Long> viewed;
    public HashMap<String,Long> screenShotted;

    public MediaDownloadDetails() {
    }

    public HashMap<String, Integer> getRooms() {
        return rooms;
    }

    public HashMap<String, Integer> getMoments() {
        return moments;
    }

    public int getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(this.getClass().getName());
        result.append(super.toString());
        result.append(" Object {");
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = this.getClass().getDeclaredFields();

        //print field names paired with their values
        for (Field field : fields) {
            result.append("  ");
            try {
                result.append(field.getName());
                result.append(": ");
                //requires access to private field:
                result.append(field.get(this));
            } catch (IllegalAccessException ex) {
                System.out.println(ex);
            }
            result.append(newLine);
        }
        result.append("}");
        return result.toString();
    }

}
