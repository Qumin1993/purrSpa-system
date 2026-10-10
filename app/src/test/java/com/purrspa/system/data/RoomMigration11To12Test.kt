package com.purrspa.system.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomMigration11To12Test {
    @Test fun preservesExistingAssessmentAndAddsGroomerFields() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-11-12-${System.nanoTime()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(11) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE grooming_assessments (id TEXT NOT NULL PRIMARY KEY, visitId TEXT NOT NULL, brushing INTEGER NOT NULL, bathing INTEGER NOT NULL, drying INTEGER NOT NULL, nailTrim INTEGER NOT NULL, paws INTEGER NOT NULL, belly INTEGER NOT NULL, tail INTEGER NOT NULL, coatCondition TEXT NOT NULL, recommendations TEXT NOT NULL, updatedMillis INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO grooming_assessments VALUES ('a1','v1',2,1,-1,-1,-1,-1,-1,'Dry coat','Brush weekly',123)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        PurrDatabase.MIGRATION_11_12.migrate(db)
                    }
                }).build()
        )
        try {
            helper.writableDatabase.query("SELECT * FROM grooming_assessments WHERE id = 'a1'").use { cursor ->
                org.junit.Assert.assertTrue(cursor.moveToFirst())
                assertEquals("Dry coat", cursor.getString(cursor.getColumnIndexOrThrow("coatCondition")))
                assertEquals("Brush weekly", cursor.getString(cursor.getColumnIndexOrThrow("recommendations")))
            }
            helper.close()
            val upgraded = FrameworkSQLiteOpenHelperFactory().create(
                SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                    .callback(object : SupportSQLiteOpenHelper.Callback(12) {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                            PurrDatabase.MIGRATION_11_12.migrate(db)
                        }
                    }).build()
            )
            try {
                upgraded.writableDatabase.query("SELECT * FROM grooming_assessments WHERE id = 'a1'").use { cursor ->
                    org.junit.Assert.assertTrue(cursor.moveToFirst())
                    assertEquals("Dry coat", cursor.getString(cursor.getColumnIndexOrThrow("coatCondition")))
                    assertEquals("Brush weekly", cursor.getString(cursor.getColumnIndexOrThrow("recommendations")))
                    for (field in listOf("aggressionNotes", "sensitiveAreas", "techniquesUsed")) {
                        assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow(field)))
                    }
                }
            } finally { upgraded.close() }
        } finally {
            helper.close()
            context.deleteDatabase(name)
        }
    }
}
