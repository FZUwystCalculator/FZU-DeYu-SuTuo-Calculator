package com.fzu.wuyu.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters ORDER BY createdAt DESC")
    suspend fun getAll(): List<SemesterEntity>

    @Query("SELECT * FROM semesters WHERE id = :id")
    suspend fun getById(id: Long): SemesterEntity?

    @Insert
    suspend fun insert(semester: SemesterEntity): Long

    @Update
    suspend fun update(semester: SemesterEntity)

    @Delete
    suspend fun delete(semester: SemesterEntity)

    @Query("SELECT COUNT(*) FROM semesters")
    suspend fun count(): Int
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY semesterId, dateEpochDay DESC")
    fun observeAll(): Flow<List<RecordEntity>>

    @Insert
    suspend fun insert(record: RecordEntity): Long

    @Insert
    suspend fun insertAll(records: List<RecordEntity>)

    @Update
    suspend fun update(record: RecordEntity)

    @Delete
    suspend fun delete(record: RecordEntity)

    @Query("DELETE FROM records WHERE semesterId = :semesterId")
    suspend fun deleteBySemester(semesterId: Long)

    @Query("DELETE FROM records")
    suspend fun deleteAll()
}

@Dao
interface AssessmentDao {
    @Query("SELECT * FROM semester_assessments")
    fun observeAll(): Flow<List<AssessmentEntity>>

    @Upsert
    suspend fun upsert(assessment: AssessmentEntity)

    @Query("DELETE FROM semester_assessments")
    suspend fun deleteAll()
}
