package com.waylens.hachi.rest.body;

/**
 * Created by Xiaofei on 2016/7/25.
 */
public class SocialProvider {
    String provider;

    String code;

    String accessToken;

    public static SocialProvider newFaceBookProvider(String token) {
        SocialProvider provider = new SocialProvider();
        provider.provider = "facebook";
        provider.accessToken = token;
        return provider;
    }
}
