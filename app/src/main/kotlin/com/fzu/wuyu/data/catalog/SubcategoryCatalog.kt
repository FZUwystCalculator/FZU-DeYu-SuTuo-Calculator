package com.fzu.wuyu.data.catalog

import com.fzu.wuyu.domain.model.BasicEvaluationInfo
import com.fzu.wuyu.domain.model.CapType
import com.fzu.wuyu.domain.model.Category
import com.fzu.wuyu.domain.model.ScoringType
import com.fzu.wuyu.domain.model.Subcategory
import com.fzu.wuyu.domain.model.TierOption

/**
 * 福州大学本科生综合素质评价·五育测评门类目录
 * 数据来源：《福州大学本科生综合素质评价参考细则》（用户提供的《德育上限.md》）
 * 五育各项满分 100 分 = 基础性测评 70 分 + 发展性测评 30 分。
 *
 * 注意：等级竞赛类"国家级10/7/5"这类三档分值，按惯例理解为 一等奖/二等奖/三等奖，
 * 如学院细则定义不同，可在录入时用"自定义"选项修正。
 */
object SubcategoryCatalog {

    const val BASIC_CAP = 70f          // 基础性测评上限
    const val DEVELOPMENT_CAP = 30f    // 发展性测评总上限

    /** 未分类记录的门类 id：用户添加时暂不归类的记录，不计入五育，可在总览页随时归类 */
    const val UNKNOWN_SUBCATEGORY_ID = "unknown"

    /** 未分类记录的占位门类（不在 byCategory 结果中，byId 可反查） */
    val unknownSubcategory = Subcategory(
        id = UNKNOWN_SUBCATEGORY_ID,
        category = Category.A,
        name = "未分类",
        scoringType = ScoringType.MANUAL,
        capType = CapType.NONE,
        capValue = null,
        note = "尚未归类的记录，不计入五育得分，归类后开始计分"
    )

