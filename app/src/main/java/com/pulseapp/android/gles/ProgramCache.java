package com.pulseapp.android.gles;

import android.util.Log;

import com.pulseapp.android.broadcast.FilterManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Generates and caches program. Programs are generated based on
 * ProgramDescriptions.
 */
class ProgramCache {

    private static final String TAG = "GLES20-ProgramCache";
    private static final boolean VERBOSE = false;

/*    static final String gVS_Header_Attributes =
            "attribute vec4 position;\n";*/

    static final String gVS_Header_Attributes =
            "attribute vec3 position;\n";

    static final String gVS_Header_Varyings_Position =
            "attribute vec2 outPosition;\n";

    static final String gVS_Header_Attributes_Colors =
            "attribute vec4 color;\n";

    static final String gVS_Header_Attributes_TexCoords =
            "attribute vec2 texCoords;\n";

    static final String gVS_Header_Uniforms_HasGradient =
            "uniform mat4 screenSpace;\n";

    static final String gVS_Header_Uniforms_TexelSize =
            "uniform vec2 texelSize;\n";

    static final String gVS_Header_Uniforms_TextureTransform =
            "uniform mat4 mainTextureTransform;\n";

    static final String gVS_Header_Varyings_Colors =
            "varying vec4 outColor;\n";

    static final String gVS_Header_Varyings_HasTexture =
            "varying vec2 outTexCoordinates;\n";

    static final String gVS_Header_Varyings_Neighours =
//        "vec2 texelSize = vec2(0.0018, 0.0018);\n" +
        "leftTextureCoordinate = outTexCoordinates.xy + vec2(-texelSize.x, 0.0);\n" +
        "rightTextureCoordinate = outTexCoordinates.xy + vec2(texelSize.x, 0.0);\n" +
        "topTextureCoordinate = outTexCoordinates.xy + vec2(0.0, -texelSize.y);\n" +
        "topLeftTextureCoordinate = outTexCoordinates.xy + vec2(-texelSize.x, -texelSize.y);\n" +
        "topRightTextureCoordinate = outTexCoordinates.xy + vec2(-texelSize.x, texelSize.y);\n" +
        "bottomTextureCoordinate = outTexCoordinates.xy + vec2(0.0, texelSize.y);\n" +
        "bottomLeftTextureCoordinate = outTexCoordinates.xy - vec2(texelSize.x, -texelSize.y);\n" +
        "bottomRightTextureCoordinate = outTexCoordinates.xy + vec2(texelSize.x, texelSize.y);\n"
    ;

    // 2 pass box blur
    // Here texelSize is responsible for holding only horizontal or vertical offsets.
    // 1 of them have to be 0 for 2 pass box blur filter to work properly
    static final String gVS_Header_Varyings_1dBlur_Neighbours =
        "vec2 firstOffset = vec2(1.5 * texelSize.x, 1.5 * texelSize.y);\n" +
        "vec2 secondOffset = vec2(2.5 * texelSize.x, 2.5 * texelSize.y);\n" +
        "vec2 thirdOffset = vec2(3.5 * texelSize.x, 3.5 * texelSize.y);\n" +
        "vec2 fourthOffset = vec2(4.5 * texelSize.x, 4.5 * texelSize.y);\n" +
        "oneStepLeftTextureCoordinate = outTexCoordinates.xy - firstOffset;\n" +
        "oneStepRightTextureCoordinate = outTexCoordinates.xy + firstOffset;\n" +
        "twoStepLeftTextureCoordinate = outTexCoordinates.xy - secondOffset;\n" +
        "twoStepRightTextureCoordinate = outTexCoordinates.xy + secondOffset;\n" +
        "threeStepLeftTextureCoordinate = outTexCoordinates.xy - thirdOffset;\n" +
        "threeStepRightTextureCoordinate = outTexCoordinates.xy + thirdOffset;\n" +
        "fourStepLeftTextureCoordinate = outTexCoordinates.xy - fourthOffset;\n" +
        "fourStepRightTextureCoordinate = outTexCoordinates.xy + fourthOffset;\n"
    ;

    static final String gVS_Header_Varyings_2dBlur_5x5_Neighbours =
            "   blurCoord0x0 = (float((-2+2)*(2*2+1)+(-2+2)))/64.0;\n" +
            "   blurCoord0x1 = (float((-2+2)*(2*2+1)+(-1+2)))/64.0;\n" +
            "   blurCoord0x2 = (float((-2+2)*(2*2+1)+(0+2)))/64.0;\n" +
            "   blurCoord0x3 = (float((-2+2)*(2*2+1)+(1+2)))/64.0;\n" +
            "   blurCoord0x4 = (float((-2+2)*(2*2+1)+(2+2)))/64.0;\n"
            +
            "   blurCoord1x0 = (float((-1+2)*(2*2+1)+(-2+2)))/64.0;\n" +
            "   blurCoord1x1 = (float((-1+2)*(2*2+1)+(-1+2)))/64.0;\n" +
            "   blurCoord1x2 = (float((-1+2)*(2*2+1)+(0+2)))/64.0;\n" +
            "   blurCoord1x3 = (float((-1+2)*(2*2+1)+(1+2)))/64.0;\n" +
            "   blurCoord1x4 = (float((-1+2)*(2*2+1)+(2+2)))/64.0;\n"
            +
            "   blurCoord2x0 = (float((0+2)*(2*2+1)+(-2+2)))/64.0;\n" +
            "   blurCoord2x1 = (float((0+2)*(2*2+1)+(-1+2)))/64.0;\n" +
            "   blurCoord2x2 = (float((0+2)*(2*2+1)+(0+2)))/64.0;\n" +
            "   blurCoord2x3 = (float((0+2)*(2*2+1)+(1+2)))/64.0;\n" +
            "   blurCoord2x4 = (float((0+2)*(2*2+1)+(2+2)))/64.0;\n"
            +
            "   blurCoord3x0 = (float((1+2)*(2*2+1)+(-2+2)))/64.0;\n" +
            "   blurCoord3x1 = (float((1+2)*(2*2+1)+(-1+2)))/64.0;\n" +
            "   blurCoord3x2 = (float((1+2)*(2*2+1)+(0+2)))/64.0;\n" +
            "   blurCoord3x3 = (float((1+2)*(2*2+1)+(1+2)))/64.0;\n" +
            "   blurCoord3x4 = (float((1+2)*(2*2+1)+(2+2)))/64.0;\n"
            +
            "   blurCoord4x0 = (float((2+2)*(2*2+1)+(-2+2)))/64.0;\n" +
            "   blurCoord4x1 = (float((2+2)*(2*2+1)+(-1+2)))/64.0;\n" +
            "   blurCoord4x2 = (float((2+2)*(2*2+1)+(0+2)))/64.0;\n" +
            "   blurCoord4x3 = (float((2+2)*(2*2+1)+(1+2)))/64.0;\n" +
            "   blurCoord4x4 = (float((2+2)*(2*2+1)+(2+2)))/64.0;\n"
        ;

    static final String gFS_Header_Varyings_2dBlur_3x3_Neighours =
        "varying vec2 leftTextureCoordinate;\n" +
        "varying vec2 rightTextureCoordinate;\n" +
        "varying vec2 topTextureCoordinate;\n" +
        "varying vec2 topLeftTextureCoordinate;\n" +
        "varying vec2 topRightTextureCoordinate;\n" +
        "varying vec2 bottomTextureCoordinate;\n" +
        "varying vec2 bottomLeftTextureCoordinate;\n" +
        "varying vec2 bottomRightTextureCoordinate;\n"
        ;

    static final String gFS_Header_Varyings_2dBlur_5x5_Neighours =
        "varying float blurCoord0x0;\n" +
        "varying float blurCoord0x1;\n" +
        "varying float blurCoord0x2;\n" +
        "varying float blurCoord0x3;\n" +
        "varying float blurCoord0x4;\n" +

