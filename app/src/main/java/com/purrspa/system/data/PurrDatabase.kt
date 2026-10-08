package com.purrspa.system.data

import android.content.Context
import androidx.room.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class Client(@PrimaryKey val id: String, val name: String, val phone: String, val email: String = "", val address: String = "")

@Entity(tableName = "cats", foreignKeys = [ForeignKey(entity = Client::class, parentColumns = ["id"], childColumns = ["clientId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("clientId")])
data class Cat(@PrimaryKey val id: String, val clientId: String, val name: String, val breed: String = "", val notes: String = "", val sex: String = "", val dateOfBirth: String = "", val neutered: Boolean = false, val healthNotes: String = "", val photoConsent: Boolean = false, val socialConsent: Boolean = false, val consentUpdatedMillis: Long = 0L)

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

@Entity(tableName = "consent_events", foreignKeys = [ForeignKey(entity = Cat::class, parentColumns = ["id"], childColumns = ["catId"], onDelete = ForeignKey.CASCADE)], indices = [Index("catId"), Index("recordedMillis")])
data class ConsentEvent(
    @PrimaryKey val id: String,
    val catId: String,
    val photoAllowed: Boolean,
    val socialAllowed: Boolean,
    val recordedMillis: Long,
    val source: String = "STAFF_RECORDED"
)

/** Owner-reported information, kept separate from observed grooming assessments. */
@Entity(tableName = "owner_intakes", foreignKeys = [ForeignKey(entity = Cat::class, parentColumns = ["id"], childColumns = ["catId"], onDelete = ForeignKey.CASCADE)], indices = [Index(value = ["catId"], unique = true)])
data class OwnerIntake(
    @PrimaryKey val id: String,
    val catId: String,
    val healthConditions: String = "",
    val medications: String = "",
    val allergies: String = "",
    val previousGrooming: String = "",
    val behaviourTriggers: String = "",
    val handlingAdvice: String = "",
    val brushingTolerance: String = "UNKNOWN",
    val bathingTolerance: String = "UNKNOWN",
    val dryerTolerance: String = "UNKNOWN",
    val nailsTolerance: String = "UNKNOWN",
    val source: String = "STAFF_RECORDED",
    val updatedMillis: Long = 0L
)

@Dao
interface OwnerIntakeDao {
    @Query("SELECT * FROM owner_intakes ORDER BY updatedMillis DESC")
    fun observe(): Flow<List<OwnerIntake>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: OwnerIntake)
}

@Dao
interface ConsentEventDao {
    @Query("SELECT * FROM consent_events ORDER BY recordedMillis DESC")
    fun observe(): Flow<List<ConsentEvent>>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: ConsentEvent)
}

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
    @Query("UPDATE cats SET name = :name, breed = :breed, notes = :notes, sex = :sex, dateOfBirth = :dob, neutered = :neutered, healthNotes = :health WHERE id = :id")
    suspend fun updateProfile(id: String, name: String, breed: String, notes: String, sex: String, dob: String, neutered: Boolean, health: String): Int
    @Query("UPDATE cats SET photoConsent = :photo, socialConsent = :social, consentUpdatedMillis = :updated WHERE id = :id")
    suspend fun updateConsents(id: String, photo: Boolean, social: Boolean, updated: Long): Int
    @Query("SELECT * FROM cats WHERE id = :id LIMIT 1") suspend fun get(id: String): Cat?
}
@Dao
interface VisitDao {
    @Query("SELECT * FROM visits ORDER BY startMillis DESC") fun observe(): Flow<List<Visit>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(item: Visit)
    @Update suspend fun update(item: Visit)
    @Query("UPDATE visits SET paymentStatus = :status, paymentMethod = :method WHERE id = :id AND (:status != 'PAID' OR status NOT IN ('CANCELLED', 'NO_SHOW'))")
    suspend fun updatePayment(id: String, status: String, method: String): Int
    @Query("UPDATE visits SET travelFeePence = :travel, depositPaidPence = :deposit WHERE id = :id AND :travel >= 0 AND :deposit >= 0 AND :deposit <= pricePence + :travel")
    suspend fun updateCharges(id: String, travel: Long, deposit: Long): Int
    @Query("UPDATE visits SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String): Int
    @Query("SELECT * FROM visits WHERE status NOT IN ('CANCELLED', 'NO_SHOW') AND startMillis < :endExclusive AND startMillis > :earliestStart LIMIT 1")
    suspend fun findOverlapping(endExclusive: Long, earliestStart: Long): Visit?
    @Query("UPDATE visits SET status = :next WHERE id = :id AND status = :expected")
    suspend fun transitionStatus(id: String, expected: String, next: String): Int
}
@Database(entities = [Client::class, Cat::class, Visit::class, GroomingAssessment::class, ConsentEvent::class, OwnerIntake::class], version = 8, exportSchema = true)
abstract class PurrDatabase : RoomDatabase() {
    abstract fun clients(): ClientDao
    abstract fun cats(): CatDao
    abstract fun visits(): VisitDao
    abstract fun assessments(): AssessmentDao
    abstract fun ownerIntakes(): OwnerIntakeDao
    abstract fun consentEvents(): ConsentEventDao
    suspend fun recordConsent(event: ConsentEvent): Boolean = withTransaction {
        val cat = cats().get(event.catId) ?: return@withTransaction false
        val photo = event.photoAllowed
        val social = photo && event.socialAllowed
        val updated = cats().updateConsents(cat.id, photo, social, event.recordedMillis)
        if (updated != 1) return@withTransaction false
        consentEvents().insert(event.copy(socialAllowed = social))
        true
    }

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
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cats ADD COLUMN photoConsent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cats ADD COLUMN socialConsent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cats ADD COLUMN consentUpdatedMillis INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS consent_events (id TEXT NOT NULL PRIMARY KEY, catId TEXT NOT NULL, photoAllowed INTEGER NOT NULL, socialAllowed INTEGER NOT NULL, recordedMillis INTEGER NOT NULL, source TEXT NOT NULL, FOREIGN KEY(catId) REFERENCES cats(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_consent_events_catId ON consent_events(catId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_consent_events_recordedMillis ON consent_events(recordedMillis)")
            }
        }
        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS owner_intakes (
                    id TEXT NOT NULL PRIMARY KEY, catId TEXT NOT NULL,
                    healthConditions TEXT NOT NULL, medications TEXT NOT NULL,
                    allergies TEXT NOT NULL, previousGrooming TEXT NOT NULL,
                    behaviourTriggers TEXT NOT NULL, handlingAdvice TEXT NOT NULL,
                    brushingTolerance TEXT NOT NULL, bathingTolerance TEXT NOT NULL,
                    dryerTolerance TEXT NOT NULL, nailsTolerance TEXT NOT NULL,
                    source TEXT NOT NULL, updatedMillis INTEGER NOT NULL,
                    FOREIGN KEY(catId) REFERENCES cats(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )""".trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_owner_intakes_catId ON owner_intakes(catId)")
            }
        }
        @Volatile private var instance: PurrDatabase? = null
        fun get(context: Context): PurrDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, PurrDatabase::class.java, "purrspa.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8).build().also { instance = it }
        }
    }
}
