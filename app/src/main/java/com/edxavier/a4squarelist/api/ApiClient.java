package com.edxavier.a4squarelist.api;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Eder Xavier Rojas on 03/08/2016.
 */
public class ApiClient {
    public static final String BASE_URL = "https://api.foursquare.com/v2/";
    public static final String TOKEN_URL = "https://foursquare.com/";
    private static Retrofit retrofit = null;
    private static Retrofit retrofitToken = null;

    public static Retrofit getClient(String URL) {
        if (retrofit==null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }
        return retrofit;
    }
    public static Retrofit getTokenClient(String URL) {
        if (retrofitToken==null) {
            retrofitToken = new Retrofit.Builder()
                    .baseUrl(URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }
        return retrofitToken;
    }
}
