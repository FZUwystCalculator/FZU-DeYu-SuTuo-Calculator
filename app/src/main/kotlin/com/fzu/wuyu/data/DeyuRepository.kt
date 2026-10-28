package com.fzu.wuyu.data

import com.fzu.wuyu.data.local.AssessmentEntity
import com.fzu.wuyu.data.local.DeyuDatabase
import com.fzu.wuyu.data.local.RecordEntity
import com.fzu.wuyu.data.local.SemesterEntity
import com.fzu.wuyu.domain.model.AssessmentBackup
import com.fzu.wuyu.domain.model.DeyuRecord
import com.fzu.wuyu.domain.model.DeyuSemester
import com.fzu.wuyu.domain.model.SemesterAssessment
import com.fzu.wuyu.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 数据仓库：统一封装 Room 访问。
 * 首次启动自动创建当前学期（如 "2026-2027学年第一学期"）。
 */
class DeyuRepository(private val db: DeyuDatabase) {

    private val semesterDao = db.semesterDao()
    private val recordDao = db.recordDao()
    private val assessmentDao = db.assessmentDao()

    fun observeSemesters(): Flow<List<DeyuSemester>> =
        semesterDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAllRecords(): Flow<List<DeyuRecord>> =
        recordDao.observeAll().map { list -> list.map { it.toDomain() } }

    /** 各学期综测参数（绩点 + 五育基础性得分） */
    fun observeAssessments(): Flow<List<SemesterAssessment>> =
        assessmentDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun ensureDefaultSemester(): Long {
        if (semesterDao.count() > 0) {
            return semesterDao.getAll().first().id
        }
        return semesterDao.insert(
            SemesterEntity(name = DateUtils.currentSemesterName())
        )
    }

    suspend fun addSemester(name: String): Long =
        semesterDao.insert(SemesterEntity(name = name.trim()))

    suspend fun renameSemester(id: Long, newName: String) {
        val s = semesterDao.getById(id) ?: return
        semesterDao.update(s.copy(name = newName.trim()))
    }

    suspend fun deleteSemester(id: Long) {
        val s = semesterDao.getById(id) ?: return
        recordDao.deleteBySemester(id)
        semesterDao.delete(s)
    }

    suspend fun addRecord(record: DeyuRecord): Long =
        recordDao.insert(record.toEntity())

    suspend fun updateRecord(record: DeyuRecord) =
        recordDao.update(record.toEntity())

    suspend fun deleteRecord(record: DeyuRecord) =
        recordDao.delete(record.toEntity())

    /** 导入用：清空后整体写入（学期 id 重新生成，记录按旧 id 映射到新 id，综测参数按学期名映射） */
    suspend fun replaceAll(
        semesters: List<DeyuSemester>,
        records: List<DeyuRecord>,
        assessments: List<AssessmentBackup> = emptyList()
    ) {
        recordDao.deleteAll()
        assessmentDao.deleteAll()
        semesterDao.getAll().forEach { semesterDao.delete(it) }
        val idMap = HashMap<Long, Long>()
        val nameMap = HashMap<String, Long>()
        semesters.forEach { s ->
            val newId = semesterDao.insert(SemesterEntity(name = s.name, createdAt = s.createdAt))
            idMap[s.id] = newId
            nameMap.putIfAbsent(s.name, newId)
        }
        val entities = records.mapNotNull { r ->
            idMap[r.semesterId]?.let { sid ->
                RecordEntity(
                    semesterId = sid,
                    subcategoryId = r.subcategoryId,
                    title = r.title,
                    score = r.score,
                    dateEpochDay = r.dateEpochDay,
                    note = r.note,
                    createdAt = r.createdAt
                )
            }
        }
        recordDao.insertAll(entities)
        assessments.forEach { ab ->
            nameMap[ab.semesterName]?.let { sid ->
                assessmentDao.upsert(
                    AssessmentEntity(
                        semesterId = sid,
                        gpa = ab.gpa,
                        basicA = ab.basicA,
                        basicB = ab.basicB,
                        basicC = ab.basicC,
                        basicD = ab.basicD,
                        basicE = ab.basicE
                    )
                )
            }
        }
    }

    /** 保存某学期的综测参数 */
    suspend fun saveAssessment(assessment: SemesterAssessment) {
        assessmentDao.upsert(
            AssessmentEntity(
                semesterId = assessment.semesterId,
                gpa = assessment.gpa,
                basicA = assessment.basicA,
                basicB = assessment.basicB,
                basicC = assessment.basicC,
                basicD = assessment.basicD,
                basicE = assessment.basicE
            )
        )
    }
}

private fun SemesterEntity.toDomain() = DeyuSemester(id = id, name = name, createdAt = createdAt)

private fun DeyuSemester.toEntity() = SemesterEntity(id = id, name = name, createdAt = createdAt)

private fun RecordEntity.toDomain() = DeyuRecord(
    id = id,
    semesterId = semesterId,
    subcategoryId = subcategoryId,
    title = title,
    score = score,
    dateEpochDay = dateEpochDay,
    note = note,
    createdAt = createdAt
)

private fun DeyuRecord.toEntity() = RecordEntity(
    id = id,
    semesterId = semesterId,
    subcategoryId = subcategoryId,
    title = title,
    score = score,
    dateEpochDay = dateEpochDay,
    note = note,
    createdAt = if (createdAt == 0L) System.currentTimeMillis() else createdAt
)

private fun AssessmentEntity.toDomain() = SemesterAssessment(
    semesterId = semesterId,
    gpa = gpa,
    basicA = basicA,
    basicB = basicB,
    basicC = basicC,
    basicD = basicD,
    basicE = basicE
)
