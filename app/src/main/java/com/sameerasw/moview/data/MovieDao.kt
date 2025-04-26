package com.sameerasw.moview.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MovieDao {

    // --- Add this method ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: Movie) // For saving single movie (Task 4)

    // Keep existing methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<Movie>) // For initial load (Task 2)

    @Query("SELECT * FROM movies WHERE LOWER(actors) LIKE '%' || LOWER(:actorName) || '%'")
    suspend fun findMoviesByActor(actorName: String): List<Movie> // For Task 5

    @Query("SELECT * FROM movies")
    suspend fun getAllMovies(): List<Movie> // For verification/debugging
}