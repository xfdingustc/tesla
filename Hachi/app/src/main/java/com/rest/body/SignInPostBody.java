package com.rest.body;

/**
 * Created by liushuwei on 2016/6/13.
 */
public class SignInPostBody {
    public String email;
    public String password;
    public SignInPostBody(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
