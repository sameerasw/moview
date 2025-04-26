package com.sameerasw.moview.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
    // primary key
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,

    val title: String?,
    val year: String?,
    val rated: String?,
    val released: String?,
    val runtime: String?,
    val genre: String?,
    val director: String?,
    val writer: String?,
    val actors: String?,
    val plot: String?,
    val poster: String?,
    val imdbRating: String?,
    val type: String?
)