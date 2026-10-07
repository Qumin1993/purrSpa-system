package com.purrspa.system.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class Client(@PrimaryKey val id: String, val name: String, val phone: String, val email: String = "", val address: String = "")

@Entity(tableName = "cats", foreignKeys = [ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("clientId")])
data class Cat(@PrimaryKey val id: String, val clientId: String, val name: String, val breed: String = "", val notes: String = "")

@Entity(tableName = "visits", foreignKeys = [ForeignKey(entity = Cat::class, parentColumns = ["id"], childColumns = ["catId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("catId"), Index("startMillis")])
data class Visit(@PrimaryKey val id: String, val catId: String, val startMillis: Long, val service: String, val location: String, val pricePence: Long, val status: String = "SCHEDULED", val notes: String = "")

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun observe(): Flow<List<Client>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Client)
    @Update suspend fun update(item: Client)
}
@Dao
interface CatDao {
    @Query("SELECT * FROM cats ORDER BY name COLLATE NOCASE") fun observe(): Flow<List<Cat>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Cat)
    @Update suspend fun update(item: Cat)
}
@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY startMillis DESC") fun observe(): Flow<List<Visit>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Visit)
    @Update suspend fun update(item: Visit)
}
@Database(entities = [Client::class, Cat::class, Visit::class], version = 1, exportSchema = true)
abstract class PurrDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
    abstract fun cats(): CatDao
    abstract fun visits(): VisitDao
    companion object {
        @Volatile private var instance: PurrDatabase? = null
        fun get(context: Context): PurrDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PurrDatabase::class.java, "purrspa.db").build().also { instance = it }
        }
    }
}
