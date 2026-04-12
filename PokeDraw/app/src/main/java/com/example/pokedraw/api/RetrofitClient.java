package com.example.pokedraw.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://pokeapi.co/api/v2/";
    private static RetrofitClient instance;
    private final PokeApiService apiService;

    private RetrofitClient() {
        apiService = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PokeApiService.class);
    }

    public static RetrofitClient getInstance() {
        if (instance == null) instance = new RetrofitClient();
        return instance;
    }

    public PokeApiService getApiService() { return apiService; }
}
