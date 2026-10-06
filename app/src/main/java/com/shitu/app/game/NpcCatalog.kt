package com.shitu.app.game

import com.shitu.app.model.Faction
import com.shitu.app.model.NpcProfile
import com.shitu.app.model.NpcTier

/**
 * 主要人物的静态设定。关系数值（关系/信任/风险）不在这里，
 * 而是随游戏进程存在 RelationshipState 中。
 */
object NpcCatalog {

    val npcs: List<NpcProfile> = listOf(
        NpcProfile(
            id = "qian_jinming",
            name = "钱进明",
            title = "市直机关下派干部",
            age = 38,
            faction = Faction.PRAGMATIC,
            tier = NpcTier.COLLEAGUE,
            influence = 52,
            personality = listOf("干练", "目标明确"),
            description = "从市里下来挂职，材料写得漂亮，做事讲效率。"
        ),
        NpcProfile(
            id = "he_wei",
            name = "何伟",
            title = "选调生",
            age = 29,
            faction = Faction.REFORM,
            tier = NpcTier.COLLEAGUE,
            influence = 38,
            personality = listOf("锐气", "敢闯"),
            description = "选调生出身，学历好、想法多，被不少人看作苗子。"
        ),
        NpcProfile(
            id = "chen_jianguo",
            name = "陈建国",
            title = "分管副县长",
            age = 48,
            faction = Faction.LOCAL,
            tier = NpcTier.COUNTY_LEADER,
            influence = 78,
            personality = listOf("稳重", "护短"),
            description = "在本地干了二十多年，说话不紧不慢，但很少有人反驳他。"
        ),
        NpcProfile(
            id = "zhou_huilan",
            name = "周慧兰",
            title = "市委组织部科长",
            age = 41,
            faction = Faction.CORE,
            tier = NpcTier.ORGANIZATION,
            influence = 66,
            personality = listOf("严谨", "记性好"),
            description = "管着干部档案和考察材料，见过的人比她记得的还多。"
        ),
        NpcProfile(
            id = "ma_wenbin",
            name = "马文斌",
            title = "同科室副科长",
            age = 35,
            faction = Faction.PRAGMATIC,
            tier = NpcTier.COLLEAGUE,
            influence = 42,
            personality = listOf("精明", "好胜"),
            description = "和你同期进单位，业务熟，也一直盯着同一个位子。"
        ),
        NpcProfile(
            id = "gao_zhiqiang",
            name = "高志强",
            title = "县发改局副局长",
            age = 45,
            faction = Faction.REFORM,
            tier = NpcTier.COLLEAGUE,
            influence = 58,
            personality = listOf("大胆", "敢说"),
            description = "开会时敢提别人不敢提的方案，也因此在上面有些名声。"
        ),
        NpcProfile(
            id = "sun_dehai",
            name = "孙德海",
            title = "退休老领导",
            age = 63,
            faction = Faction.CONSERVATIVE,
            tier = NpcTier.MENTOR,
            influence = 55,
            personality = listOf("念旧", "固执"),
            description = "退下来五年了，逢年过节还有人上门，人情还在。"
        ),
        NpcProfile(
            id = "zhao_chunsheng",
            name = "赵春生",
            title = "乡镇党委书记",
            age = 44,
            faction = Faction.LOCAL,
            tier = NpcTier.COUNTY_LEADER,
            influence = 60,
            personality = listOf("圆滑", "务实"),
            description = "基层一把手，对下有一套，对上也有分寸。"
        ),
        NpcProfile(
            id = "lin_xiaoyun",
            name = "林晓芸",
            title = "办公室科员",
            age = 27,
            faction = Faction.NEUTRAL,
            tier = NpcTier.SUBORDINATE,
            influence = 25,
            personality = listOf("踏实", "内向"),
            description = "材料写得细，话不多，是办公室里最肯加班的人。"
        ),
        NpcProfile(
            id = "zheng_haitao",
            name = "郑海涛",
            title = "本地企业负责人",
            age = 50,
            faction = Faction.NEUTRAL,
            tier = NpcTier.BUSINESS,
            influence = 50,
            personality = listOf("豪爽", "会算账"),
            description = "和很多部门都熟，出手大方，从来不做亏本的买卖。"
        ),
        // ===== 第十二阶段：新增 52 名虚构人物 =====
        // —— 上级与组织系统 ——
        NpcProfile("n12_wei_cheng", "韦承", "市委副书记", 52, Faction.CORE, NpcTier.CITY_LEADER, 84,
            listOf("沉稳", "重结果"), "分管干部的市委领导，话不多，但一句顶一句。"),
        NpcProfile("n12_tang_li", "汤莉", "市委组织部副部长", 46, Faction.CORE, NpcTier.ORGANIZATION, 74,
            listOf("细致", "原则性强"), "考察干部时最看重实绩和口碑，不轻易表态。"),
        NpcProfile("n12_guo_zhen", "郭震", "市纪委监委室主任", 44, Faction.CORE, NpcTier.ORGANIZATION, 70,
            listOf("严肃", "不留情面"), "查办案件的负责人，谁见了都要客气三分。"),
        NpcProfile("n12_shen_yi", "沈毅", "省厅下派干部", 40, Faction.REFORM, NpcTier.CITY_LEADER, 66,
            listOf("视野宽", "要求高"), "从省厅下来挂职，张口就是标准和前沿做法。"),
        NpcProfile("n12_bai_ming", "白鸣", "市政府副秘书长", 47, Faction.PRAGMATIC, NpcTier.CITY_LEADER, 68,
            listOf("会协调", "消息灵"), "协调各方的一把好手，很多事到他这里就顺了。"),
        NpcProfile("n12_luo_shan", "罗山", "分管副市长", 51, Faction.LOCAL, NpcTier.CITY_LEADER, 80,
            listOf("护下", "讲情分"), "在本地工作多年，对肯干的下属很护着。"),
        // —— 同僚 ——
        NpcProfile("n12_han_xu", "韩旭", "兄弟乡镇书记", 39, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 50,
            listOf("爽快", "好胜"), "和你一样在基层熬，既互相帮忙也暗暗较劲。"),
        NpcProfile("n12_qin_yue", "秦悦", "县府办副主任", 37, Faction.NEUTRAL, NpcTier.COLLEAGUE, 46,
            listOf("周全", "嘴严"), "经手大量文件，知道的事多，说出来的少。"),
        NpcProfile("n12_fang_lei", "方磊", "发改局科长", 34, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 44,
            listOf("专业", "爱较真"), "项目审批的把关人，方案不扎实就过不了他这关。"),
        NpcProfile("n12_xia_qing", "夏清", "财政局预算科长", 42, Faction.CONSERVATIVE, NpcTier.COLLEAGUE, 52,
            listOf("谨慎", "认死理"), "管预算的人，谁想多要一分钱都得先过她这一关。"),
        NpcProfile("n12_du_yong", "杜勇", "住建局副局长", 45, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 56,
            listOf("能干事", "胆子大"), "工程上的行家，敢拍板，也容易踩线。"),
        NpcProfile("n12_yin_ru", "尹茹", "卫健局科长", 38, Faction.NEUTRAL, NpcTier.COLLEAGUE, 42,
            listOf("温和", "耐心"), "业务熟，遇到突发事件总能顶上去。"),
        NpcProfile("n12_lu_heng", "陆恒", "应急局副局长", 43, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 54,
            listOf("警觉", "雷厉风行"), "常年绷着一根弦，最怕的就是侥幸心理。"),
        NpcProfile("n12_song_yan", "宋岩", "农业农村局副局长", 48, Faction.LOCAL, NpcTier.COLLEAGUE, 50,
            listOf("接地气", "倔"), "在田里比在办公室多，说话直来直去。"),
        NpcProfile("n12_qu_bo", "曲波", "宣传科副科长", 33, Faction.NEUTRAL, NpcTier.COLLEAGUE, 40,
            listOf("机灵", "爱表现"), "写材料快，处理舆情有自己的一套。"),
        NpcProfile("n12_ning_fan", "宁帆", "兄弟单位业务骨干", 36, Faction.REFORM, NpcTier.COLLEAGUE, 45,
            listOf("思路活", "不服管"), "总想试新办法，成绩和争议一样多。"),
        // —— 下属 ——
        NpcProfile("n12_peng_yu", "彭宇", "办公室科员", 26, Faction.NEUTRAL, NpcTier.SUBORDINATE, 20,
            listOf("勤快", "经验少"), "刚参加工作的小伙子，让干啥就干啥，只是还不够稳。"),
        NpcProfile("n12_jiang_lan", "蒋岚", "综合科科员", 28, Faction.NEUTRAL, NpcTier.SUBORDINATE, 22,
            listOf("细心", "内向"), "报表做得极细，很少出错，也不太会来事。"),
        NpcProfile("n12_he_chuan", "何川", "驻村干部", 30, Faction.LOCAL, NpcTier.SUBORDINATE, 28,
            listOf("能吃苦", "包袱重"), "在村里一待就是几年，群众认识他比认识你还多。"),
        NpcProfile("n12_zhou_min", "周敏", "便民服务中心负责人", 32, Faction.NEUTRAL, NpcTier.SUBORDINATE, 30,
            listOf("热情", "嗓门大"), "窗口的台柱子，群众有事都愿意找她。"),
        NpcProfile("n12_yan_dong", "严冬", "执法队队员", 31, Faction.CONSERVATIVE, NpcTier.SUBORDINATE, 26,
            listOf("原则性强", "不会拐弯"), "执法不留情面，也因此常被投诉。"),
        NpcProfile("n12_xu_ning", "徐宁", "年轻选调生", 25, Faction.REFORM, NpcTier.SUBORDINATE, 24,
            listOf("有想法", "沉不住气"), "思路新，但还需要多摔打几年。"),
        // —— 企业负责人 ——
        NpcProfile("n12_qi_shan", "齐山", "龙头企业董事长", 55, Faction.PRAGMATIC, NpcTier.BUSINESS, 64,
            listOf("老练", "讲条件"), "本地纳税大户，说话有底气。"),
        NpcProfile("n12_wen_jia", "温嘉", "新兴产业负责人", 41, Faction.REFORM, NpcTier.BUSINESS, 52,
            listOf("敏锐", "爱谈规划"), "做数字产业的年轻人，项目讲得很漂亮。"),
        NpcProfile("n12_mo_lin", "莫林", "食品加工厂厂长", 48, Faction.LOCAL, NpcTier.BUSINESS, 45,
            listOf("实在", "会哭穷"), "厂子不大，但养着几百号人，遇事总先诉苦。"),
        NpcProfile("n12_shang_qi", "尚奇", "房地产开发负责人", 50, Faction.PRAGMATIC, NpcTier.BUSINESS, 58,
            listOf("精明", "路子广"), "项目多，资金链也绷得紧。"),
        NpcProfile("n12_fu_an", "傅安", "物流企业负责人", 44, Faction.NEUTRAL, NpcTier.BUSINESS, 42,
            listOf("务实", "重效率"), "跑运输起家，最关心路和通关效率。"),
        NpcProfile("n12_zou_ke", "邹可", "连锁商超区域经理", 38, Faction.NEUTRAL, NpcTier.BUSINESS, 40,
            listOf("会算账", "客气"), "谈合作先谈利，但账目干净。"),
        // —— 专家与专业人士 ——
        NpcProfile("n12_yu_gong", "于工", "规划设计院专家", 58, Faction.NEUTRAL, NpcTier.MENTOR, 48,
            listOf("严谨", "不留情面"), "评审会上敢说方案不行，谁的面子都不给。"),
        NpcProfile("n12_pei_jiao", "裴教授", "高校产业经济学者", 53, Faction.REFORM, NpcTier.MENTOR, 50,
            listOf("理性", "偏理想"), "给地方做产业顾问，观点常与本地打法不同。"),
        NpcProfile("n12_yi_wei", "易维", "环保技术专家", 47, Faction.REFORM, NpcTier.MENTOR, 44,
            listOf("较真", "重数据"), "只认监测数据，不接受含糊说法。"),
        NpcProfile("n12_ke_zheng", "柯正", "医疗管理专家", 52, Faction.NEUTRAL, NpcTier.MENTOR, 46,
            listOf("务实", "有耐心"), "医共体改革的技术顾问，方案做得扎实。"),
        NpcProfile("n12_sui_lao", "隋老师", "退休农技推广员", 66, Faction.LOCAL, NpcTier.MENTOR, 42,
            listOf("热心", "爱唠叨"), "在村里跑了半辈子，农民都信他。"),
        // —— 群众代表 ——
        NpcProfile("n12_liu_shu", "刘叔", "老村民代表", 62, Faction.LOCAL, NpcTier.COLLEAGUE, 38,
            listOf("直率", "认理"), "说事直来直去，代表着一批老村民的想法。"),
        NpcProfile("n12_ma_ayi", "马阿姨", "社区居民代表", 59, Faction.NEUTRAL, NpcTier.COLLEAGUE, 34,
            listOf("热心肠", "爱较真"), "小区里的事她都要管一管，也是最好的民意窗口。"),
        NpcProfile("n12_wang_ge", "王哥", "外卖骑手代表", 35, Faction.NEUTRAL, NpcTier.COLLEAGUE, 30,
            listOf("爽快", "怕麻烦"), "跑单的人，最关心的其实是路好不好走、停车难不难。"),
        NpcProfile("n12_chen_jie", "陈姐", "农贸市场商户代表", 46, Faction.LOCAL, NpcTier.COLLEAGUE, 32,
            listOf("泼辣", "重实际"), "市场翻新之后她第一个搬回来，也第一个提意见。"),
        NpcProfile("n12_he_sheng", "何师傅", "外来务工人员代表", 43, Faction.NEUTRAL, NpcTier.COLLEAGUE, 28,
            listOf("沉默", "认干"), "欠薪的时候他不闹，但一直在等一个说法。"),
        // —— 地方干部 ——
        NpcProfile("n12_gu_shu", "谷书记", "老乡镇党委书记", 49, Faction.LOCAL, NpcTier.COUNTY_LEADER, 60,
            listOf("圆熟", "有威望"), "在一个镇干了十几年，底下的人都听他的。"),
        NpcProfile("n12_tian_zhen", "田镇长", "新任镇长", 36, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 48,
            listOf("有冲劲", "根基浅"), "刚提上来的年轻镇长，急着做出成绩。"),
        NpcProfile("n12_zhang_wei", "章伟", "街道办主任", 42, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 52,
            listOf("细致", "会做工作"), "城市治理的门道摸得很清楚。"),
        NpcProfile("n12_dai_chun", "戴春", "村党支部书记", 47, Faction.LOCAL, NpcTier.COLLEAGUE, 40,
            listOf("能干", "性子急"), "村里的当家人，办事利索，但有时不讲程序。"),
        NpcProfile("n12_mao_jian", "毛健", "社区党委书记", 39, Faction.NEUTRAL, NpcTier.COLLEAGUE, 42,
            listOf("耐心", "接地气"), "老旧小区的事，他比谁都清楚。"),
        NpcProfile("n12_o_yang", "欧阳明", "开发区管委会副主任", 45, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 58,
            listOf("能谈", "指标导向"), "天天和企业打交道，招商是他的强项。"),
        // —— 同层骨干与项目负责人 ——
        NpcProfile("n12_xiong_wei", "熊威", "同层骨干", 37, Faction.PRAGMATIC, NpcTier.COLLEAGUE, 60,
            listOf("强势", "目标明确"), "和你同层级，抓项目是把好手，做事有一股不服输的劲。"),
        NpcProfile("n12_lai_qi", "赖琪", "人脉骨干", 40, Faction.LOCAL, NpcTier.COLLEAGUE, 55,
            listOf("圆融", "人脉广"), "走到哪里都有人打招呼，办事先从关系入手。"),
        NpcProfile("n12_jiao_yang", "焦阳", "专业骨干", 43, Faction.REFORM, NpcTier.COLLEAGUE, 57,
            listOf("专业", "清高"), "技术出身，材料写得漂亮，不太看得上拉关系。"),
        NpcProfile("n12_ji_hong", "纪红", "基层骨干", 41, Faction.LOCAL, NpcTier.COLLEAGUE, 53,
            listOf("扎实", "能忍"), "在基层熬了很多年，群众基础极好。"),
        NpcProfile("n12_quan_yi", "全毅", "改革骨干", 38, Faction.REFORM, NpcTier.COLLEAGUE, 56,
            listOf("敢闯", "锋芒盛"), "推起改革来不留余地，成绩和争议都不少。"),
        NpcProfile("n12_ying_ran", "应然", "稳健骨干", 45, Faction.CONSERVATIVE, NpcTier.COLLEAGUE, 54,
            listOf("四平八稳", "不出错"), "从不冒险，考核从没出过问题。"),
        NpcProfile("n12_pan_shuo", "潘硕", "青年才俊", 33, Faction.CORE, NpcTier.COLLEAGUE, 62,
            listOf("聪明", "背景好"), "年纪不大，位置却很好，大家都说他前途无量。"),
        NpcProfile("n12_ruan_qi", "阮琦", "项目负责人", 44, Faction.PRAGMATIC, NpcTier.BUSINESS, 46,
            listOf("务实", "急脾气"), "园区的现场负责人，进度慢一点都不行。"),
        NpcProfile("n12_li_zhi", "李志", "道路工程项目负责人", 49, Faction.LOCAL, NpcTier.BUSINESS, 44,
            listOf("经验足", "会说话"), "修了半辈子路，手上的施工队很能干。"),
        NpcProfile("n12_kong_yi", "孔义", "医院扩建项目负责人", 46, Faction.NEUTRAL, NpcTier.BUSINESS, 45,
            listOf("细致", "讲流程"), "医疗项目的流程最复杂，他打理得井井有条。"),
        NpcProfile("n12_mu_qing", "穆清", "数字政务项目负责人", 35, Faction.REFORM, NpcTier.BUSINESS, 48,
            listOf("技术强", "爱较真"), "盯着每一个功能细节，不接受「差不多」。"),
        NpcProfile("n12_shi_yan", "施岩", "环保项目负责人", 42, Faction.REFORM, NpcTier.BUSINESS, 47,
            listOf("较真", "环保底线硬"), "宁可工期延后，也不肯在环保上让步。")
    )

    private val byId: Map<String, NpcProfile> = npcs.associateBy { it.id }

    fun find(id: String): NpcProfile? = byId[id]

    fun byFaction(faction: Faction): List<NpcProfile> = npcs.filter { it.faction == faction }
}

