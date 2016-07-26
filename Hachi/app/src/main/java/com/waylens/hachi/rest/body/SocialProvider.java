package com.waylens.hachi.rest.body;

/**
 * Created by Xiaofei on 2016/7/25.
 */
public class SocialProvider {
    public static final String FACEBOOK = "facebook";
    public static final String YOUTUBE = "youtube";

    String provider;

    String code;

    String accessToken;

    public static SocialProvider newFaceBookProvider(String token) {
        SocialProvider provider = new SocialProvider();
        provider.provider = FACEBOOK;
        provider.accessToken = token;
        return provider;
    }

    public static SocialProvider newYoutubeProvider(String code) {
        SocialProvider provider = new SocialProvider();
        provider.provider = YOUTUBE;
        provider.code = code;
        return provider;
    }
}
