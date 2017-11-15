//package com.pulseapp.android.util;
//
//import android.content.Intent;
//
//import com.firebase.client.DataSnapshot;
//
//import java.lang.reflect.Field;
//
///**
// * Created by deepankur on 8/2/16.
// */
//public class SnapShotParser {
//
//    public Class<?> SnapShotParser(DataSnapshot dataSnapshot, final Class<?> castTo) {
//
//        Field[] fields = castTo.getClass().getDeclaredFields();
//        for (Field field : fields) {
//
//            if (field.getType().equals(String.class)){
//                field.set(dataSnapshot.getValue());
//            }
//
//            result.append("  ");
//            try {
//                result.append(field.getName());
//                result.append(": ");
//                //requires access to private field:
//                result.append(field.get(this));
//            } catch (IllegalAccessException ex) {
//                System.out.println(ex);
//            }
//            result.append(newLine);
//        }
//        return castTo;
//    }
//
//    public String toString() {
//        StringBuilder result = new StringBuilder();
//        String newLine = System.getProperty("line.separator");
//
//        result.append(this.getClass().getName());
//        result.append(super.toString());
//        result.append(" Object {");
//        result.append(newLine);
//
//        //determine fields declared in this class only (no fields of superclass)
//        Field[] fields = this.getClass().getDeclaredFields();
//
//        //print field names paired with their values
//        for (Field field : fields) {
//            result.append("  ");
//            try {
//                result.append(field.getName());
//                result.append(": ");
//                //requires access to private field:
//                result.append(field.get(this));
//            } catch (IllegalAccessException ex) {
//                System.out.println(ex);
//            }
//            result.append(newLine);
//        }
//        result.append("}");
//        return result.toString();
//    }
//
//}