    val subcategories: List<Subcategory> = listOf(
        // ============ A 德育·A2 发展性测评（总上限30分） ============
        Subcategory(
            id = "a2_thought_moral",
            category = Category.A,
            name = "思想品德教育类活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "0.4分/次，每学期4分"
        ),
        Subcategory(
            id = "a2_ideological_club",
            category = Category.A,
            name = "思想政治类学生社团活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 2f,
            unitScore = 0.4f,
            note = "0.4分/次，每学期2分"
        ),
        Subcategory(
            id = "a2_social_work",
            category = Category.A,
            name = "社会工作（学生骨干）",
            scoringType = ScoringType.INFO_ONLY,
            capType = CapType.ABSOLUTE,
            capValue = 15f,
            note = "岗位分值×考核系数，0～15分。按学院认定结果自行换算，本 App 不记录。"
        ),
        Subcategory(
            id = "a2_class_building",
            category = Category.A,
            name = "班级、团支部、党支部建设工作",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 6f,
            note = "班级评价小组学期考核，0～6分。每学期录入一次考核得分。"
        ),
        Subcategory(
            id = "a2_school_work",
            category = Category.A,
            name = "学校、学院工作（不含已加分类别）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院自定，每学期4分"
        ),
        Subcategory(
            id = "a2_honor_personal",
            category = Category.A,
            name = "荣誉（个人综合类）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国家级", 15f),
                TierOption("省部级", 10f),
                TierOption("校·市级", 5f),
                TierOption("院级", 3f)
            ),
            note = "国家级15 / 省部级10 / 校·市级5 / 院级3"
        ),
        Subcategory(
            id = "a2_honor_advanced",
            category = Category.A,
            name = "荣誉（先进个人）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国家级", 10f),
                TierOption("省级", 5f),
                TierOption("校级", 3f),
                TierOption("院级", 1f)
            ),
            note = "国家级10 / 省级5 / 校级3 / 院级1"
        ),
        Subcategory(
            id = "a2_honor_collective",
            category = Category.A,
            name = "集体荣誉",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("全国·一等奖", 15f, "全国", "一等奖"),
                TierOption("全国·二等奖", 8f, "全国", "二等奖"),
                TierOption("全国·三等奖", 5f, "全国", "三等奖"),
                TierOption("全省·一等奖", 10f, "全省", "一等奖"),
                TierOption("全省·二等奖", 5f, "全省", "二等奖"),
                TierOption("全省·三等奖", 3f, "全省", "三等奖"),
                TierOption("校·市·一等奖", 5f, "校·市", "一等奖"),
                TierOption("校·市·二等奖", 3f, "校·市", "二等奖"),
                TierOption("校·市·三等奖", 1f, "校·市", "三等奖"),
                TierOption("院级·一等奖", 3f, "院级", "一等奖"),
                TierOption("院级·二等奖", 2f, "院级", "二等奖"),
                TierOption("院级·三等奖", 0.5f, "院级", "三等奖")
            ),
            note = "全国15/8/5；全省10/5/3；校·市5/3/1；院3/2/0.5"
        ),
        Subcategory(
            id = "a2_other",
            category = Category.A,
            name = "其他未涵盖项（学院酌情）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院酌情加分，最高4分"
        ),

        // ============ B 智育·B2 发展性测评（总上限30分） ============
        Subcategory(
            id = "b2_competition_activity",
            category = Category.B,
            name = "学术与科技竞赛类活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "0.4分/次，每学期4分"
        ),
        Subcategory(
            id = "b2_competition_achievement",
            category = Category.B,
            name = "学术与科技竞赛成果",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = buildTiered(
                mapOf(
                    "A+类" to listOf(20f, 10f, 7f),
                    "A类" to listOf(10f, 7f, 5f),
                    "A-类" to listOf(7f, 5f, 3f),
                    "B类" to listOf(5f, 3f, 2f),
                    "C类" to listOf(3f, 2f, 1.5f),
                    "校级" to listOf(2f, 1.5f, 1f),
                    "院级" to listOf(1.5f, 1f, 0.8f)
                )
            ),
            note = "A+类20/10/7；A类10/7/5；A-类7/5/3；B类5/3/2；C类3/2/1.5；校级2/1.5/1；院级1.5/1/0.8"
        ),
        Subcategory(
            id = "b2_invention",
            category = Category.B,
            name = "创新实践—发明创造",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国家发明专利", 10f),
                TierOption("实用新型/外观设计", 2f)
            ),
            note = "国家发明专利10分；实用新型/外观设计2分"
        ),
        Subcategory(
            id = "b2_research",
            category = Category.B,
            name = "创新实践—科研成果",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "经审定适当加分，最高4分"
        ),
        Subcategory(
            id = "b2_paper",
            category = Category.B,
            name = "创新实践—学术论文",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国际顶刊", 20f),
                TierOption("国内顶刊", 15f),
                TierOption("卓越期刊", 10f),
                TierOption("一类核心", 5f),
                TierOption("核心", 4f),
                TierOption("其他CN刊", 2f)
            ),
            note = "国际顶刊20/国内顶刊15/卓越期刊10/一类核心5/核心4/其他CN刊2（每篇）"
        ),
        Subcategory(
            id = "b2_other",
            category = Category.B,
            name = "其他未涵盖项（学院酌情）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院酌情加分，最高4分"
        ),

        // ============ C 体育·C2 发展性测评（总上限30分） ============
        Subcategory(
            id = "c2_sports_activity",
            category = Category.C,
            name = "体育类活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "如「青年爱体育」等。0.4分/次，每学期4分"
        ),
        Subcategory(
            id = "c2_sports_competition",
            category = Category.C,
            name = "体育类竞赛（不含参与奖）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = buildTiered(
                mapOf(
                    "国家级" to listOf(10f, 7f, 5f),
                    "省部级" to listOf(7f, 5f, 3f),
                    "校级" to listOf(5f, 3f, 2f),
                    "院级" to listOf(2f, 1.5f, 1f)
                )
            ),
            note = "国家级10/7/5；省部级7/5/3；校级5/3/2；院级2/1.5/1"
        ),
        Subcategory(
            id = "c2_sports_award",
            category = Category.C,
            name = "体育运动竞赛奖励（专项）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = buildTiered(
                mapOf(
                    "团队·国家级" to listOf(30f, 20f, 10f),
                    "团队·省部级" to listOf(20f, 10f, 6f),
                    "个人·国家级" to listOf(15f, 10f, 5f),
                    "个人·省部级" to listOf(10f, 5f, 3f)
                )
            ),
            note = "团队：国家级30/20/10、省部级20/10/6；个人：国家级15/10/5、省部级10/5/3"
        ),
        Subcategory(
            id = "c2_health_run",
            category = Category.C,
            name = "健康跑/AI运动加次",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.2f,
            note = "每增加1次0.2分，每学期4分"
        ),
        Subcategory(
            id = "c2_other",
            category = Category.C,
            name = "其他未涵盖项（学院酌情）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院酌情加分，最高4分"
        ),

        // ============ D 美育·D2 发展性测评（总上限30分） ============
        Subcategory(
            id = "d2_art_activity",
            category = Category.D,
            name = "语言、文化与艺术类活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "如「青年爱音乐」等。0.4分/次，每学期4分"
        ),
        Subcategory(
            id = "d2_art_competition",
            category = Category.D,
            name = "语言、文化与艺术竞赛（不含参与奖）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = buildTiered(
                mapOf(
                    "国家级" to listOf(10f, 7f, 5f),
                    "省部级" to listOf(7f, 5f, 3f),
                    "校级" to listOf(5f, 3f, 2f),
                    "院级" to listOf(2f, 1.5f, 1f)
                )
            ),
            note = "国家级10/7/5；省部级7/5/3；校级5/3/2；院级2/1.5/1"
        ),
        Subcategory(
            id = "d2_other",
            category = Category.D,
            name = "其他未涵盖项（学院酌情）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院酌情加分，最高4分"
        ),

        // ============ E 劳育·E2 发展性测评（总上限30分） ============
        Subcategory(
            id = "e2_labor_activity",
            category = Category.E,
            name = "劳动教育活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "如「劳模工匠进校园」讲座、「青年爱劳动」成果展示、课外劳动实践等。0.4分/次，每学期4分"
        ),
        Subcategory(
            id = "e2_social_practice",
            category = Category.E,
            name = "寒暑假社会实践",
            scoringType = ScoringType.PER_DAY,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            unitScore = 0.4f,
            note = "0.4分/天，每学期4分（即最多计10天）"
        ),
        Subcategory(
            id = "e2_certificate",
            category = Category.E,
            name = "劳动技能—专业技能资格证书",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(TierOption("取得证书", 2f)),
            note = "2分/证"
        ),
        Subcategory(
            id = "e2_ncre",
            category = Category.E,
            name = "劳动技能—全国计算机等级考试（非计算机专业）",
            scoringType = ScoringType.FIXED,
            capType = CapType.ABSOLUTE,
            capValue = 6f,
            options = listOf(
                TierOption("四级", 4f),
                TierOption("三级", 2f)
            ),
            note = "四级4分、三级2分，上限6分"
        ),
        Subcategory(
            id = "e2_volunteer_star",
            category = Category.E,
            name = "志愿服务（星级认定）",
            scoringType = ScoringType.FIXED,
            capType = CapType.ABSOLUTE,
            capValue = 27f,
            options = listOf(
                TierOption("五星", 10f),
                TierOption("四星", 7f),
                TierOption("三星", 5f),
                TierOption("二星", 3f),
                TierOption("一星", 2f)
            ),
            note = "五星10/四星7/三星5/二星3/一星2，上限27分"
        ),
        Subcategory(
            id = "e2_professional_practice",
            category = Category.E,
            name = "专业技能实践",
            scoringType = ScoringType.MANUAL,
            capType = CapType.NONE,
            capValue = null,
            note = "按细则加分"
        ),
        Subcategory(
            id = "e2_other",
            category = Category.E,
            name = "其他未涵盖项（学院酌情）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 4f,
            note = "学院酌情加分，最高4分"
        ),

        // ============ F 负面评价（F 类减分，按 100% 扣减综测） ============
        Subcategory(
            id = "f_discipline",
            category = Category.F,
            name = "纪律处分",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("受通报批评", 0.8f),
                TierOption("受学校警告处分", 1.2f),
                TierOption("受学校严重警告处分", 1.6f),
                TierOption("受学校记过处分", 2f),
                TierOption("受学校留校察看处分", 2.4f)
            ),
            note = "通报批评0.8/警告1.2/严重警告1.6/记过2/留校察看2.4（分/次）"
        ),
        Subcategory(
            id = "f_class_order",
            category = Category.F,
            name = "课堂秩序",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("无故迟到早退", 0.1f),
                TierOption("旷课", 0.15f),
                TierOption("冒名顶替课堂点名", 0.1f),
                TierOption("课堂喧哗", 0.2f),
                TierOption("穿拖鞋、带餐进课堂、上课玩手机", 0.1f),
                TierOption("无故不参加校、院、年段组织的会议或活动", 0.15f),
                TierOption("迟到、早退（晚自修）", 0.1f),
                TierOption("缺席（晚自修）", 0.2f)
            ),
            note = "按次扣分；旷课按 0.15 分/节计，多次可连录多条或用「自定义分数」累计"
        ),
        Subcategory(
            id = "f_dorm_hygiene",
            category = Category.F,
            name = "宿舍卫生（学院/学校检查）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("学院检查·个人不合格", 0.1f, "学院检查", "个人不合格"),
                TierOption("学院检查·不合格宿舍舍长", 0.15f, "学院检查", "舍长"),
                TierOption("学院检查·不合格宿舍舍员", 0.1f, "学院检查", "舍员"),
                TierOption("学院检查·学生骨干、党员", 0.15f, "学院检查", "学生骨干、党员"),
                TierOption("学校检查·个人不合格", 0.2f, "学校检查", "个人不合格"),
                TierOption("学校检查·不合格宿舍舍长", 0.3f, "学校检查", "舍长"),
                TierOption("学校检查·不合格宿舍舍员", 0.2f, "学校检查", "舍员"),
                TierOption("学校检查·学生骨干、党员", 0.3f, "学校检查", "学生骨干、党员")
            ),
            note = "按次扣分，不合格宿舍连带扣舍长/舍员/学生骨干、党员"
        ),
        Subcategory(
            id = "f_dorm_safety",
            category = Category.F,
            name = "宿舍安全",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("晚熄灯后迟归（22:30 后未返回宿舍）", 0.2f),
                TierOption("夜不归宿（23:00 后仍未回宿舍）", 0.8f),
                TierOption("冒名顶替晚点名", 0.1f),
                TierOption("留宿非本宿舍人员", 1.2f),
                TierOption("未按时熄灯，或熄灯后影响他人休息", 0.1f),
                TierOption("在宿舍或教室吸烟", 0.8f),
                TierOption("在宿舍区饮酒（酗酒另按学生管理规定处分）", 0.4f),
                TierOption("保管或使用违规电器", 1.2f)
            ),
            note = "按次扣分"
        ),
        Subcategory(
            id = "f_daily_other",
            category = Category.F,
            name = "其他日常秩序",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("使用明火（燃放鞭炮焰火、点蜡烛蚊香、焚烧物品纸张）", 0.8f),
                TierOption("以攀爬、翻越等不正当方式进出学生公寓、宿舍区", 0.8f),
                TierOption("将易燃、易爆、有毒物品或管制刀具凶器带进学校", 1.2f),
                TierOption("未经批准擅自组织集体外出", 0.8f),
                TierOption("学习作息时间内和晚熄灯后在宿舍玩电脑游戏屡教不改", 0.1f),
                TierOption("浪费粮食、水电等不听劝导", 0.1f),
                TierOption("其他违反学校学院管理规定", 0.1f)
            ),
            note = "按次扣分"
        ),
        Subcategory(
            id = "f_traffic",
            category = Category.F,
            name = "校园交通",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("电动自行车、自行车乱停乱放、未在指定区域停放", 0.2f),
                TierOption("擅自移动、破坏、遮挡校园交通标识、监控、门禁、道闸等设施", 0.8f),
                TierOption("不服从校园交通管理、阻碍执勤、顶撞工作人员，或交通违规被学校通报批评", 0.8f)
            ),
            note = "按次扣分"
        ),

        // ============ S 素质拓展分（独立体系，不计入综测） ============
        // 数据来源：《大一：五育、素拓全解析PPT》PPT 第 27–31 页（素拓测评细则）
        // 与第 33–37 页（素拓减分类）。图片表格中的分值已逐项抄录。
        //
        // —— 加分类（一）文化、艺术、科技、实践类活动 ——
        Subcategory(
            id = "s_culture_join",
            category = Category.S,
            name = "文化、艺术、科技、实践类活动（参与奖）",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国家级参与奖", 3f),
                TierOption("省级参与奖", 2f),
                TierOption("校级参与奖", 1f)
            ),
            note = "获参与奖：国家级3分、省级2分、校级1分"
        ),
        Subcategory(
            id = "s_culture_award",
            category = Category.S,
            name = "文化、艺术、科技、实践类活动（获奖）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = buildTiered(
                mapOf(
                    "全国" to listOf(20f, 16f, 13f),
                    "省级" to listOf(12f, 10f, 7f),
                    "校级及以下" to listOf(6f, 4f, 3f)
                )
            ),
            note = "全国20/16/13；省级12/10/7；校级及以下6/4/3（一等/二等/三等）"
        ),
        // —— 加分类（二）体育竞赛类活动 ——
        Subcategory(
            id = "s_sports_join",
            category = Category.S,
            name = "体育竞赛类活动（参与基础分）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("国家级·团体项目", 6f, "国家级", "团体"),
                TierOption("国家级·个人项目", 5f, "国家级", "个人"),
                TierOption("省级·团体项目", 5f, "省级", "团体"),
                TierOption("省级·个人项目", 4f, "省级", "个人"),
                TierOption("市级·团体项目", 4f, "市级", "团体"),
                TierOption("市级·个人项目", 3f, "市级", "个人"),
                TierOption("校级及以下·团体项目", 3f, "校级及以下", "团体"),
                TierOption("校级及以下·个人项目", 3f, "校级及以下", "个人")
            ),
            note = "团体项目：国家级6/省级5/市级4/校级及以下3；" +
                "个人项目：国家级5/省级4/市级3/校级及以下3"
        ),
        Subcategory(
            id = "s_sports_award",
            category = Category.S,
            name = "体育竞赛类活动（获奖加分）",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("团体项目·第一名", 3f, "团体项目", "第一名"),
                TierOption("团体项目·第二、三名", 1.5f, "团体项目", "第二、三名"),
                TierOption("团体项目·第四至八名", 0.5f, "团体项目", "第四至八名"),
                TierOption("个人项目·第一名", 4f, "个人项目", "第一名"),
                TierOption("个人项目·第二、三名", 2f, "个人项目", "第二、三名"),
                TierOption("个人项目·第四至八名", 1f, "个人项目", "第四至八名")
            ),
            note = "在参与基础分之外额外加分。团体：第一名3/第二三名1.5/第四至八名0.5；" +
                "个人：第一名4/第二三名2/第四至八名1"
        ),
        Subcategory(
            id = "s_sports_record",
            category = Category.S,
            name = "体育竞赛类活动（破纪录）",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("省级破纪录", 30f),
                TierOption("校级破纪录", 10f),
                TierOption("院级破纪录", 5f)
            ),
            note = "破纪录额外加分：省级30、校级10、院级5"
        ),
        // —— 加分类（三）其他学生活动 ——
        Subcategory(
            id = "s_audience",
            category = Category.S,
            name = "招募观众类学生活动",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.ABSOLUTE,
            capValue = 30f,
            unitScore = 1f,
            note = "各类讲座、座谈会、报告会等，每人1分/次，累计上限30分"
        ),
        Subcategory(
            id = "s_venue",
            category = Category.S,
            name = "场务类学生活动（按职务）",
            scoringType = ScoringType.MANUAL,
            capType = CapType.NONE,
            capValue = null,
            note = "院级各学生组织负责人0-10；学生党支部书记0-9；院级学生组织各部门部长、" +
                "年级学生分会主席团成员、年级分团委学生副书记、班长、团支部书记0-8；" +
                "院级学生组织各部门副部长、年级学生会部长、班导、学生社团主要负责人、" +
                "学生党支部委员、楼长0-7；年级学生会副部长、班级班委、学生社团负责人、" +
                "层长、各类文体「院队」负责人0-6；院级学生组织各部门干事0-5；宿舍长0-3"
        ),
        Subcategory(
            id = "s_showcase",
            category = Category.S,
            name = "现场展示类学生活动",
            scoringType = ScoringType.MANUAL,
            capType = CapType.NONE,
            capValue = null,
            note = "细则未列固定分值，由指导老师/学院团委认定后录入"
        ),
        Subcategory(
            id = "s_volunteer_year",
            category = Category.S,
            name = "志愿服务（年度满25小时）",
            scoringType = ScoringType.FIXED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(TierOption("年度志愿时长满25小时", 6f)),
            note = "每年度志愿服务时长达25小时，可获6分（以志愿汇记录为准）"
        ),
        Subcategory(
            id = "s_broadcast",
            category = Category.S,
            name = "广播站工作",
            scoringType = ScoringType.PER_TIME,
            capType = CapType.PER_SEMESTER,
            capValue = 2f,
            unitScore = 0.2f,
            note = "播音、撰稿、办公工作0.2分/次，每学期上限2分（由广播站负责人学期末申报）"
        ),
        Subcategory(
            id = "s_special",
            category = Category.S,
            name = "特别贡献",
            scoringType = ScoringType.MANUAL,
            capType = CapType.PER_SEMESTER,
            capValue = 10f,
            note = "细则未体现的学生工作或重大活动中对校区有特别贡献的项目，" +
                "由指导老师或学院团委书记签字同意后申请，每学期原则上不超过10分"
        ),
        // —— 素拓减分类（分值独立于五育 F 类，另有一套口径） ——
        Subcategory(
            id = "s_deduct_discipline",
            category = Category.S,
            name = "违纪处理处分",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("通报批评", 5f),
                TierOption("警告", 10f),
                TierOption("严重警告", 15f),
                TierOption("记过处分", 30f),
                TierOption("留校察看", 40f)
            ),
            note = "通报批评5/警告10/严重警告15/记过30/留校察看40（分/次）",
            deduct = true
        ),
        Subcategory(
            id = "s_deduct_class",
            category = Category.S,
            name = "课堂秩序",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("课堂·无故迟到、早退", 5f, "课堂", "迟到早退"),
                TierOption("课堂·旷课", 10f, "课堂", "旷课"),
                TierOption("课堂·无故不参加校、院、年段统一组织的会议或活动", 10f, "课堂", "缺席会议"),
                TierOption("早晚自习·迟到、早退", 5f, "早晚自习", "迟到早退"),
                TierOption("早晚自习·无故缺席", 10f, "早晚自习", "缺席"),
                TierOption("早点（相关活动）·无故迟到、缺席", 5f, "早点", "迟到缺席")
            ),
            note = "课堂：迟到早退5/次、旷课10/节、无故不参加统一组织的会议或活动按旷课处理10/节；" +
                "早晚自习：迟到早退5/次、无故缺席10/次；早点：无故迟到、缺席5/次",
            deduct = true
        ),
        Subcategory(
            id = "s_deduct_daily",
            category = Category.S,
            name = "日常秩序",
            scoringType = ScoringType.TIERED,
            capType = CapType.NONE,
            capValue = null,
            options = listOf(
                TierOption("宿舍整体检查不合格（每人每次）", 2f, "宿舍卫生", "整体不合格"),
                TierOption("单个床位卫生不合格（本人每次）", 2f, "宿舍卫生", "床位不合格"),
                TierOption("对检评员态度恶劣或拒检", 2f, "宿舍卫生", "态度恶劣"),
                TierOption("晚熄灯后晚归未销假", 5f, "宿舍安全及其他", "晚归"),
                TierOption("夜不归宿", 15f, "宿舍安全及其他", "夜不归宿"),
                TierOption("留宿非本宿舍人员", 20f, "宿舍安全及其他", "留宿他人"),
                TierOption("没按时熄灯", 1f, "宿舍安全及其他", "未按时熄灯"),
                TierOption("保管或使用违规电器、使用明火、携带易燃易爆及管制刀具进校", 10f, "宿舍安全及其他", "违规电器/明火/危险品")
            ),
            note = "宿舍卫生：整体不合格或单个床位不合格均2分/次（对检评员态度恶劣或拒检按不合格处理）；" +
                "宿舍安全及其他：晚归未销假5/次、夜不归宿15/次、留宿他人20/次、未按时熄灯1/次、" +
                "违规电器/明火/危险品10/次",
            deduct = true
        )
    )

    /** 基础性测评计算方式说明（不记录，仅在页面上提示） */
    val basicEvaluations: List<BasicEvaluationInfo> = listOf(
        BasicEvaluationInfo(
            Category.A,
            "A1 =（班级同学互评平均分 × 80% + 辅导员评价得分 × 20%）× 70%。" +
                "等次换算：优秀100、良好90、一般75、差50。"
        ),
        BasicEvaluationInfo(
            Category.B,
            "满足任一条件得70分，未完成以50分计：①学术讲座、沙龙、论坛、学科调研、创新创业实践等 ≥ 2次，" +
                "或主讲/主持 ≥ 1次；②读书分享会、学习互助分享等 ≥ 2次，或主讲/主持 ≥ 1次；" +
                "③科研训练计划、学科竞赛、辩论赛等 ≥ 1次。"
        ),
        BasicEvaluationInfo(
            Category.C,
            "有体育必修课：正考成绩合格得70分，不合格以50分计。无体育必修课（完成任一得70分，未完成50分）：" +
                "①每学期完成健康跑36次，每周不少于1次（男生每次≥2公里、配速≥7.5min/km；女生每次≥1.6公里、配速≥8.5min/km）；" +
                "②福大体育APP\"AI运动\"任选2个项目，各打卡36次。"
        ),
        BasicEvaluationInfo(
            Category.D,
            "完成任一条件得70分，未完成以50分计：①参与艺术展览、文艺演出、艺术沙龙、讲坛等艺术欣赏与体验 ≥ 2次；" +
                "②参与文化艺术活动或作品的创作、演出、展示、报告等 ≥ 1次。"
        ),
        BasicEvaluationInfo(
            Category.E,
            "完成任一条件得70分，未完成以50分计：①宿舍卫生评价合格；②志愿服务时长（以志愿汇记录为准）≥ 10小时。"
        )
    )

    fun byId(id: String): Subcategory? =
        subcategories.firstOrNull { it.id == id }
            ?: if (id == UNKNOWN_SUBCATEGORY_ID) unknownSubcategory else null

    fun byCategory(category: Category): List<Subcategory> =
        subcategories.filter { it.category == category && it.id != UNKNOWN_SUBCATEGORY_ID }

    fun basicInfoOf(category: Category): BasicEvaluationInfo? =
        basicEvaluations.firstOrNull { it.category == category }

    /** 把 "级别 -> [一等, 二等, 三等]" 的映射展开为选项列表 */
    private fun buildTiered(levelScores: Map<String, List<Float>>): List<TierOption> {
        val ranks = listOf("一等奖", "二等奖", "三等奖")
        return levelScores.flatMap { (level, scores) ->
            scores.mapIndexed { index, score ->
                TierOption(
                    label = "$level·${ranks.getOrElse(index) { "第${index + 1}档" }}",
                    score = score,
                    group = level,
                    rank = ranks.getOrElse(index) { "第${index + 1}档" }
                )
            }
        }
    }
}
