package com.edxavier.a4squarelist.api.apiModel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Eder Xavier Rojas on 04/08/2016.
 */
public class Token {

    @SerializedName("access_token")
    @Expose
    private String accessToken;

    /**
     *
     * @return
     * The accessToken
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     *
     * @param accessToken
     * The access_token
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
