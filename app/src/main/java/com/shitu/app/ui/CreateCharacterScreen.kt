package com.shitu.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shitu.app.game.CharacterDraft
import com.shitu.app.game.CharacterFactory
import com.shitu.app.game.TraitOption
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.shitu.app.model.Attr
import com.shitu.app.model.CityData
import com.shitu.app.model.CityDataRepository
import com.shitu.app.model.Player
import com.shitu.app.model.RegionCatalog
import com.shitu.app.ui.components.AttributeBar
import com.shitu.app.ui.components.SectionCard
import com.shitu.app.ui.components.ShituBackground
import com.shitu.app.ui.components.ThinDivider

// 人物创建：姓名、性别、学历、初始职业，并实时预览初始属性。
@Composable
fun CreateCharacterScreen(
    onBack: () -> Unit,
    onStart: (CharacterDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(CharacterFactory.GENDER_OPTIONS.first()) }
    var education by remember { mutableStateOf(CharacterFactory.EDUCATION_OPTIONS[1].name) }
    var start by remember { mutableStateOf(CharacterFactory.START_OPTIONS.first().name) }
    var traits by remember { mutableStateOf(emptyList<String>()) }
    var regionId by remember { mutableStateOf(RegionCatalog.default.id) }

    val finalName = name.trim().ifEmpty { "无名" }
    val preview: Player = remember(finalName, gender, education, start, traits) {
        CharacterFactory.create(finalName, gender, education, start, traits)
    }

    ShituBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "创建人物",
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onBack) { Text("返回") }
            }
            Spacer(modifier = Modifier.height(16.dp))

            SectionCard(title = "基本信息") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(12) },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "性别",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OptionRow(
                    options = CharacterFactory.GENDER_OPTIONS,
                    selected = gender,
                    onSelect = { gender = it }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(title = "学历") {
                OptionRow(
                    options = CharacterFactory.EDUCATION_OPTIONS.map { it.name },
                    selected = education,
                    onSelect = { picked ->
                        education = picked
                        // 学历决定可选的初始职业：博士只能走中央选调生。
                        start = CharacterFactory.defaultStartOf(picked)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = CharacterFactory.educationOf(education).description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                LabelValueLine("预计起始年龄", "${CharacterFactory.startingAgeOf(education)} 岁")
                if (CharacterFactory.isDoctor(education)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LabelValueLine("特殊身份", CharacterFactory.CENTRAL_SELECTED_START)
                    Spacer(modifier = Modifier.height(6.dp))
                    LabelValueLine(
                        "起始职级",
                        com.shitu.app.model.positionAt(CharacterFactory.startingRankIndexOf(education)).name
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(title = "初始职业") {
                val doctor = CharacterFactory.isDoctor(education)
                if (doctor) {
                    // 博士不再显示普通基层职业选项。
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = scheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = CharacterFactory.CENTRAL_SELECTED_START,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "博士专属起点，直接以副处级进入游戏。",
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    OptionRow(
                        options = CharacterFactory.startOptionsFor(education).map { it.name },
                        selected = start,
                        onSelect = { start = it }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val chosenStart = CharacterFactory.startOf(
                    if (doctor) CharacterFactory.CENTRAL_SELECTED_START else start
                )
                Text(
                    text = "${chosenStart.unit.label} · ${chosenStart.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(
                title = "天赋特质",
                trailing = "已选 ${traits.size} / ${CharacterFactory.MAX_TRAITS}"
            ) {
                Text(
                    text = "最多选择两项。每一项天赋都有长处，也都有代价。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                TraitOptionList(
                    options = CharacterFactory.TRAIT_OPTIONS,
                    selected = traits,
                    onToggle = { name ->
                        traits = when {
                            name in traits -> traits - name
                            traits.size < CharacterFactory.MAX_TRAITS -> traits + name
                            else -> traits
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            SectionCard(title = "初始属性预览", trailing = "起始年龄 ${preview.ageAtStart} 岁") {
                listOf(
                    Attr.WORK,
                    Attr.ABILITY,
                    Attr.POLITICS,
                    Attr.WRITING,
                    Attr.MASSES,
                    Attr.LEADER_TRUST,
                    Attr.INTEGRITY_RISK,
                    Attr.HEALTH,
                    Attr.FAMILY,
                    Attr.WEALTH
                ).forEach { attr ->
                    AttributeBar(
                        label = attr.label,
                        value = preview.attributes.valueOf(attr),
                        barColor = previewColor(attr)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                ThinDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "不同学历与起点的初始属性不同，数值属于游戏设计，" +
                        "不代表现实规则。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionCard(title = "选择任职城市") {
                Text(
                    text = "城市名称与行政层级为真实数据；经济、人口、财政等为游戏化归一化数值。" +
                        "不同城市的产业、事件与治理难度都不同。",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                var regionFilter by remember { mutableStateOf("全部") }
                val regionOptions = remember {
                    listOf("全部") + CityDataRepository.cities.map { it.region }.distinct()
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(regionOptions) { option ->
                        FilterChip(
                            label = option,
                            selected = option == regionFilter,
                            onClick = { regionFilter = option }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val provinceOptions = remember(regionFilter) {
                    CityDataRepository.cities
                        .filter { regionFilter == "全部" || it.region == regionFilter }
                        .map { it.province }
                        .distinct()
                }
                var provinceFilter by remember(regionFilter) {
                    mutableStateOf(provinceOptions.firstOrNull().orEmpty())
                }
                val activeProvince =
                    if (provinceFilter in provinceOptions) provinceFilter else provinceOptions.firstOrNull().orEmpty()
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(provinceOptions) { option ->
                        FilterChip(
                            label = option,
                            selected = option == activeProvince,
                            onClick = { provinceFilter = option }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                val cityOptions = remember(activeProvince) {
                    CityDataRepository.byProvince(activeProvince)
                }
                cityOptions.forEach { city ->
                    CityOptionCard(
                        city = city,
                        selected = city.id == regionId,
                        onClick = { regionId = city.id }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))
                ThinDivider()
                Spacer(modifier = Modifier.height(10.dp))
                val chosen = CityDataRepository.findOrDefault(regionId)
                Text(
                    text = "已选择：${chosen.province} · ${chosen.name}（${chosen.cityType}）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "主要产业：${chosen.industryText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    onStart(
                        CharacterDraft(
                            name = finalName,
                            gender = gender,
                            education = education,
                            start = CharacterFactory.defaultStartOf(education),
                            traits = traits,
                            regionId = regionId
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("开始仕途", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 天赋多选列表：点击切换，超出上限时忽略。
@Composable
private fun TraitOptionList(
    options: List<TraitOption>,
    selected: List<String>,
    onToggle: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        options.forEach { option ->
            val isSelected = option.name in selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle(option.name) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) scheme.primaryContainer else scheme.surface,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) scheme.primary else scheme.outline
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) scheme.primary else scheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isSelected) "已选" else "点击选择",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) scheme.primary else scheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = option.upside,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.downside,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            OutlinedButton(
                onClick = { onSelect(option) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) scheme.primary else scheme.outline
                )
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant
                )
            }
        }
    }
}

// 城市筛选标签。
@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) scheme.primary else scheme.outline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

// 城市卡片：展示真实行政层级与游戏化属性星级，不出现任何「推荐 / 最佳」暗示。
@Composable
private fun CityOptionCard(city: CityData, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) scheme.primary else scheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = city.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) scheme.primary else scheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = city.cityType,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = city.province + " · 含权量 " + city.powerWeight,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "经济 " + starText(city.economic) +
                    "　政治 " + starText(city.politics) +
                    "　文化 " + starText(city.culture),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Text(
                text = "科教 " + starText(city.education) +
                    "　国际 " + starText(city.internationalInfluence) +
                    "　战略 " + starText(city.strategicImportance),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = city.industryText,
                style = MaterialTheme.typography.labelSmall,
                color = scheme.tertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = city.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

private fun starText(value: Int): String {
    val filled = ((value.coerceIn(0, 100) + 19) / 20).coerceIn(1, 5)
    return "★".repeat(filled) + "☆".repeat(5 - filled)
}

/** 创建页的一行「标签 + 数值」，用于预计起始年龄与其他客观信息。 */
@Composable
private fun LabelValueLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun previewColor(attr: Attr) = when (attr) {
    Attr.INTEGRITY_RISK -> MaterialTheme.colorScheme.error
    Attr.HEALTH, Attr.FAMILY, Attr.WEALTH, Attr.ENERGY -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.primary
}
