package com.pulseapp.android.gles;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.ETC1Util;
import android.opengl.GLES20;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by indianrenters on 10/4/16.
 */
public class PKMEncoder {
    public static int loadCompressedTexture(final Context context, final int resourceId){
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0){
            InputStream input = context.getResources().openRawResource(resourceId);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            try{
                ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0,GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5, input);
            }
            catch(IOException e){
                System.out.println("DEBUG! IOException"+e.getMessage());
            }
            finally{
                try {
                    input.close();
                } catch (IOException e) {
                    // ignore exception thrown from close.
                }
            }
            //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        }
        else
            throw new RuntimeException("Error loading texture.");
        return textureHandle[0];
    }

    public static Bitmap createBitmapFromStream(InputStream inputStream) {
        Bitmap bitmap=null;
        //Do something here to create bitmap
        return bitmap;
    }

}