        "varying float blurCoord1x0;\n" +
        "varying float blurCoord1x1;\n" +
        "varying float blurCoord1x2;\n" +
        "varying float blurCoord1x3;\n" +
        "varying float blurCoord1x4;\n" +

        "varying float blurCoord2x0;\n" +
        "varying float blurCoord2x1;\n" +
        "varying float blurCoord2x2;\n" +
        "varying float blurCoord2x3;\n" +
        "varying float blurCoord2x4;\n" +

        "varying float blurCoord3x0;\n" +
        "varying float blurCoord3x1;\n" +
        "varying float blurCoord3x2;\n" +
        "varying float blurCoord3x3;\n" +
        "varying float blurCoord3x4;\n" +

        "varying float blurCoord4x0;\n" +
        "varying float blurCoord4x1;\n" +
        "varying float blurCoord4x2;\n" +
        "varying float blurCoord4x3;\n" +
        "varying float blurCoord4x4;\n"
    ;

    static final String gFS_Header_Varyings_1d_Blur_Neighbours =
        "varying vec2 oneStepLeftTextureCoordinate;\n" +
        "varying vec2 oneStepRightTextureCoordinate;\n" +
        "varying vec2 twoStepLeftTextureCoordinate;\n" +
        "varying vec2 twoStepRightTextureCoordinate;\n" +
        "varying vec2 threeStepLeftTextureCoordinate;\n" +
        "varying vec2 threeStepRightTextureCoordinate;\n" +
        "varying vec2 fourStepLeftTextureCoordinate;\n" +
        "varying vec2 fourStepRightTextureCoordinate;\n"
    ;

    static final String gFS_Header_Func_HSV =
        "vec3 rgb2hsv(vec3 c)\n" +
        "{\n" +
        "    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
        "    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
        "    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
        "\n" +
        "    float d = q.x - min(q.w, q.y);\n" +
        "    float e = 1.0e-10;\n" +
        "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
        "}\n"

        +

        "vec3 hsv2rgb(vec3 c)\n" +
        "{\n" +
        "    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);\n" +
        "    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);\n" +
        "    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);\n" +
        "}\n"
        ;

/*    static final String gVS_Header_Uniforms =
            "uniform mat4 projection;\n" +
            "uniform mat4 transform;\n";*/

    static final String gVS_Header_Uniforms =
            "uniform mat4 uMVPMatrix;\n";

    static final String gVS_Main =
            "void main() {\n";

    static final String gVS_Main_OutColors =
            "    outColor = color;\n";

    static final String gVS_Main_OutTexCoords =
            "    outTexCoordinates = texCoords;\n";

    static final String gVS_Main_OutTransformedTexCoords =
            "    outTexCoordinates = (mainTextureTransform * vec4(texCoords, 0.0, 1.0)).xy;\n";

//s
/*    static final String gVS_Main_Position =
            "    vec4 transformedPosition = projection * transform * position;\n" +
            "    gl_Position = transformedPosition;\n";*/

static final String gVS_Main_Position =
        "    vec4 transformedPosition = uMVPMatrix * vec4(position, 1.0);\n" +
                "    gl_Position = transformedPosition;\n";

    static final String gVS_Footer =
            "}\n";

    static final String gFS_Header_Extension_ExternalTexture =
            "#extension GL_OES_EGL_image_external : require\n";

    static final String gFS_Header =
            "precision mediump float;\n";

    static final String gFS_Uniforms_TextureSampler =
            "uniform sampler2D baseSampler;\n";

    static final String gFS_Uniforms_TextureSampler2 =
            "uniform sampler2D baseSampler2;\n";

    static final String gFS_Uniforms_TextureSampler3 =
            "uniform sampler2D baseSampler3;\n";

    static final String gFS_Uniforms_TextureSampler4 =
            "uniform sampler2D baseSampler4;\n";

    static final String gFS_Uniforms_TextureSampler5 =
            "uniform sampler2D baseSampler5;\n";

    static final String gFS_Uniforms_TextureSampler6 =
            "uniform sampler2D baseSampler6;\n";

    static final String gFS_Uniforms_TextureSampler7 =
            "uniform sampler2D baseSampler7;\n";

    static final String gFS_Uniforms_TextureSampler8 =
            "uniform sampler2D baseSampler8;\n";


    static final String gFS_Uniforms_BlurCoeffSampler =
            "uniform sampler2D blurCoeffSampler;\n";

    static final String gFS_Uniforms_ExternalTextureSampler =
            "uniform samplerExternalOES baseSampler;\n";

    static final String gFS_Uniforms_Textures_BlendFactor =
            "uniform float blendFactor;\n";

    static final String gFS_Uniforms_Texture_AlphaFactor =
            "uniform float alphaFactor;\n";

    static final String gFS_Uniforms_Orientation =
            "uniform int orientation;\n";

    static final String gFS_Uniforms_Texture_TexelSize =
            "uniform vec2 texelSize;\n";
    /*
Filter List which we support START
 */
    static final String gFS_Uniforms_ColorFilter_BW =
            "uniform bool bwMode;\n";

    static final String gFS_Uniforms_ColorFilter_Lively =
            "uniform bool livelyMode;\n";

    static final String gFS_Uniforms_ColorFilter_Rich =
            "uniform bool richMode;\n";

    static final String gFS_Uniforms_ColorFilter_Night =
            "uniform bool nightMode;\n";
/*
Filter List which we support END
 */

    static final String gFS_Main =
            "void main(void) {\n" +
            "    mediump vec4 fragColor;\n";

    static final String gFS_Main_Texture2_Dec =
            "    vec4 fragColor2;\n";

    // General case
    static final String gFS_Main_FetchColor =
            "    fragColor = blendFactor * outColor;\n";

    static final String gFS_Main_FetchTexture =
            "    fragColor = texture2D(baseSampler, outTexCoordinates);\n";

    static final String gFS_Main_FetchTexture2 =
            "    fragColor2 = texture2D(baseSampler2, outTexCoordinates);\n";

    static final String gFS_Main_Blend_Textures =
/*            "    if(outTexCoordinates.y < 0.5)\n" +*/
/*            "    fragColor.rgb = fragColor2.rgb * vec3(blendFactor, blendFactor, blendFactor) + fragColor.rgb * vec3(1.0-blendFactor, 1.0-blendFactor, 1.0-blendFactor);\n"*//* +
            "    else\n" +*/
            "    fragColor = fragColor2 * vec4(blendFactor, blendFactor, blendFactor, blendFactor) + fragColor * vec4(1.0-blendFactor, 1.0-blendFactor, 1.0-blendFactor, 1.0-blendFactor);\n";

    static final String gFS_Main_FetchA8Texture =
        "    fragColor = texture2D(baseSampler, outTexCoordinates);\n";

/*
Implementation of Filter List which we support START
 */

/*
Assistant functions for filters START
 */
static final String gFS_Main_VoronoiFcn =
    "vec2 getCoordFromColor(vec4 color)\n" +
    "{\n" +
    "    float z = (color.r * 256.0 + color.g * 256.0 + color.b * 256.0) / 3.0;\n" +
    "    float yoff = float(floor(z / 8.0));\n" + //32
    "    float xoff = float(mod(z, 8.0));\n" + //8
    "    float x = color.x*256.0 + xoff*256.0;\n" +
    "    float y = color.y*256.0 + yoff*256.0;\n" +
    "    return vec2(x,y) / vec2(8.0 * 256.0, 32.0 * 256.0) / 10.0;\n" +
    "}\n"
    ;
/*
Assistant functions for filters END
 */

    static final String gFS_Main_If_Start =
            "if( ";

    static final String gFS_Main_If_End =
            ") ";

    static final String gFS_Main_block_Start =
            "{";

    static final String gFS_Main_block_End =
            "    }\n";

    static final String gFS_Main_else_start =
            "    else ";

    static final String gFS_Main_ColorFilter_BW =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;\n" +
            "       fragColor = vec4(pixelLuminance, pixelLuminance, pixelLuminance, fragColor.a);\n"
//            + "}\n"
            ;

