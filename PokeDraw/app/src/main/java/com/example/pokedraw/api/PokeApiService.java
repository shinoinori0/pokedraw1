package com.example.pokedraw.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface PokeApiService {
    @GET("pokemon?limit=1025")
    Call<PokemonListResponse> getPokemonList();

    @GET("pokemon/{id}")
    Call<PokemonResponse> getPokemon(@Path("id") int id);

    @GET("pokemon-species/{id}")
    Call<PokemonSpeciesResponse> getPokemonSpecies(@Path("id") int id);
}
