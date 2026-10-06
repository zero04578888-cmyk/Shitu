package com.shitu.app.game

import com.shitu.app.model.CareerEndingResult
import com.shitu.app.model.CareerRecordKind
import com.shitu.app.model.CareerRoute
import com.shitu.app.model.LifeEvaluation
import com.shitu.app.model.PoliticalTendency

/**
 * 第十阶段：统一结局判定。
 *
 * 把所有系统（履历、职位、政绩、关系、派系、路线、风险、民望、家庭、压力、
 * 成长、重大历史选择）一起读进来，先判断「这个人是谁」，再给结局。
 * 具体公式不对外展示，玩家只看到结论与几条关键原因。
 * 所有阈值都是游戏模拟参数。
 */
object CareerEndingEvaluator {

    private data class Decision(
        val id: String,
        val title: String,
        val category: String,
        val tier: String,
        val description: String
    )

    fun evaluate(state: GameState): CareerEndingResult {
        val evaluation = CareerEngine.evaluate(state)
        val decision = decide(state, evaluation)
        return CareerEndingResult(
            id = decision.id,
            title = decision.title,
            category = decision.category,
            tier = decision.tier,
            description = decision.description,
            reasons = reasonsOf(state, evaluation, decision),
            keyNodes = keyNodesOf(state),
            keyChoices = keyChoicesOf(state)
        )
    }