    static final String gFS_Main_ColorFilter_Lively =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       fragColor = vec4(log2(fragColor + vec4(1.0, 1.0, 1.0, 1.0)));\n"
//            + "}\n"
            ;

//TODO
//sahil.bajaj@instalively.com
// Contrast should generally be set based on the average pixel luminance of the image
// Assuming it's 0.4, for now.
    static final String gFS_Main_ColorFilter_Contrast =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       float blendFactorContrast = -0.13;\n" +
            "       fragColor = vec4(blendFactorContrast * vec3(0.4, 0.4, 0.4) + (1.0-blendFactorContrast) * fragColor.rgb,fragColor.a);\n" //Higher Contrast
//            + "}\n"
            ;

    static final String gFS_Main_ColorFilter_Saturation =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       float blendFactorSaturation = -0.20;\n" +
            "       float pixelLuminance = fragColor.r * 0.3 + fragColor.g * 0.59 + fragColor.b * 0.11;\n" +
            "       fragColor = vec4(blendFactorSaturation * vec3(pixelLuminance, pixelLuminance, pixelLuminance) + (1.0-blendFactorSaturation) * fragColor.rgb,fragColor.a);\n" //Higher Saturation
//            + "}\n"
            ;

    static final String gFS_Main_ColorFilter_Hot =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "        vec3 layer = vec3(228.0/255.0, 43.0/255.0, 43.0/255.0);\n" +
            "        fragColor.rgb = mix(fragColor.rgb, layer, 0.16);\n"
//            + "}\n"
            ;

    static final String gFS_Main_ColorFilter_Sepia =
//          " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "    const vec3 sepia = vec3(1.2, 1.0, 0.8);\n" +
            "    fragColor.rgb = mix(fragColor.rgb, sepia, 0.6);\n"
//            + "}\n"
            ;

    static final String gFS_Main_ColorFilter_Pixellate =
//          " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "highp vec2 sampleDivisor = vec2(0.012, 0.012);\n" +
            "highp vec2 samplePos = outTexCoordinates.xy - mod(outTexCoordinates.xy, sampleDivisor) + 0.5 * sampleDivisor;\n" +
            "fragColor = texture2D(baseSampler, samplePos);\n"
//            + "}\n"
    ;

/*    static final String gFS_Main_ColorFilter_Night =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       float factor = 0.2;\n" +
            "       fragColor = fragColor + fragColor * vec4(factor, factor, factor, 0.0);\n" //Higher Contrast
//            + "}\n"
            ;*/

    static final String gFS_Main_ColorFilter_Bulge =
            "    vec2 focusCoord = vec2(0.0, 0.0);\n" +
            "    float focusBoundary = 0.5;\n" +
            "    vec2 texCoord = 2.0 * outTexCoordinates - 1.0;\n" +
            "    vec2 diffVector = texCoord - focusCoord;\n" +
            "    float r = length(diffVector); // to polar coords \n" +
            "    float phi = atan(diffVector.y, diffVector.x); // to polar coords \n" +
            "    r = r * smoothstep(-0.05-focusBoundary/5.0, focusBoundary, 0.5 * r);\n"+ // Bulge
            "    texCoord.x = r * cos(phi); \n" +
            "    texCoord.y = r * sin(phi); \n" +
            "    texCoord = texCoord / 2.0 + 0.5;\n" +
            "    fragColor = texture2D(baseSampler, texCoord);\n"
            ;

    static final String gFS_Main_ColorFilter_Mirror =
            "    vec2 focusCoord = vec2(0.0, 1.0);\n" +
            "    vec2 texCoord = 2.0 * outTexCoordinates - vec2(1.0, 1.0);\n" +
            "    texCoord = texCoord * sign(texCoord + focusCoord);\n" +
            "    texCoord = texCoord * 0.5 + vec2(0.5, 0.5);\n" +
            "    fragColor = texture2D(baseSampler, texCoord);\n"
            ;

    static final String gFS_Main_ColorFilter_Voronoi =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "       fragColor = texture2D(baseSampler, getCoordFromColor(fragColor));\n"
//            + "}\n"
            ;

    static final String gFS_Main_Fetch8Texels =
            "float bottomLeftIntensity = (texture2D(baseSampler, bottomLeftTextureCoordinate).r + texture2D(baseSampler, bottomLeftTextureCoordinate).g) / 2.0;\n" +
            "float topRightIntensity = (texture2D(baseSampler, topRightTextureCoordinate).r + texture2D(baseSampler, topRightTextureCoordinate).g) / 2.0;\n" +
            "float topLeftIntensity = (texture2D(baseSampler, topLeftTextureCoordinate).r + texture2D(baseSampler, topLeftTextureCoordinate).g) / 2.0;\n" +
            "float bottomRightIntensity = (texture2D(baseSampler, bottomRightTextureCoordinate).r + texture2D(baseSampler, bottomRightTextureCoordinate).g) / 2.0;\n" +
            "float leftIntensity = (texture2D(baseSampler, leftTextureCoordinate).r + texture2D(baseSampler, leftTextureCoordinate).g) / 2.0;\n" +
            "float rightIntensity = (texture2D(baseSampler, rightTextureCoordinate).r + texture2D(baseSampler, rightTextureCoordinate).g) / 2.0;\n" +
            "float bottomIntensity = (texture2D(baseSampler, bottomTextureCoordinate).r + texture2D(baseSampler, bottomTextureCoordinate).g) / 2.0;\n" +
            "float topIntensity = (texture2D(baseSampler, topTextureCoordinate).r + texture2D(baseSampler, topTextureCoordinate).g) / 2.0;\n"
            ;

/*    static final String gFS_Main_FetchBlurTexels =
        "fragColor = 0.25 * texture2D(baseSampler, outTexCoordinates);\n" +
        "fragColor += 0.165 * texture2D(baseSampler, oneStepLeftTextureCoordinate);\n" +
        "fragColor += 0.165 * texture2D(baseSampler, oneStepRightTextureCoordinate);\n" +
        "fragColor += 0.1 * texture2D(baseSampler, twoStepLeftTextureCoordinate);\n" +
        "fragColor += 0.1 * texture2D(baseSampler, twoStepRightTextureCoordinate);\n" +
        "fragColor += 0.07 * texture2D(baseSampler, threeStepLeftTextureCoordinate);\n" +
        "fragColor += 0.07 * texture2D(baseSampler, threeStepRightTextureCoordinate);\n" +
        "fragColor += 0.04 * texture2D(baseSampler, fourStepLeftTextureCoordinate);\n" +
        "fragColor += 0.04 * texture2D(baseSampler, fourStepRightTextureCoordinate);\n"
    ;*/

    static final String gFS_Main_FetchBlurTexels =
            "fragColor = 0.20 * texture2D(baseSampler, outTexCoordinates);\n" +
            "fragColor += 0.165 * texture2D(baseSampler, oneStepLeftTextureCoordinate);\n" +
            "fragColor += 0.165 * texture2D(baseSampler, oneStepRightTextureCoordinate);\n" +
            "fragColor += 0.13 * texture2D(baseSampler, twoStepLeftTextureCoordinate);\n" +
            "fragColor += 0.13 * texture2D(baseSampler, twoStepRightTextureCoordinate);\n" +
            "fragColor += 0.07 * texture2D(baseSampler, threeStepLeftTextureCoordinate);\n" +
            "fragColor += 0.07 * texture2D(baseSampler, threeStepRightTextureCoordinate);\n" +
            "fragColor += 0.04 * texture2D(baseSampler, fourStepLeftTextureCoordinate);\n" +
            "fragColor += 0.04 * texture2D(baseSampler, fourStepRightTextureCoordinate);\n"
            ;

