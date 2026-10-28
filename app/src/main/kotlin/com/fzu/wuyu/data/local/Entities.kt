package com.fzu.wuyu.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("semesterId"), Index("subcategoryId")]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val subcategoryId: String,
    val title: String,
    val score: Float,
    val dateEpochDay: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

/** 某学期的综测参数：平均学分绩点 + 五育基础性得分（达标 70，未达标 50） */
@Entity(
    tableName = "semester_assessments",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("semesterId")]
)
data class AssessmentEntity(
    @PrimaryKey val semesterId: Long,
    /** 平均学分绩点；null = 未填写 */
    val gpa: Float? = null,
    val basicA: Float = 70f,
    val basicB: Float = 70f,
    val basicC: Float = 70f,
    val basicD: Float = 70f,
    val basicE: Float = 70f
)
