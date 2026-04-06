package com.retailone.pos.localstorage.RoomDB

import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email AND pin = :pin")
    suspend fun getUserByCredentials(email: String, pin: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("DELETE FROM users WHERE email = :email")
    suspend fun deleteUser(email: String)

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}