    static final String gFS_Main_Fetch_Variable_BlurTexels =
        "       fragColor = vec4(0.0, 0.0, 0.0, 0.0);\n" +

        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord0x0, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-2.0, -2.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord0x1, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-2.0, -1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord0x2, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-2.0, 0.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord0x3, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-2.0, 1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord0x4, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-2.0, 2.0) * texelSize);\n" +

        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord1x0, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-1.0, -2.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord1x1, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-1.0, -1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord1x2, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-1.0, 0.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord1x3, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-1.0, 1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord1x4, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(-1.0, 2.0) * texelSize);\n" +

        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord2x0, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(0.0, -2.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord2x1, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(0.0, -1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord2x2, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(0.0, 0.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord2x3, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(0.0, 1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord2x4, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(0.0, 2.0) * texelSize);\n" +

        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord3x0, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(1.0, -2.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord3x1, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(1.0, -1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord3x2, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(1.0, 0.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord3x3, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(1.0, 1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord3x4, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(1.0, 2.0) * texelSize);\n" +

        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord4x0, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(2.0, -2.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord4x1, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(2.0, -1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord4x2, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(2.0, 0.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord4x3, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(2.0, 1.0) * texelSize);\n" +
        "       fragColor += texture2D(blurCoeffSampler, vec2(blurCoord4x4, 0.5)).g * texture2D(baseSampler, outTexCoordinates + vec2(2.0, 2.0) * texelSize);\n"
        ;

    static final String gFS_Main_ColorFilter_Sketch =
//        " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
        "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
        "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
        "float mag = 1.0 - (length(vec2(h, v)) * 3.1);\n" +
        "fragColor = vec4(vec3(mag, mag, mag), 1.0);\n"
//        + "}\n"
        ;

    // * edgeStrength
    static final String gFS_Main_ColorFilter_Toon =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "float mag = length(vec2(h, v));\n" +
            "vec3 hsv = rgb2hsv(fragColor.rgb);\n" +
            "hsv.r = floor((hsv.r * 17.0) + 0.5) / 17.0;\n" +
            "hsv.g = floor((hsv.g * 7.0) + 0.9) / 7.0;\n" +
            "hsv.b = floor((hsv.b * 9.0) + 0.5) / 9.0;\n" +
            "float thresholdTest = 1.0 - step(0.47, mag);\n" +
            "fragColor.rgb = hsv2rgb(hsv);\n" +
            "fragColor = vec4(fragColor.rgb * thresholdTest, fragColor.a);\n"
//            "fragColor.rgb = floor((fragColor.rgb * 20.0) + 0.5) / 20.0;\n"
//           + "}\n"
            ;

/*    static final String gFS_Main_ColorFilter_Night =
//            " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
            "   lowp float d = distance(outTexCoordinates, vec2(0.5, 0.5));\n" +
            "   lowp float percent = smoothstep(0.25, 0.57, d);\n" +
            "   fragColor = vec4(mix(fragColor.rgb, vec3(-0.25, 0.30, 0.1), 0.4), fragColor.a);\n" +
            "   fragColor = vec4(mix(fragColor.rgb, vec3(0.0, 0.0, 0.0), percent), fragColor.a);\n"
*//*           + "}\n"*//*
            ;*/

    static final String gFS_Main_ColorFilter_Night =
            "    vec3 hsv = rgb2hsv(fragColor.rgb);\n" +
//            "    hsv.r = 0.5;\n" +
            "   hsv.r = mix(hsv.r, 0.55, 0.9);\n" +
            "   lowp float d = distance(outTexCoordinates, vec2(0.5, 0.5));\n" +
            "   lowp float percent = smoothstep(0.2, 0.475, d);\n" +
            "   fragColor.rgb = hsv2rgb(hsv);\n" +
            "   fragColor = vec4(mix(fragColor.rgb, vec3(0.0, 0.0, 0.0), percent), fragColor.a);\n"
            ;

    static final String gFS_Main_ColorFilter_Vignette =
//        " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
        "   float blendFactorContrast = 0.3;\n" +
        "   fragColor = vec4(blendFactorContrast * vec3(0.4, 0.4, 0.4) + (1.0-blendFactorContrast) * fragColor.rgb,fragColor.a);\n" +
        "   lowp float d = distance(outTexCoordinates, vec2(0.5, 0.5));\n" +
        "   lowp float percent = smoothstep(0.15, 0.62, d);\n" +
        "   fragColor = vec4(mix(fragColor.rgb, vec3(0.0, 0.0, 0.0), percent), fragColor.a);\n"
/*           + "}\n"*/
        ;


    static final String gFS_Main_ColorFilter_Header =
          "const vec3 W = vec3(0.2125, 0.7154, 0.0721);\n"+
          "vec3 BrightnessContrastSaturation(vec3 color, float brt, float con, float sat)\n"+
          "{\n"+
          "vec3 black = vec3(0.0, 0.0, 0.0);\n"+
          "vec3 middle = vec3(0.5, 0.5, 0.5);\n"+
          "float luminance = dot(color, W);\n"+
          "vec3 gray = vec3(luminance, luminance, luminance);\n"+
          "vec3 brtColor = mix(black, color, brt);\n"+
          "vec3 conColor = mix(middle, brtColor, con);\n"+
          "vec3 satColor = mix(gray, conColor, sat);\n"+
          "return satColor;\n"+
          "}\n"+
          "vec3 ovelayBlender(vec3 Color, vec3 filter){\n"+
          "vec3 filter_result;\n"+
          "float luminance = dot(filter, W);\n"+
          "if(luminance < 0.5)\n"+
          "filter_result = 2. * filter * Color;\n"+
          "else\n"+
          "filter_result = 1. - (1. - (2. *(filter - 0.5)))*(1. - Color);\n"+
          "return filter_result;\n"+
          "}\n"+
          "vec3 multiplyBlender(vec3 Color, vec3 filter){\n"+
          "vec3 filter_result;\n"+
          "float luminance = dot(filter, W);\n"+
          "if(luminance < 0.5)\n"+
          "filter_result = 2. * filter * Color;\n"+
          "else\n"+
          "filter_result = Color;\n"+
          "return filter_result;\n"+
          "}\n"
          ;


    static final String gFS_Main_ColorFilter_Curve_Header =
            "vec4 curves(vec4 inColor, sampler2D texCurve){\n"+
            "return vec4(texture2D(texCurve, vec2(inColor.r, 0.3)).r, texture2D(texCurve, vec2(inColor.g, 0.6)).g, texture2D(texCurve, vec2(inColor.b, 0.9)).b, inColor.a);\n"+
            "}"
            ;

    static final String gFS_Main_ColorFilter_Amaro =
//        " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
           // "vec4 texel = texture2D(baseSampler, outTexCoordinates);\n"+
            "vec4 texel=texelnew;\n"+
            "texel.r = texture2D(baseSampler3, vec2(0.5, texel.r)).r;\n"+
            "texel.g = texture2D(baseSampler3, vec2(0.5, texel.g)).g;\n"+
            "texel.b = texture2D(baseSampler3, vec2(0.5, texel.b)).b;\n"+
            "vec4 mapped;\n"+
            "mapped.r = texture2D(baseSampler4, vec2(texel.r, .83333)).r;\n"+
            "mapped.g = texture2D(baseSampler4, vec2(texel.g, .5)).g;\n"+
            "mapped.b = texture2D(baseSampler4, vec2(texel.b, .16666)).b;\n"+
            "mapped.a = 1.0;\n"+
            "fragColor = mapped;\n"
          //"fragColor=vec4(texture2D(baseSampler4,outTexCoordinates));\n"
            ;


    static final String gFS_Main_ColorFilter_XPro =
//        " if(outTexCoordinates.x < outTexCoordinates.y) {\n" +
           "const vec3 W = vec3(0.2125, 0.1754, 0.0721);\n"+
           "float T_bright = 1.3;\n"+
           "float T_contrast = 1.1;\n"+
           "float T_saturation = 1.2;\n"+
           "vec3 irgb = texelnew.rgb;\n"+
           "vec3 filter=texture2D(baseSampler3,outTexCoordinates).rgb;\n"+
           "vec3 bcs_result = BrightnessContrastSaturation(irgb, T_bright, T_contrast, T_saturation);\n"+
           "vec3 blue_result = vec3(bcs_result.r, bcs_result.g*1.03, bcs_result.b);\n"+
           "float luminance = dot(irgb, W);\n"+
           "vec3 gray = vec3(luminance, luminance, luminance);\n"+
           "vec3 result = BrightnessContrastSaturation(gray, 2.5, 1.1, 1.0);\n"+
           "vec3 after_filter = mix(bcs_result, multiplyBlender(bcs_result, filter), 0.7);\n"+
           "fragColor = vec4(after_filter,1.0);\n"
         //"fragColor=vec4(texture2D(baseSampler3,outTexCoordinates));\n"
            ;



    static final String gFS_Main_ColorFilter_InkWell =
            "vec3 texel=texelnew.rgb;\n"+
           //"vec3 texel= texture2D(baseSampler,outTexCoordinates).rgb;\n"+
                   "float T_bright = 0.2;\n"+
                   "float T_contrast = 0.75;\n"+
                   "float T_saturation = 0.1;\n"+
                   "texel = BrightnessContrastSaturation(texel, T_bright, T_contrast, T_saturation);\n"+
           //"texel = vec3(dot(vec3(0.35, 0.65, 0.15), texel));\n"+
           "texel = vec3(texture2D(baseSampler3, vec2(texel.r, .83333)).r);\n"+
           "fragColor = vec4(texel,1.0);\n"
         //"fragColor=vec4(texture2D(baseSampler3,outTexCoordinates));\n"
            ;

    static final String gFS_Main_ColorFilter_NashVille =
            "vec3 texel= texelnew.rgb;\n"+
            "texel = vec3(\n"+
                         "texture2D(baseSampler3, vec2(texel.r, .83333)).r,\n"+
                         "texture2D(baseSampler3, vec2(texel.g, .5)).g,\n"+
                         "texture2D(baseSampler3, vec2(texel.b, .16666)).b);\n"+
            "fragColor = vec4(texel,1.0);\n"
          //"fragColor = vec4(texture2D(baseSampler3,outTexCoordinates));\n"
             ;
    static final String gFS_Main_ColorFilter_Valencia =
            //"vec3 texel= texture2D(baseSampler,outTexCoordinates).rgb;\n"+
            "vec3 texel=texelnew.rgb;\n"+
            "mat3 saturateMatrix = mat3(\n"+
                                        "1.1402,-0.0598,-0.061,\n"+
                                        "-0.1174,1.0826,-0.1186,\n"+
                                        "-0.0228,-0.0228,1.1772);\n"+
            "vec3 lumaCoeffs = vec3(.3, .59, .11);\n"+
            "texel = vec3(\n"+
                          "texture2D(baseSampler3, vec2(texel.r, .83333)).r,\n"+
                          "texture2D(baseSampler3, vec2(texel.g, .5)).g,\n"+
                          "texture2D(baseSampler3, vec2(texel.b, .16666)).b);\n"+
            "fragColor = vec4(texel,1.0);\n"+
            "texel = saturateMatrix * texel;\n"+
            "float luma = dot(lumaCoeffs, texel);\n"+
            "texel = vec3(\n"+
                          "texture2D(baseSampler4, vec2(luma, texel.r)).r,\n"+
                          "texture2D(baseSampler4, vec2(luma, texel.g)).g,\n"+
                          "texture2D(baseSampler4, vec2(luma, texel.b)).b);\n"+
            "fragColor = vec4(texel, 1.0);\n"
          //"fragColor = vec4(texture2D(baseSampler3,outTexCoordinates));\n"
            ;


    static final String gFS_Main_ColorFilter_Hudson =
           // "vec4 texel= texture2D(baseSampler,outTexCoordinates);\n"+
            "vec4 texel=texelnew;\n"+
            "texel.r = texture2D(baseSampler3, vec2(0.5, texel.r)).r;\n"+
            "texel.g = texture2D(baseSampler3, vec2(0.5, texel.g)).g;\n"+
            "texel.b = texture2D(baseSampler3, vec2(0.5, texel.b)).b;\n"+
            "vec4 mapped;\n"+
            "mapped.r = texture2D(baseSampler4, vec2(texel.r, .83333)).r;\n"+
            "mapped.g = texture2D(baseSampler4, vec2(texel.g, .5)).g;\n"+
            "mapped.b = texture2D(baseSampler4, vec2(texel.b, .16666)).b;\n"+
            "mapped.a = 1.0;\n"+
            "fragColor = mapped;\n"
          //"fragColor = vec4(texture2D(baseSampler4,outTexCoordinates));\n"
            ;


    static final String gFS_Main_ColorFilter_Hefe =
            // "vec4 texel= texture2D(baseSampler,outTexCoordinates);\n"+
            "vec3 texel=texelnew.rgb;\n"+
            "vec3 edge = texture2D(baseSampler3, outTexCoordinates).rgb;\n"+
            "texel = texel;\n"+
            "texel = vec3(\n"+
                         "texture2D(baseSampler4, vec2(texel.r, .83333)).r,\n"+
                         "texture2D(baseSampler4, vec2(texel.g, .5)).g,\n"+
                         "texture2D(baseSampler4, vec2(texel.b, .16666)).b);\n"+
            "vec3 luma = vec3(.30, .59, .11);\n"+
            "vec3 gradSample = texture2D(baseSampler5, vec2(dot(luma, texel), .5)).rgb;\n"+
            "texel = vec3(\n"+
                          "texture2D(baseSampler6, vec2(gradSample.r, texel.r)).r,\n"+
                          "texture2D(baseSampler6, vec2(gradSample.g, texel.g)).g,\n"+
                          "texture2D(baseSampler6, vec2(gradSample.b, texel.b)).b \n"+
                          " );\n"+
            "vec3 metal = texture2D(baseSampler7, outTexCoordinates).rgb;\n"+
            "vec3 metaled = vec3(\n"+
            "texture2D(baseSampler6, vec2(metal.r, texel.r)).r,\n"+
            "texture2D(baseSampler6, vec2(metal.g, texel.g)).g,\n"+
            "texture2D(baseSampler6, vec2(metal.b, texel.b)).b \n"+
            ");\n"+

            "fragColor = vec4(metaled, 1.0);\n"
                  //  "fragColor = mapped;\n"
           // "fragColor = vec4(texture2D(baseSampler7,outTexCoordinates));\n"
            ;

    static final String gFS_Main_ColorFilter_Brannan =
            // "vec4 texel= texture2D(baseSampler,outTexCoordinates);\n"+
            "mat3 saturateMatrix = mat3(\n"+
                                     "1.105150, -0.044850, -0.046000,\n"+
                                    "-0.088050, 1.061950, -0.089200,\n"+
                                    "-0.017100, -0.017100, 1.132900);\n"+

            "vec3 luma = vec3(.3, .59, .11);\n"+
            "vec3 texel=texelnew.rgb;\n"+
            "vec2 lookup;\n"+
            "lookup.y = 0.5;\n"+
            "lookup.x = texel.r;\n"+
            "texel.r = texture2D(baseSampler3, lookup).r;\n"+
            "lookup.x = texel.g;\n"+
            "texel.g = texture2D(baseSampler3, lookup).g;\n"+
            "lookup.x = texel.b;\n"+
            "texel.b = texture2D(baseSampler3, lookup).b;\n"+
            "texel = saturateMatrix * texel;\n"+
            "vec2 tc = (2.0 * outTexCoordinates) - 1.0;\n"+
            "float d = dot(tc, tc);\n"+
            "vec3 sampled;\n"+
            "lookup.y = 0.5;\n"+
            "lookup.x = texel.r;\n"+
            "sampled.r = texture2D(baseSampler4, lookup).r;\n"+
            "lookup.x = texel.g;\n"+
            "sampled.g = texture2D(baseSampler4, lookup).g;\n"+
            "lookup.x = texel.b;\n"+
            "sampled.b = texture2D(baseSampler4, lookup).b;\n"+
            "float value = smoothstep(0.0, 1.0, d);\n"+
            "texel = mix(sampled, texel, value);\n"+
            "lookup.x = texel.r;\n"+
            "texel.r = texture2D(baseSampler5, lookup).r;\n"+
            "lookup.x = texel.g;\n"+
            "texel.g = texture2D(baseSampler5, lookup).g;\n"+
            "lookup.x = texel.b;\n"+
            "texel.b = texture2D(baseSampler5, lookup).b;\n"+
            "lookup.x = dot(texel, luma);\n"+
            "texel = mix(texture2D(baseSampler6, lookup).rgb, texel, .5);\n"+
            "lookup.x = texel.r;\n"+
            "texel.r = texture2D(baseSampler7, lookup).r;\n"+
            "lookup.x = texel.g;\n"+
            "texel.g = texture2D(baseSampler7, lookup).g;\n"+
            "lookup.x = texel.b;\n"+
            "texel.b = texture2D(baseSampler7, lookup).b;\n"+
                    "fragColor = vec4(texel, 1.0);\n"
            //  "fragColor = mapped;\n"
            // "fragColor = vec4(texture2D(baseSampler7,outTexCoordinates));\n"
            ;


    static final String gFS_Main_ColorFilter_Moon =
            // "vec4 texel= texture2D(baseSampler,outTexCoordinates);\n"+
            "vec3 texel=texelnew.rgb;\n"+
                    "mat3 saturateMatrix = mat3(\n"+
                    "1.105150, -0.044850, -0.046000,\n"+
                    "-0.088050, 1.061950, -0.089200,\n"+
                    "-0.017100, -0.017100, 1.132900);\n"+

                   // "vec3 luma = vec3(.3, .59, .11);\n"+
                   // "texel=texel*luma;\n"+
                    "float T_bright = 1.0;\n"+
                    "float T_contrast = 0.0;\n"+
                    "float T_saturation = 0.07;\n"+

                    "texel = BrightnessContrastSaturation(texel, T_bright, T_contrast, T_saturation);\n"+
                    "texel.r = texture2D(baseSampler3, vec2(texel.r, 0.5)).r;\n"+
                    "texel.g = texture2D(baseSampler3, vec2(texel.g, 0.5)).g;\n"+
                    "texel.b = texture2D(baseSampler3, vec2(texel.b, 0.5)).b;\n"+
                    //"texel = saturateMatrix * texel;\n"+
                    "vec3 mapped;\n"+
                    "mapped.r = texture2D(baseSampler4, vec2(texel.r, .5)).r;\n"+
                    "mapped.g = texture2D(baseSampler4, vec2(texel.g, .5)).g;\n"+
                    "mapped.b = texture2D(baseSampler4, vec2(texel.b, .5)).b;\n"+
                    "fragColor = vec4(vec3(mapped),1.0);\n"
           // "fragColor = vec4(texture2D(baseSampler4,outTexCoordinates));\n"
            ;



    static final String gFS_Main_ColorFilter_Texel_External =
            "vec4 texelnew= texture2D(baseSampler,outTexCoordinates).bgra;\n";
    static final String gFS_Main_ColorFilter_Texel =
            "vec4 texelnew= texture2D(baseSampler,outTexCoordinates);\n";

/*
Implementation of Filter List which we support END
 */

    static final String gFS_Main_FragColor_Color =
    "    gl_FragColor = vec4(fragColor.rgba);\n";

    static final String gFS_Main_FragColor_Texture =
//            "    gl_FragColor = fragColor;\n";
    "    gl_FragColor = vec4(fragColor.rgba);\n";
    static final String gFS_Main_FragColor_Bitmap =
//            "    gl_FragColor = fragColor;\n";
    "    gl_FragColor = vec4(fragColor.bgra) * vec4(fragColor.a, fragColor.a, fragColor.a, 1.0);\n";

    static final String gFS_Footer =
            "}\n\n";

    HashMap <Integer, Program> mCache;
    private String[] mShaderFilters;

    public ProgramCache() {
        mCache = new HashMap<Integer, Program>();
        mShaderFilters = new String[] {
                ""
                , gFS_Main_ColorFilter_Lively + gFS_Main_ColorFilter_Contrast + gFS_Main_ColorFilter_Saturation
                , gFS_Main_ColorFilter_BW
                , gFS_Main_ColorFilter_Night
                , gFS_Main_ColorFilter_Vignette
                , gFS_Main_ColorFilter_Mirror
                , gFS_Main_ColorFilter_Bulge
                , gFS_Main_ColorFilter_NashVille
                , gFS_Main_ColorFilter_XPro
                , gFS_Main_ColorFilter_Amaro
                , gFS_Main_ColorFilter_Hudson
                , gFS_Main_ColorFilter_Valencia
                , gFS_Main_Fetch8Texels + gFS_Main_ColorFilter_Toon
                , gFS_Main_Fetch8Texels + gFS_Main_ColorFilter_Sketch
                , gFS_Main_ColorFilter_InkWell
                , gFS_Main_ColorFilter_Hefe
                , gFS_Main_ColorFilter_Brannan
                , gFS_Main_ColorFilter_Moon
        };
    }

    Program get(final ProgramDescription description, OpenGLRenderer.Fuzzy mRenderType) {
        int mask = 0;
        switch(mRenderType) {
            case PREVIEW:
                mask = 0x10000000;
                break;
            case VIDEO:
                mask = 0x20000000;
                break;
            case OFFSCREEN:
                mask = 0x40000000;
                break;
            default:
                mask = 0x80000000;
                break;
        }

        int key = description.key() | mask;
        Program program =  mCache.get(new Integer(key));
        if(program == null) {
            program = generateProgram(description);
            if(program.mProgramHandle <= 0) {
                return null;
            }
        }
        mCache.put(Integer.valueOf(key), program);
        return program;
    }

    void clear() {
        if(mCache == null) {
            return;
        }
        int size = mCache.size();

        Set<Integer> keySet = mCache.keySet();
        Iterator<Integer> iterator = keySet.iterator();
        while(iterator.hasNext()) {
            mCache.get(iterator.next()).remove();
        }

        mCache.clear();
        mCache = null;
    }

    void delete() {
        clear();
        mCache = null;
    }

    private Program generateProgram(ProgramDescription description) {
        String vertexShader = generateVertexShader(description);
        String fragmentShader = generateFragmentShader(description);

        if(VERBOSE) {
//            if (description.isKernelBased) {
                Log.d(TAG, "vertex shader :");
                Log.d(TAG, vertexShader);

                Log.d(TAG, "fragment shader :");
                Log.d(TAG, fragmentShader);
//            }
        }
        return new Program(vertexShader, fragmentShader);
    }
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
String generateVertexShader(ProgramDescription description) {
    String shader = "";
    shader += gFS_Header;
    shader += gVS_Header_Attributes;
    shader += gVS_Header_Varyings_Position;

    if(description.hasColor) {
        shader += gVS_Header_Attributes_Colors;
        shader += gVS_Header_Varyings_Colors;
    }

    if (description.hasTexture || description.hasExternalTexture) {
        shader += gVS_Header_Attributes_TexCoords;
    }
     // Uniforms
    shader += gVS_Header_Uniforms;
    shader += gVS_Header_Uniforms_TexelSize;

    if (description.hasTextureTransform) {
        shader += gVS_Header_Uniforms_TextureTransform;
    }
    if (description.hasGradient) {
        shader += gVS_Header_Uniforms_HasGradient;
    }
    if (description.hasTexture || description.hasExternalTexture) {
        shader += gVS_Header_Varyings_HasTexture;
    }
    int filterMode = ((description.colorFilterMode & ProgramDescription.PROGRAM_KEY_FILTER_MODE) >> 12);
    Log.d(TAG,filterMode+"");
    if (description.hasColorFilter && ((filterMode == FilterManager.FILTER_SKETCH)||(filterMode==FilterManager.FILTER_TOON))) {
        shader += gFS_Header_Varyings_2dBlur_3x3_Neighours;
    }

    if(description.isKernelBased) {
        int blurMode = (description.blurMode & ProgramDescription.PROGRAM_KEY_BLUR) >> 20;
        if(blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR) {
            shader += gFS_Header_Varyings_2dBlur_5x5_Neighours;
        } else if(blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL || blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL) {
            shader += gFS_Header_Varyings_1d_Blur_Neighbours;
        }
    }

//TODO
/*    if (description.hasGradient) {
        shader += gVS_Header_Varyings_HasGradient[gradientIndex(description)]);
    }*/

    // Begin the shader
    shader += gVS_Main;
    if (description.hasColor) {
        shader += gVS_Main_OutColors;
    }
    if (description.hasTextureTransform) {
        shader += gVS_Main_OutTransformedTexCoords;
    } else if (description.hasTexture || description.hasExternalTexture) {
        shader += gVS_Main_OutTexCoords;
    }
    // Output transformed position
    shader += gVS_Main_Position;
//TODO
//Sahil
/*    if (description.hasGradient) {
        shader += gVS_Main_OutGradient[gradientIndex(description)];
    }*/
    if (description.hasColorFilter && ((filterMode == FilterManager.FILTER_SKETCH)||(filterMode==FilterManager.FILTER_TOON))) {
        shader += gVS_Header_Varyings_Neighours;
    }
    if(description.isKernelBased) {
        int blurMode = (description.blurMode & ProgramDescription.PROGRAM_KEY_BLUR) >> 20;
        if(blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR) {
            shader += gVS_Header_Varyings_2dBlur_5x5_Neighbours;
        } else if(blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL || blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL) {
            shader += gVS_Header_Varyings_1dBlur_Neighbours;
        }
    }

    // End the shader
    shader += gVS_Footer;
    return shader;
}

String generateFragmentShader(ProgramDescription description) {
    String shader = "";
    if (description.hasExternalTexture) {
        shader += gFS_Header_Extension_ExternalTexture;
    }
    shader += gFS_Header;
    // Varyings
    if (description.hasTexture || description.hasExternalTexture) {
        shader += gVS_Header_Varyings_HasTexture;
    }
    int filterMode = ((description.colorFilterMode & ProgramDescription.PROGRAM_KEY_FILTER_MODE) >> 12);
    if(description.hasColorFilter && ((filterMode == FilterManager.FILTER_SKETCH)||(filterMode==FilterManager.FILTER_TOON))) {
        shader += gFS_Header_Varyings_2dBlur_3x3_Neighours;
    }

    if(description.isKernelBased) {
        int blurMode = (description.blurMode & ProgramDescription.PROGRAM_KEY_BLUR) >> 20;
        if (blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR) {
            shader += gFS_Header_Varyings_2dBlur_5x5_Neighours;
        } else if (blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL || blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL) {
            shader += gFS_Header_Varyings_1d_Blur_Neighbours;
        }
    }
//TODO
//Sahil
/*if (description.hasGradient) {
       shader += gVS_Header_Varyings_HasGradient[gradientIndex(description)];
   }*/
    if (description.hasColor) {
        shader += gVS_Header_Varyings_Colors;
        shader += gFS_Uniforms_Textures_BlendFactor;
    }
    if (description.hasExternalTexture) {
        shader += gFS_Uniforms_ExternalTextureSampler;
        shader += gFS_Uniforms_TextureSampler3;
        shader += gFS_Uniforms_TextureSampler4;
        shader += gFS_Uniforms_TextureSampler5;
        shader += gFS_Uniforms_TextureSampler6;
        shader += gFS_Uniforms_TextureSampler7;
        shader += gFS_Uniforms_TextureSampler8;
    } else if (description.hasTexture) {
        shader += gFS_Uniforms_TextureSampler;

        if (description.hasTexture2) {
            shader += gFS_Uniforms_TextureSampler2;
        }

       if(description.hasFilterTexture){
            shader +=gFS_Uniforms_TextureSampler3;
            shader +=gFS_Uniforms_TextureSampler4;
            shader +=gFS_Uniforms_TextureSampler5;
            shader += gFS_Uniforms_TextureSampler6;
            shader += gFS_Uniforms_TextureSampler7;
            shader += gFS_Uniforms_TextureSampler8;
        }
        if(description.hasTexture && description.hasTexture2) {
            shader += gFS_Uniforms_Textures_BlendFactor;
        }
    }
    shader += gFS_Uniforms_Texture_TexelSize;
    shader += gFS_Uniforms_Texture_AlphaFactor;

    if(description.hasColorFilter) {
        shader += gFS_Uniforms_ColorFilter_BW;
        shader += gFS_Uniforms_ColorFilter_Lively;
        shader += gFS_Uniforms_ColorFilter_Rich;
        shader += gFS_Uniforms_ColorFilter_Night;
//        shader += gFS_Main_VoronoiFcn;
    }

    if(description.needsHSV) {
        shader += gFS_Header_Func_HSV;
    }
    if(description.needsFilterHeader){
        shader+=gFS_Main_ColorFilter_Header;
        shader+=gFS_Main_ColorFilter_Curve_Header;
    }
    if(description.isKernelBased) {
        shader += gFS_Uniforms_BlurCoeffSampler;
    }
//TODO
//Sahil
/*   if (description.hasGradient) {
       shader.appendFormat(gFS_Uniforms_GradientSampler[description.isSimpleGradient],
               gFS_Uniforms_Dither;
   }
if (description. colorOp == ProgramDescription.ColorModifier.kColorBlend) {
       generateBlend(shader, "blendColors", description.colorOp);
   }*/
// Begin the shader
    shader += gFS_Main;

    if(description.hasTexture2) {
        shader += gFS_Main_Texture2_Dec;
    }

    if(description.hasColor) {
        shader += gFS_Main_FetchColor;
    }

        // Stores the result in fragColor directly
    if (description.hasTexture || description.hasExternalTexture) {
        if (description.hasAlpha8Texture) {
//          if (!description.hasGradient) {
                shader += gFS_Main_FetchA8Texture;
//          }
        } else {
            if(!description.isKernelBased)
                shader += gFS_Main_FetchTexture;

            if(description.hasTexture2) {
                shader += gFS_Main_FetchTexture2;
            }
            if(description.hasTexture && description.hasTexture2) {
                shader += gFS_Main_Blend_Textures;
            }
        }
    }

    // Keep the kernel (blur) part before color filters, so that filters can use the 'fragColor' value to apply filter
    // Note that only color based filters will work with kernel.
    // Distortion/kernel based filters including bulge/mirror etc WON'T.
    if(description.isKernelBased) {
        int blurMode = (description.blurMode & ProgramDescription.PROGRAM_KEY_BLUR) >> 20;
        if (blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR) {
            shader += gFS_Main_Fetch_Variable_BlurTexels;
        } else if (blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_HORIZONTAL || blurMode == Drawable2d.CONVOLUTION_TYPE_BLUR_VERTICAL) {
            shader += gFS_Main_FetchBlurTexels;
        }
    }
    if(description.hasExternalTexture){
        shader+=gFS_Main_ColorFilter_Texel_External;
    }
    else{
        shader+=gFS_Main_ColorFilter_Texel;
    }
    if(description.hasColorFilter) {
        int index = (description.colorFilterMode & ProgramDescription.PROGRAM_KEY_FILTER_MODE) >> 12;
        if(index < mShaderFilters.length) {
            shader += mShaderFilters[index];
        }
    }

    if(description.hasColor) {
        shader += gFS_Main_FragColor_Color;
    } else {
        if(description.mOffscreenSurface) {
            if (description.hasExternalTexture || description.hasSBitmapTexture || description.isFBOBased) {
                shader += gFS_Main_FragColor_Bitmap;
            } else if(description.hasTexture) {
                shader += gFS_Main_FragColor_Texture;
            } else {
                Log.e(TAG, "Something went wrong while creating shader. No suitable choice for fragColor type");
            }
        } else {
            if (description.hasExternalTexture || description.hasSBitmapTexture || description.isFBOBased) {
                if(description.hasFilterTexture) {
                    shader += gFS_Main_FragColor_Bitmap;
                } else {
                    shader += gFS_Main_FragColor_Texture;
                }
            } else if(description.hasTexture) {
                shader += gFS_Main_FragColor_Bitmap;
            } else {
                Log.e(TAG, "Something went wrong while creating shader. No suitable choice for fragColor type");
            }
        }
    }
// End the shader
   shader += gFS_Footer;

    if (VERBOSE)
        Log.i(TAG,shader);

    return shader;
}

//TODO
//Sahil
/*void generateBlend(String shader, String name) {

}*/

/*
void printLongString(String shader) {
    ssize_t index = 0;
    ssize_t lastIndex = 0;
    final String str = shader.string();
    while ((index = shader.find("\n", index)) > -1) {
        String line(str, index - lastIndex);
        if (line.length() == 0) line += "\n";
        PROGRAM_LOGD("%s", line.string());
        index++;
        str += (index - lastIndex);
        lastIndex = index;
    }*/

/**
 * Describe the features required for a given program. The features
 * determine the generation of both the vertex and fragment shaders.
 * A ProgramDescription must be used in conjunction with a ProgramCache.
 */
public static class ProgramDescription {
    static final int PROGRAM_KEY_COLOR =            0x01;
    static final int PROGRAM_KEY_TEXTURE =          0x02;
    static final int PROGRAM_KEY_A8_TEXTURE =       0x04;
    static final int PROGRAM_KEY_EXTERNAL_TEXTURE = 0x08;
    static final int PROGRAM_KEY_SCREEN_TEXTURE =   0x10;

    static final int PROGRAM_KEY_GRADIENT =         0x20;

    static final int PROGRAM_KEY_COLOR_FILTER =     0x40;
    static final int PROGRAM_KEY_HSV =              0x80;

    static final int PROGRAM_KEY_Filter_Header=             0x120;

    static final int PROGRAM_KEY_TEXTURE_TRANSFORM = 0x100;

    static final int PROGRAM_KEY_TEXTURE_MULTI =    0x200;

    static final int PROGRAM_KEY_TEXTURE_MULTI_NEW= 0x300;

    static final int PROGRAM_KEY_BLUR =              0x00F00000;

    static final int PROGRAM_KEY_FILTER_MODE =       0x000FF000;

    public enum ColorModifier {
        kColorNone,
        kColorMatrix,
        kColorBlend
    };

    public enum Gradient {
        kGradientLinear,
        kGradientCircular,
        kGradientSweep
    };

    ProgramDescription() {
        reset();
    }

    public boolean hasColor;

    // Texturing
    public boolean hasTexture;
    public boolean hasTexture2;
    public boolean hasFilterTexture;

    public boolean hasAlpha8Texture;
    public boolean  hasTextureTransform;

    public boolean hasExternalTexture;
    public boolean hasSBitmapTexture;
    public boolean mOffscreenSurface;

    public boolean hasGradient;
    Gradient gradientType;

    public boolean hasColorFilter;
    public boolean needsHSV;
    public boolean needsFilterHeader;

    public boolean isKernelBased; // Currently only 5x5 supported. To be combined with mipmaps and s/w blur
    public int blurMode;

    ColorModifier colorOp;

    int colorFilterMode = 0;
    public boolean isFBOBased;
    /**
     * Resets this description. All fields are reset back to the default
     * values they hold after building a new instance.
     */
    void reset() {
        hasColor = false;
        hasTexture = false;
        hasTexture2 = false;
        hasFilterTexture=false;
        hasExternalTexture = false;
        hasSBitmapTexture = false;
        hasAlpha8Texture = false;
        hasTextureTransform = false;

        isKernelBased = false;
        blurMode = Drawable2d.CONVOLUTION_TYPE_NONE;

        isFBOBased = false;

        hasGradient = false;
        gradientType = Gradient.kGradientLinear;

        hasColorFilter = false;
        colorFilterMode = 0;
    }

    /**
     * Computes the unique key identifying this program.
     */
    protected int key() {
        int key = 0;
        if (hasColor) key |= PROGRAM_KEY_COLOR;
        if (hasTexture) key |= PROGRAM_KEY_TEXTURE;
        if (hasTexture2) key |= PROGRAM_KEY_TEXTURE_MULTI;
        if (hasFilterTexture) key  |=PROGRAM_KEY_TEXTURE_MULTI_NEW;
        if( hasExternalTexture) key |= PROGRAM_KEY_EXTERNAL_TEXTURE;
        if( hasSBitmapTexture) key |= PROGRAM_KEY_SCREEN_TEXTURE;
        if (hasAlpha8Texture) key |= PROGRAM_KEY_A8_TEXTURE;
        if (hasGradient) key |= PROGRAM_KEY_GRADIENT;
        if (needsHSV) key |= PROGRAM_KEY_HSV;
        if (needsFilterHeader)key|=PROGRAM_KEY_Filter_Header;
        if (isKernelBased) {
            key |= PROGRAM_KEY_BLUR & blurMode;
        }

        if (hasColorFilter) {
            key |= PROGRAM_KEY_COLOR_FILTER;
            key |= (PROGRAM_KEY_FILTER_MODE & colorFilterMode);
        }
        if (hasTextureTransform) key |= PROGRAM_KEY_TEXTURE_TRANSFORM;

        // key |= gradientType << PROGRAM_GRADIENT_TYPE_SHIFT;
        // if (hasTextureTransform) key |= 0x1 << PROGRAM_HAS_TEXTURE_TRANSFORM_SHIFT;
        return key;
    }

    public ProgramDescription clone(ProgramDescription programDescription) {
        ProgramDescription newProgramDescription = new ProgramDescription();
        newProgramDescription.hasTexture = programDescription.hasTexture;
        newProgramDescription.hasTexture2 = programDescription.hasTexture2;
        newProgramDescription.hasFilterTexture = programDescription.hasFilterTexture;
        newProgramDescription.hasTextureTransform = programDescription.hasTextureTransform;
        newProgramDescription.hasColor = programDescription.hasColor;
        newProgramDescription.hasAlpha8Texture = programDescription.hasAlpha8Texture;
        newProgramDescription.isKernelBased = programDescription.isKernelBased;
        newProgramDescription.hasColorFilter = programDescription.hasColorFilter;
        newProgramDescription.hasExternalTexture = programDescription.hasExternalTexture;
        newProgramDescription.hasGradient = programDescription.hasGradient;
        newProgramDescription.hasSBitmapTexture = programDescription.hasSBitmapTexture;

        newProgramDescription.colorFilterMode = programDescription.colorFilterMode;

        newProgramDescription.mOffscreenSurface = programDescription.mOffscreenSurface;
        newProgramDescription.needsHSV = programDescription.needsHSV;
        newProgramDescription.needsFilterHeader=programDescription.needsFilterHeader;
        return newProgramDescription;
    }

}; // struct ProgramDescription

}; // class ProgramCache

class RenderParams {
    private static final int KERNEL_SIZE = 3;
    // Handles to the GL program and various components of it.
    // Name of the OpenGL program and shaders
    private int mProgramId;
    private int mVertexShader;
    private int mFragmentShader;

//Uniforms
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;

    private int muIsLivelyModeLoc;

//Attributes
    private int maPositionLoc;
    private int maTextureCoordLoc;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;

    // Keeps track of attributes and uniforms slots
    HashMap<String, Integer> mAttributes;
    HashMap<String, Integer> mUniforms;

    boolean mUse;

    // Uniforms caching
    boolean mHasColorUniform;
    int mColorUniform;

    boolean mHasSampler;

//    mat4 mProjection;
    boolean mOffset;

}
