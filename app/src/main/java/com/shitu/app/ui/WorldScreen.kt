package com.shitu.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.GameState
import com.shitu.app.game.ProjectEngine
import com.shitu.app.game.RegionEngine
import com.shitu.app.game.WorldSimulationEngine
import com.shitu.app.model.DistrictState
import com.shitu.app.model.NewsLevel
import com.shitu.app.game.NpcCatalog
import com.shitu.app.model.RegionCatalog
import com.shitu.app.model.WorldNews
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.EmptyHint
import com.shitu.app.ui.components.InfoPair
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituCard
import com.shitu.app.ui.components.ThinDivider

/**
 * 第十三阶段：世界动态页面。
 *
 * 玩家在首页、政务、事件之外，可以在这里看到「世界自己在发生什么」：
 * 经济周期、地区变化、人物动向、派系格局、项目进展与长期政策造成的后果。
 * 只展示结果与趋势，具体公式留在后台。
 */
@Composable
fun WorldScreen(state: GameState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val world = state.world
    val economy = world.economy
    val playerDistrict = world.districtOf(state.regionId)
    val development = WorldSimulationEngine.worldDevelopment(world)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        ShituCard {
            Text("世界动态", style = MaterialTheme.typography.headlineMedium, color = scheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "第 ${state.year} 年 · 世界时间第 ${world.month} 个月",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            ThinDivider()
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("时代", world.era.label, modifier = Modifier.weight(1f))
                InfoPair("经济周期", economy.phase.label, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoPair("景气指数", "${economy.index}", modifier = Modifier.weight(1f))
                InfoPair("整体发展", "$development", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(world.era.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(economy.phase.description, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "本月动态") {
            val news = world.recentNews(14)
            if (news.isEmpty()) {
                EmptyHint("世界暂时风平浪静。推进时间后，这里会出现经济、地区与人物动态。")
            } else {
                news.forEach { item ->
                    NewsRow(item)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "当前任职地区") {
            Text(
                "${RegionCatalog.findOrDefault(state.regionId).name} · 主导产业 ${playerDistrict.dominantIndustry.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            DistrictBars(playerDistrict, scheme.primary)
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "地区对比") {
            val others = RegionCatalog.regions
                .map { world.districtOf(it.id) }
                .sortedByDescending { it.development }
            others.forEach { district ->
                val region = RegionCatalog.findOrDefault(district.id)
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(region.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        Spacer(modifier = Modifier.height(0.dp))
                        Text(
                            "  发展 ${district.development} · 经济 ${district.economy} · 环境 ${district.environment}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "人物动向") {
            val runtimes = world.npcs.values
                .mapNotNull { runtime -> NpcCatalog.find(runtime.npcId)?.let { it to runtime } }
                .sortedByDescending { it.second.power }
                .take(8)
            if (runtimes.isEmpty()) {
                EmptyHint("还没有掌握足够的人物动态。")
            } else {
                runtimes.forEach { (profile, runtime) ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "${profile.name} · ${profile.title}" + if (!runtime.active) "（已调离）" else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            "声望 ${runtime.power} · ${runtime.agenda.ifEmpty { "按部就班地做事" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "派系格局") {
            val influences = state.politics.factionInfluence.entries.sortedByDescending { it.value }
            if (influences.isEmpty()) {
                EmptyHint("派系格局尚未形成。")
            } else {
                influences.forEach { (faction, value) ->
                    AttributeBar(label = faction.label, value = value, barColor = scheme.secondary)
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "影响力随时间缓慢变化，不会因为一件事突然翻盘。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "项目进展") {
            val active = ProjectEngine.activeOf(state)
            if (active.isEmpty()) {
                EmptyHint("当前没有在建项目。")
            } else {
                active.forEach { project ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            ProjectEngine.labelOf(project.id),
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface
                        )
                        Text(
                            ProjectEngine.progressText(project),
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SectionCard(title = "政策记忆") {
            val policies = world.policies.takeLast(8).reversed()
            if (policies.isEmpty()) {
                EmptyHint("你还没有推动过成型的政策或项目。")
            } else {
                policies.forEach { record ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(
                            "第 ${record.year} 年 · ${record.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface
                        )
                        Text(
                            record.direction + " · " + record.tags.joinToString("、") { it.label },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "这些做法会在一段时间后产生长期影响，\u201c世界动态\u201d里会看到结果。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NewsRow(item: WorldNews) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        LevelTag(item.level)
        Spacer(modifier = Modifier.height(0.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
            Text(item.detail, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LevelTag(level: NewsLevel) {
    val scheme = MaterialTheme.colorScheme
    val color = when (level) {
        NewsLevel.HIGH -> scheme.error
        NewsLevel.MEDIUM -> scheme.primary
        NewsLevel.LOW -> scheme.onSurfaceVariant
    }
    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun DistrictBars(district: DistrictState, color: androidx.compose.ui.graphics.Color) {
    AttributeBar("经济", district.economy, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("就业", district.employment, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("教育", district.education, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("医疗", district.medical, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("治安", district.security, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("环境", district.environment, color)
    Spacer(modifier = Modifier.height(6.dp))
    AttributeBar("财政", district.finance.coerceAtMost(100), color)
}