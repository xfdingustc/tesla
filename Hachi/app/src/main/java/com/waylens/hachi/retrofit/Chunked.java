package com.waylens.hachi.retrofit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Created by Xiaofei on 2016/9/8.
 */
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Chunked {
}