    private fun decide(state: GameState, evaluation: LifeEvaluation): Decision {
        val attrs = state.player.attributes
        val index = state.player.positionIndex
        val years = state.player.serviceYears
        val lowGrades = state.reviews.count { it.score < 55 }
        val career = state.career
        val politics = state.politics
        val merit = state.governance.merit.overall
        val approval = state.governance.jurisdiction.approval
        val writingDominant = attrs.writing >= attrs.coordination && attrs.writing >= attrs.ability
        val coordinationDominant = attrs.coordination > attrs.writing && attrs.coordination >= attrs.ability
        val abilityDominant = attrs.ability > attrs.writing && attrs.ability > attrs.coordination

        return when {
            // ===== 职业发展：先看走到哪一步 =====
            state.reachedTopYear != null && state.reachedTopYear <= 16 -> Decision(
                "fast_rise", "快速晋升", "晋升", "优",
                "从基层到正处级，你只用了很短的时间。每一步都踩在关键任务上。"
            )

            // ===== 隐藏结局：多周目且几乎无可挑剔的仕途 =====
            career.priorLives >= 1 && evaluation.overall >= 88 && index >= 6 &&
                career.unlockedAchievements.size >= 10 && attrs.integrityRisk <= 20 -> Decision(
                "hidden_legacy", "传奇人生", "隐藏", "隐藏",
                "这不是第一段仕途，却走成了最难复制的一段。履历、口碑与成绩叠在一起，" +
                    "很多年后仍有人拿你当参照。"
            )

            // ===== 发展路线定型的职业形态 =====
            index >= 4 && state.route == CareerRoute.WRITING &&
                attrs.writing >= 70 && attrs.leaderTrust >= 62 -> Decision(
                "behind_scenes", "幕后智囊", "类型", "良",
                "很多重要材料出自你的手，很多决策背后有你的判断。你习惯站在灯光之外。"
            )

            index >= 4 && state.route == CareerRoute.PROFESSIONAL && attrs.ability >= 70 -> Decision(
                "specialist", "专业型干部", "类型", "良",
                "你把一件事做到了很少有人能替代的程度。职务到了某一级，专业却一直在往前走。"
            )

            index >= 5 && state.route == CareerRoute.MANAGEMENT &&
                attrs.coordination >= 68 && attrs.leaderTrust >= 62 -> Decision(
                "manager", "管理型干部", "类型", "良",
                "你最擅长的是把人和事组织起来。复杂的事情到了你手上，总能理出头绪。"
            )

            index >= 3 && state.route == CareerRoute.MASS_WORK &&
                attrs.masses >= 78 && attrs.reputation >= 62 && attrs.integrityRisk <= 35 -> Decision(
                "people_first", "一心为民", "口碑", "优",
                "三十年间，你把最多的时间留给了群众的事。职务未必最高，但很多普通人记得你的名字。"
            )

            // ===== 第十阶段：按长期政治路线定型 =====
            index >= 4 && politics.tendencyOf(PoliticalTendency.REFORMIST) >= 8 &&
                merit >= 60 -> Decision(
                "reform_pioneer", "改革型干部", "路线", "优",
                "你这一路都在试新办法、担新风险。有些事成了，有些事留下了争议，" +
                    "但回头看不难发现：你推着这地方往前走了一段。"
            )

            index >= 4 && politics.tendencyOf(PoliticalTendency.PRAGMATIC) >= 8 &&
                merit >= 62 -> Decision(
                "pragmatic_achiever", "实干型干部", "路线", "优",
                "你不喜欢表态，也不太争辩，只是把一件件事办成。数字和口碑替你说了话。"
            )

            index >= 3 && politics.tendencyOf(PoliticalTendency.STEADY) >= 8 &&
                attrs.integrityRisk <= 25 && state.warningCount == 0 -> Decision(
                "steady_statesman", "稳健型干部", "路线", "良",
                "这些年你几乎没有出过事。有人说你保守，但每一任领导都放心把担子交给你。"
            )

            index in 2..4 && years >= 15 && approval >= 70 && merit >= 55 -> Decision(
                "grassroots_governor", "基层治理型", "职业", "良",
                "你在基层待了很久，把一个地方从里到外摸透了。职务不算最高，" +
                    "但治理的底子是你一笔一笔打下来的。"
            )

            // ===== 高峰型：走到很高且评价突出 =====
            index >= 7 && evaluation.overall >= 82 && attrs.integrityRisk <= 30 -> Decision(
                "pinnacle", "高峰型仕途", "晋升", "优",
                "你走到了这个系统里很高的位置，而且不是靠熬年头。履历翻开，每一页都站得住。"
            )

            // ===== 无明确路线时按专长倾向判定 =====
            index >= 4 && writingDominant && attrs.writing >= 82 && attrs.leaderTrust >= 75 -> Decision(
                "behind_scenes", "幕后智囊", "类型", "良",
                "很多重要材料出自你的手，很多决策背后有你的判断。你习惯站在灯光之外。"
            )

            index >= 4 && abilityDominant && attrs.ability >= 82 -> Decision(
                "specialist", "专业型干部", "类型", "良",
                "你把一件事做到了很少有人能替代的程度。职务到了某一级，专业却一直在往前走。"
            )

            index >= 5 && coordinationDominant && attrs.coordination >= 80 && attrs.leaderTrust >= 75 -> Decision(
                "manager", "管理型干部", "类型", "良",
                "你最擅长的是把人和事组织起来。复杂的事情到了你手上，总能理出头绪。"
            )

            index >= 4 && (attrs.writing >= 82 || attrs.ability >= 82) -> Decision(
                "specialist", "专业型干部", "类型", "良",
                "你把一件事做到了很少有人能替代的程度。职务到了某一级，专业却一直在往前走。"
            )

            index >= 5 -> Decision(
                "steady_rise", "稳步晋升", "晋升", "良",
                "没有惊人的跳跃，也没有明显的失误。你按部就班，最终走到了大多数人走不到的位置。"
            )

            // ===== 口碑类 =====
            attrs.masses >= 85 && attrs.reputation >= 80 && attrs.integrityRisk <= 20 && attrs.family >= 60 ->
                Decision(
                    "people_first", "一心为民", "口碑", "优",
                    "三十年间，你把最多的时间留给了群众的事。职务未必最高，但很多普通人记得你的名字。"
                )

            attrs.masses >= 88 -> Decision(
                "mass_recognition", "群众认可", "口碑", "良",
                "你走到哪里，都有群众愿意和你说话。这份信任比任何评价都难得。"
            )

            attrs.reputation >= 88 -> Decision(
                "high_reputation", "声望极高", "声望", "良",
                "你的名字在本系统内被反复提起。人们谈起你时，用的词是「靠得住」。"
            )

            attrs.wealth >= 80 && index <= 4 -> Decision(
                "business_turn", "商海转身", "转折", "中",
                "在体制内走到中途，你选择了另一条路。往后的事，与档案无关了。"
            )

            lowGrades >= 4 || (attrs.leaderTrust <= 35 && attrs.reputation <= 40) -> Decision(
                "frustration", "事业受挫", "职业", "憾",
                "连续的差评与冷落之后，你被安排到了边缘岗位。日子平静，只是心里清楚发生了什么。"
            )

            attrs.health >= 70 && attrs.family >= 70 && index >= 2 -> Decision(
                "balanced_life", "平衡人生", "类型", "优",
                "你没有把一切都押在工作上，也没有辜负岗位。家庭和身体都还在，这很难得。"
            )

            state.promotionCount == 0 && years >= 15 -> Decision(
                "stagnation", "职业停滞", "职业", "憾",
                "很多年过去了，你的职级几乎没有变化。你依然每天准时上班，只是不再谈将来。"
            )

            index <= 1 && years >= 20 -> Decision(
                "grassroots", "基层干部", "职业", "中",
                "你一辈子都在基层。职务不高，但你熟悉这里的每一条路、每一户人。"
            )

            // ===== 遗憾型：事业不错，但某些方面被牺牲 =====
            evaluation.governance >= 72 && (evaluation.family < 48 || evaluation.integrity < 50) -> Decision(
                "regretful_success", "遗憾的成功", "人生", "中",
                "从履历看，你做得很好；只有你自己知道，代价是什么。"
            )

            attrs.family <= 25 -> Decision(
                "family_break", "家庭破裂", "家庭", "败",
                "你把最好的年华给了工作，家里的位置却越来越空。"
            )

            attrs.health <= 30 -> Decision(
                "health_ruin", "健康透支", "身体", "败",
                "退休那年，你的体检报告比履历更厚。工作上的账清了，身体的账没有。"
            )

            attrs.integrityRisk >= 70 -> Decision(
                "integrity_fail", "廉政风险失败", "纪律", "败",
                "那些当时看起来「没什么」的选择，在多年后被一件件翻了出来。"
            )

            attrs.reputation <= 20 && attrs.work <= 25 -> Decision(
                "total_fail", "最终失败", "职业", "败",
                "既没有做出成绩，也没有留下口碑。你提前结束了这段职业生涯。"
            )

            // ===== 转折型：有过中途改变方向的关键选择 =====
            career.records.count { it.kind == CareerRecordKind.CHOICE } >= 3 &&
                career.positionsHeld >= 3 && index >= 2 -> Decision(
                "midway_turn", "转折人生", "人生", "中",
                "你在中途换过方向，也放弃过一些东西。回头看，正是那几次犹豫和决定，把路拐向了这里。"
            )

            evaluation.overall in 40..66 -> Decision(
                "ordinary_life", "平凡人生", "人生", "中",
                "没有惊天动地的成绩，也没有难以弥补的遗憾。你把一段普通的仕途走得完整而踏实。"
            )

            else -> Decision(
                "normal_retire", "正常退休", "职业", "中",
                "三十年后你按时退休。履历平稳，没有大起大落，也没有留下什么争议。"
            )
        }
    }

