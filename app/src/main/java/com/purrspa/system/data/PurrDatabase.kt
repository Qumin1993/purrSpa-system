package com.purrspa.system.data

import android.content.Context
import androidx.room.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class Client(@PrimaryKey val id: String, val name: String, val phone: String, val email: String = "", val address: String = "")

@Entity(tableName = "cats", foreignKeys = [ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("clientId")])
data class Cat(@PrimaryKey val id: String, val clientId: String, val name: String, val breed: String = "", val notes: String = "", val sex: String = "", val dateOfBirth: String = "", val neutered: Boolean = false, val healthNotes: String = "")

@Entity(tableName = "visits", foreignKeys = [ForeignKey(entity = Cat::class, parentColumns = ["id"], childColumns = ["catId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("catId"), Index("startMillis")])
data class Visit(@PrimaryKey val id: String, val catId: String, val startMillis: Long, val service: String, val location: String, val pricePence: Long, val status: String = "SCHEDULED", val notes: String = "", val paymentStatus: String = "UNPAID", val paymentMethod: String = "", val travelFeePence: Long = 0, val depositPaidPence: Long = 0)

@Entity(tableName = "grooming_assessments", foreignKeys = [ForeignKey(entity = Visit::class, parentColumns = ["id"], childColumns = ["visitId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["visitId"], unique = true)])
data class GroomingAssessment(
    @PrimaryKey val id: String,
    val visitId: String,
    val brushing: Int = -1,
    val bathing: Int = -1,
    val drying: Int = -1,
    val nailTrim: Int = -1,
    val paws: Int = -1,
    val belly: Int = -1,
    val tail: Int = -1,
    val coatCondition: String = "",
    val recommendations: String = "",
    val updatedMillis: Long = System.currentTimeMillis()
)

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM grooming_assessments ORDER BY updatedMillis DESC")
    fun observe(): Flow<List<GroomingAssessment>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroomingAssessment)
}

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun observe(): Flow<List<Client>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Client)
    @Update suspend fun update(item: Client)
    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1") suspend fun get(id: String): Client?
}
@Dao
interface CatDao {
    @Query("SELECT * FROM cats ORDER BY name COLLATE NOCASE") fun observe(): Flow<List<Cat>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Cat)
    @Update suspend fun update(item: Cat)
    @Query("SELECT * FROM cats WHERE id = :id LIMIT 1") suspend fun get(id: String): Cat?
}
@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY startMillis DESC") fun observe(): Flow<List<Visit>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Visit)
    @Update suspend fun update(item: Visit)
    @Query("UPDATE visits SET paymentStatus = :status, paymentMethod = :method WHERE id = :id")
    suspend fun updatePayment(id: String, status: String, method: String): Int
    @Query("UPDATE visits SET travelFeePence = :travel, depositPaidPence = :deposit WHERE id = :id")
    suspend fun updateCharges(id: String, travel: Long, deposit: Long): Int
    @Query("UPDATE visits SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String): Int
    @Query("SELECT * FROM visits WHERE status NOT IN ('CANCELLED', 'NO_SHOW') AND startMillis < :endExclusive AND startMillis > :earliestStart LIMIT 1")
    suspend fun findOverlapping(endExclusive: Long, earliestStart: Long): Visit?
    @Query("UPDATE visits SET status = :next WHERE id = :id AND status = :expected")
    suspend fun transitionStatus(id: String, expected: String, next: String): Int
}
@Database(entities = [Client::class, Cat::class, Visit::class, GroomingAssessment::class], version = 5, exportSchema = true)
abstract class PurrDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
    abstract fun cats(): CatDao
    abstract fun visits(): VisitDao
    abstract fun assessments(): AssessmentDao
    suspend fun insertVisitIfFree(item: Visit): Boolean = withTransaction {
        val duration = AppointmentRules.DEFAULT_DURATION_MINUTES * 60_000L
        val endExclusive = Math.addExact(item.startMillis, duration)
        val earliestStart = Math.subtractExact(item.startMillis, duration)
        if (visits().findOverlapping(endExclusive, earliestStart) != null) false
        else {
            visits().insert(item)
            true
        }
    }

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS grooming_assessments (
                    id TEXT NOT NULL PRIMARY KEY, visitId TEXT NOT NULL,
                    brushing INTEGER NOT NULL, bathing INTEGER NOT NULL,
                    drying INTEGER NOT NULL, nailTrim INTEGER NOT NULL,
                    paws INTEGER NOT NULL, belly INTEGER NOT NULL,
                    tail INTEGER NOT NULL, coatCondition TEXT NOT NULL,
                    recommendations TEXT NOT NULL, updatedMillis INTEGER NOT NULL,
                    FOREIGN KEY(visitId) REFERENCES visits(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_grooming_assessments_visitId ON grooming_assessments(visitId)")
            }
        }
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cats ADD COLUMN sex TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cats ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE cats ADD COLUMN neutered INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cats ADD COLUMN healthNotes TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'UNPAID'")
                db.execSQL("ALTER TABLE visits ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE visits ADD COLUMN travelFeePence INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE visits ADD COLUMN depositPaidPence INTEGER NOT NULL DEFAULT 0")
            }
        }
        @Volatile private var instance: PurrDatabase? = null
        fun get(context: Context): PurrDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PurrDatabase::class.java, "purrspa.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}
