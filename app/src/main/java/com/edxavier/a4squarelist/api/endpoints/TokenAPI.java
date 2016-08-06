package com.edxavier.a4squarelist.api.endpoints;

import com.edxavier.a4squarelist.api.apiModel.Token;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Eder Xavier Rojas on 04/08/2016.
 */
public interface TokenAPI {
    public static String grant_type = "authorization_code";
    @GET("oauth2/access_token?")
    Observable<Token> getAccessToken(@Query("client_id") String client_id,
                                     @Query("client_secret") String client_secret,
                                     @Query("grant_type") String grant_type,
                                     @Query("code") String code);

}