    private fun reasonsOf(
        state: GameState,
        evaluation: LifeEvaluation,
        decision: Decision
    ): List<String> {
        val reasons = mutableListOf<String>()
        val attrs = state.player.attributes
        reasons += "${decision.category}结局 · 人生评价 ${evaluation.overall}（${evaluation.rating}）"
        reasons += "最强的一项是${evaluation.strongest.label} ${evaluation.scoreOf(evaluation.strongest)}，" +
            "最弱的一项是${evaluation.weakest.label} ${evaluation.scoreOf(evaluation.weakest)}"
        reasons += "走过 ${state.player.serviceYears} 年，历经 ${state.career.positionsHeld} 个岗位，" +
            "最终职位 ${state.displayPositionTitle}"
        state.route?.let { reasons += "长期发展方向：${it.label}" }
        reasons += "主导路线倾向：${state.politics.dominantTendency.label}"
        if (state.warningCount > 0) reasons += "任内有 ${state.warningCount} 次被提醒谈话"
        if (attrs.family <= 40) reasons += "家庭关系偏低（${attrs.family}），是这段人生里被牺牲的部分"
        if (attrs.integrityRisk >= 55) reasons += "廉政风险偏高（${attrs.integrityRisk}），留下了隐患"
        if (state.career.unlockedAchievements.size >= 8) {
            reasons += "累计解锁 ${state.career.unlockedAchievements.size} 项成就"
        }
        return reasons
    }

    private fun keyNodesOf(state: GameState): List<String> {
        val important = setOf(
            CareerRecordKind.PROMOTION,
            CareerRecordKind.TRANSFER,
            CareerRecordKind.PROJECT,
            CareerRecordKind.HONOR,
            CareerRecordKind.SETBACK,
            CareerRecordKind.CHOICE,
            CareerRecordKind.FAMILY
        )
        return state.career.records
            .filter { it.kind in important }
            .takeLast(8)
            .reversed()
            .map { "第 ${it.year} 年 · ${it.title}" }
    }

    private fun keyChoicesOf(state: GameState): List<String> = state.politics.history
        .takeLast(5)
        .reversed()
        .map { "你在第 ${it.year} 年选择了「${it.choice}」" }
}
