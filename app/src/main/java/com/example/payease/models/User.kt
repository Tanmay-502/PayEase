package com.example.payease.models

data class User(
    val userId: Long = 0,
    val fullName: String,
    val email: String,
    val mobile: String,
    val passwordHash: String
)
