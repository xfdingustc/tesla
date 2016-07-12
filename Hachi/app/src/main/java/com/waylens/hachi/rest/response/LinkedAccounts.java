package com.waylens.hachi.rest.response;

import com.waylens.hachi.utils.ToStringUtils;

import java.util.List;

/**
 * Created by Xiaofei on 2016/6/14.
 */
public class LinkedAccounts {
    public List<LinkedAccount> linkedAccounts;

    public static class LinkedAccount {
        public String provider;

        public String accountName;

        @Override
        public String toString() {
            return ToStringUtils.getString(this);
        }
    }
}
