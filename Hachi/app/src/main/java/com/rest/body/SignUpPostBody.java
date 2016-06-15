package com.rest.body;

/**
 * Created by liushuwei on 2016/6/13.
 */
public class SignUpPostBody {
    public String email;
    public String userName;
    public String password;

    public SignUpPostBody(String email, String userName, String password) {
        this.email = email;
        this.userName = userName;
        this.password = password;
    }
}
