package com.patch.patchcalling.services;


import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by shivamsharma on 21/07/18.
 */

public class RetrofitClient {
    public static final String Base_url = "https://apiv2.patchus.in/api/v2/";  //Staging
    //public static final String Base_url = "https://pbapi.patchus.in/api/v2/";        //Production
    //public static final String Base_url = "https://papigcp-demo.patchus.in/api/v2/";  //Staging
    public static final String logger_url = "https://mel.patchus.in/";        //Arango_DB for logging

    public static Retrofit retrofit = null, retrofit1 = null;
    public static Retrofit retrofit2 = null;


    public static Retrofit getClient() {
        if (retrofit == null) {
//            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
//            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(Base_url)
                    //.client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

        }
        return retrofit;
    }

    public static Retrofit getApiClient(String url) {
        if (retrofit1 == null) {
            /*HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();*/
            retrofit1 = new Retrofit.Builder()
                    .baseUrl(url)
                    //.client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

        }
        return retrofit1;
    }

    public static Retrofit getLoggingClient() {
        if (retrofit2 == null) {
            retrofit2 = new Retrofit.Builder()
                    .baseUrl(logger_url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit2;
    }

}
