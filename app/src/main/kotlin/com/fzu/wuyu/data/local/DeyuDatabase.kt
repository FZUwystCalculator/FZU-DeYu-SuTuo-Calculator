package com.fzu.wuyu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SemesterEntity::class, RecordEntity::class, AssessmentEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DeyuDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun recordDao(): RecordDao
    abstract fun assessmentDao(): AssessmentDao

    companion object {
        @Volatile
        private var instance: DeyuDatabase? = null

        /** v1 → v2：新增 semester_assessments 表（综测参数）。
         *  注意：列定义必须与 Room 从实体生成的期望结构一致——Kotlin 默认值不产生 SQL DEFAULT 子句，
         *  故此处不能写 DEFAULT，否则升级时表结构校验失败。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS semester_assessments (" +
                        "semesterId INTEGER NOT NULL PRIMARY KEY, " +
                        "gpa REAL, " +
                        "basicA REAL NOT NULL, " +
                        "basicB REAL NOT NULL, " +
                        "basicC REAL NOT NULL, " +
                        "basicD REAL NOT NULL, " +
                        "basicE REAL NOT NULL, " +
                        "FOREIGN KEY(semesterId) REFERENCES semesters(id) " +
                        "ON DELETE CASCADE ON UPDATE NO ACTION)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_semester_assessments_semesterId ON semester_assessments(semesterId)")
            }
        }

        fun get(context: Context): DeyuDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DeyuDatabase::class.java,
                    "wuyu.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}