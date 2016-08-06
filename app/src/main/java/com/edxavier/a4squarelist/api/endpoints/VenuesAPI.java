package com.edxavier.a4squarelist.api.endpoints;

import com.edxavier.a4squarelist.api.apiModel.VenueResponse;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by Eder Xavier Rojas on 03/08/2016.
 */
public interface VenuesAPI {
    public static String v = "20160802";

    @GET("venues/explore?")
    Observable<VenueResponse> getVenues(@Query("ll") String lat_lon, @Query("radius") String radius,
                                        @Query("oauth_token") String oauth_token,
                                        @Query("v") String v, @Query("sortByDistance") String sortByDistance,
                                        @Query("limit") String limit,@Query("query") String query);
}
