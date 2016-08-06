package com.edxavier.a4squarelist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.edxavier.a4squarelist.adapter.AdapterVenues;
import com.edxavier.a4squarelist.api.ApiClient;
import com.edxavier.a4squarelist.api.apiModel.Item_;
import com.edxavier.a4squarelist.api.endpoints.TokenAPI;
import com.edxavier.a4squarelist.api.endpoints.VenuesAPI;
import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxCompoundButton;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.RxTextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;
import jp.wasabeef.recyclerview.adapters.SlideInBottomAnimationAdapter;
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator;
import retrofit2.Retrofit;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_FSQ_CONNECT = 1;
    SharedPreferences sharedPref;
    String TOKEN = "";
    String RADIO = "";
    String QUERY = "";
    double RATING = 0;
    AdapterVenues adapter;
    MaterialDialog dialog;
    @Bind(R.id.recycler_venues)
    RecyclerView recyclerVenues;
    @Bind(R.id.swipe)
    SwipeRefreshLayout swipe;
    @Bind(R.id.empty_msg)
    TextView emptyMsg;

    List<Item_> venues = new ArrayList<>();
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    boolean SWIPED = false;
    Subscription subscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }
        setupRecycler();
        setData(venues);
        sharedPref = getPreferences(Context.MODE_PRIVATE);
        TOKEN = sharedPref.getString("token", "");
        RADIO = sharedPref.getString("radio", "5000");
        RATING = sharedPref.getFloat("rating", 1);

        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                venues.clear();
                adapter.removeAll();
                //getData();
                SWIPED = true;
                getLocation();
            }
        });


    }

    void setupDataLoad(){
        if (TOKEN.isEmpty()) {
            Intent intent = FoursquareOAuth.getConnectIntent(this, BuildConfig.FOURSQUARE_CLIENT_ID);
            boolean play = FoursquareOAuth.isPlayStoreIntent(intent);
            if (!play) {
                startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
            } else {
                Toast.makeText(this, "No tienes instalada la app Foursquare", Toast.LENGTH_LONG).show();
                try {
                    startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
                } catch (Exception e) {
                    new MaterialDialog.Builder(this)
                            .title(R.string.info_title)
                            .content("No se pudo lanzar el play store")
                            .positiveText(R.string.agree)
                            .show();
                }
            }
        } else {
            if(!SWIPED) {
                dialog = new MaterialDialog.Builder(this)
                        .title(R.string.progress_dialog)
                        .content(R.string.please_wait)
                        .progress(true, 0)
                        .show();
            }
            getData();
        }
    }

    private void getData() {
        Retrofit retrofit = ApiClient.getClient(ApiClient.BASE_URL);
        VenuesAPI venuesAPI = retrofit.create(VenuesAPI.class);
        String coord = lastLocation.getLatitude()+","+lastLocation.getLongitude();
        subscription =  venuesAPI.getVenues(coord, RADIO, TOKEN, VenuesAPI.v, "1", "500", QUERY)
                .subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread())
                .flatMap(venueResponse -> Observable.from(venueResponse.getResponse().getGroups().get(0).getItems()))
                .filter(item_1 -> {
                    boolean ok = false;
                    if (item_1.getVenue().getRating() != null) {
                        ok = item_1.getVenue().getRating() >= RATING;
                    }
                    return ok;
                })
                .doOnCompleted(() -> {
                    dismisDlg();
                    QUERY = "";
                    swipe.setRefreshing(false);
                    setData(venues);
                })
                .subscribe(item_ -> {
                    //Log.e("EDER", item_.getVenue().getName());
                    venues.add(item_);
                }, error -> {
                    try {
                        String content = getResources().getString(R.string.content);
                        dismisDlg();
                        swipe.setRefreshing(false);
                        new MaterialDialog.Builder(this)
                                .content(content + " " + error.getMessage())
                                .positiveText(R.string.agree)
                                .show();
                    }catch (Exception ignored){}
                });/*
                .subscribe(response -> {
                    if (response.getMeta().getCode() == 200) {
                        if (!response.getResponse().getGroups().isEmpty()) {
                            setData(response.getResponse().getGroups().get(0).getItems());
                        }
                    }
                }, error -> {
                    String content = getResources().getString(R.string.content);
                    dismisDlg();
                    swipe.setRefreshing(false);
                    new MaterialDialog.Builder(this)
                            .content(content +" " +error.getMessage())
                            .positiveText(R.string.agree)
                            .show();
                });

                */
    }

    private void setupRecycler() {
        recyclerVenues.setLayoutManager(new LinearLayoutManager(this));
        recyclerVenues.setHasFixedSize(true);
        recyclerVenues.setItemAnimator(new SlideInUpAnimator(new OvershootInterpolator(1f)));
    }

    private void setData(List<Item_> items) {
        adapter = new AdapterVenues(items);
        recyclerVenues.setAdapter(new SlideInBottomAnimationAdapter(new ScaleInAnimationAdapter(adapter)));
        swipe.setRefreshing(false);
        dismisDlg();
        if (items.isEmpty()) {
            emptyMsg.setVisibility(View.VISIBLE);
        } else {
            emptyMsg.setVisibility(View.GONE);
        }
    }

    private void dismisDlg() {
        if (dialog != null) {
            if (dialog.isShowing())
                dialog.dismiss();
        }
    }


    void getToken(String code) {
        Retrofit retrofit = ApiClient.getTokenClient(ApiClient.TOKEN_URL);
        TokenAPI tokenAPI = retrofit.create(TokenAPI.class);
        subscription = tokenAPI.getAccessToken(BuildConfig.FOURSQUARE_CLIENT_ID, BuildConfig.FOURSQUARE_CLIENT_SECRET,
                TokenAPI.grant_type, code)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(token -> {
                    TOKEN = token.getAccessToken();
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("token", TOKEN);
                    editor.commit();
                    getData();
                }, error -> {
                    try {
                        String content = getResources().getString(R.string.content);
                        dismisDlg();
                        swipe.setRefreshing(false);
                        new MaterialDialog.Builder(this)
                                .content(content + " " + error.getMessage())
                                .positiveText(R.string.agree)
                                .show();
                    }catch (Exception ignored){}
                });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_FSQ_CONNECT:
                AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
                try {
                    if(!codeResponse.getCode().isEmpty()) {
                        getToken(codeResponse.getCode());
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "No fue posible obtener el Token", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                MaterialDialog fDialog = new MaterialDialog.Builder(this)
                        .title(R.string.filter)
                        .customView(R.layout.filter_view, true)
                        .positiveText(R.string.filter_btn)
                        .positiveColor(getResources().getColor(R.color.colorPrimary))
                        .negativeText(android.R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                swipe.setRefreshing(true);
                                venues.clear();
                                adapter.removeAll();
                                getData();
                            }
                        }).build();

                ButterKnife.bind(fDialog);
                EditText query = (EditText) fDialog.getCustomView().findViewById(R.id.query);
                TextView rating = (TextView) fDialog.getCustomView().findViewById(R.id.txtRating);
                TextView distance = (TextView) fDialog.getCustomView().findViewById(R.id.txtDistance);
                SeekBar ratingBar = (SeekBar) fDialog.getCustomView().findViewById(R.id.rating_bar);
                SeekBar distanceBar = (SeekBar) fDialog.getCustomView().findViewById(R.id.distance_bar);

                rating.setText(String.valueOf(RATING));
                distance.setText(RADIO);

                ratingBar.setProgress((int) ((RATING - 1) * 2));
                distanceBar.setProgress((Integer.valueOf(RADIO) - 1000) / 1000);

                RxSeekBar.changes(ratingBar).subscribe(value -> {
                    double rate = (value + 1d) / 2d;
                    rating.setText(String.format(Locale.getDefault(), "%.1f", rate));
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putFloat("rating", (float) rate);
                    RATING = rate;
                    editor.commit();
                });

                RxSeekBar.changes(distanceBar).subscribe(value -> {
                    distance.setText(String.valueOf(value + 1) + " km");
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("radio", String.valueOf(((value + 1) * 1000)));
                    RADIO = String.valueOf(((value + 1) * 1000));
                    editor.commit();
                });
                RxTextView.textChangeEvents(query).subscribe(e -> QUERY = e.text().toString());
                fDialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Crear google api client object
     * */
    protected synchronized void buildGoogleApiClient() {

        if (googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    /**
     * Metodo para verificar google play services
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Este dispositivo no esta soportado.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Metodo para obtener la ubicacion
     * */
    private void getLocation() {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) && lastLocation ==null) {
            swipe.setRefreshing(false);
            new MaterialDialog.Builder(this)
                    .content("Porfavor asegurate de activar tu gps")
                    .positiveText(R.string.agree)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .show();
            return;
        }

        Location location;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling permissions request
            return;
        }else {
            location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if(location!=null)
                lastLocation = location;
        }

        if (lastLocation != null) {
            setupDataLoad();
        } else {
            Toast.makeText(getApplicationContext(),
                    "No se pudo obtener la ubicacion.", Toast.LENGTH_LONG)
                    .show();
            swipe.setRefreshing(false);

        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(subscription!=null) {
            if (!subscription.isUnsubscribed()) {
                subscription.unsubscribe();
            }
        }
    }
}
