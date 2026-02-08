package com.overwatch.patchtracker.service

import com.overwatch.patchtracker.domain.ChangeType
import com.overwatch.patchtracker.dto.BalanceChangeDto
import com.overwatch.patchtracker.dto.HeroUpdateDto
import com.overwatch.patchtracker.dto.PatchNoteDto
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class OverwatchPatchScraper {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val baseUrl = "https://overwatch.blizzard.com/en-us/news/patch-notes"

    fun scrapeLatestPatch(): List<PatchNoteDto> {
        logger.info("Scraping latest patch notes from: $baseUrl")

        val document = Jsoup.connect(baseUrl)
            .userAgent("Mozilla/5.0 (compatible; OverwatchPatchTracker/1.0)")
            .timeout(15000)
            .get()

        return parsePatchNotes(document)
    }

    fun scrapePatchNotes(year: Int, month: Int): List<PatchNoteDto> {
        val url = "$baseUrl/$year/$month/"
        logger.info("Scraping patch notes from: $url")

        val document = try {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (compatible; OverwatchPatchTracker/1.0)")
                .timeout(15000)
                .get()
        } catch (e: Exception) {
            logger.error("Failed to scrape $url", e)
            return emptyList()
        }

        return parsePatchNotes(document)
    }

    private fun parsePatchNotes(document: Document): List<PatchNoteDto> {
        val patchNotes = mutableListOf<PatchNoteDto>()

        // 날짜 헤더 찾기 (예: "January 20, 2026")
        val dateHeaders = document.select("h3").toList()
            .filter { it.text().matches(Regex(".*\\d{4}$")) }

        logger.info("Found ${dateHeaders.size} date headers")

        dateHeaders.forEach { dateHeader ->
            try {
                val dateText = dateHeader.text()
                val patchDate = extractDate(dateText)

                // 다음 섹션까지의 내용 수집
                var currentElement = dateHeader.nextElementSibling()
                val sectionElements = mutableListOf<Element>()

                while (currentElement != null && !currentElement.tagName().equals("h3", ignoreCase = true)) {
                    sectionElements.add(currentElement)
                    currentElement = currentElement.nextElementSibling()
                }

                logger.debug("Found ${sectionElements.size} elements in section")
                logger.debug("Element tags: ${sectionElements.map { it.tagName() }.distinct()}")

                // 영웅 업데이트 파싱
                val heroUpdates = parseHeroUpdatesFromSection(sectionElements)

                if (heroUpdates.isNotEmpty()) {
                    patchNotes.add(
                        PatchNoteDto(
                            date = patchDate,
                            version = "Patch $dateText",
                            heroUpdates = heroUpdates
                        )
                    )
                    logger.info("Parsed patch for $patchDate with ${heroUpdates.size} hero updates")
                }
            } catch (e: Exception) {
                logger.error("Failed to parse patch note section", e)
            }
        }

        return patchNotes
    }
    private fun parseHeroUpdatesFromSection(elements: List<Element>): List<HeroUpdateDto> {
        val updates = mutableListOf<HeroUpdateDto>()

        // div 안쪽까지 탐색
        val allElements = elements.flatMap { element ->
            if (element.tagName() == "div") {
                element.select("*").toList()
            } else {
                listOf(element)
            }
        }

        // PatchNotesHeroUpdate 클래스 div 찾기
        val heroUpdateDivs = allElements.filter {
            it.tagName() == "div" && it.className().contains("PatchNotesHeroUpdate") && !it.className().contains("-")
        }

        logger.info("Found ${heroUpdateDivs.size} PatchNotesHeroUpdate divs")

        heroUpdateDivs.forEach { heroDiv ->
            try {
                // h5에서 영웅 이름 추출
                val h5 = heroDiv.select("h5").firstOrNull()
                if (h5 == null) {
                    logger.debug("No h5 found in hero div")
                    return@forEach
                }

                val heroName = cleanHeroName(h5.text())
                logger.debug("Processing hero: $heroName")

                // 개발자 코멘트 찾기
                val devCommentDiv = heroDiv.select("div.PatchNotes-dev").firstOrNull()
                val developerComment = devCommentDiv?.select("p")?.firstOrNull()?.text()

                // generalUpdates div에서 변경사항 찾기
                val generalUpdatesDiv = heroDiv.select("div.PatchNotesHeroUpdate-generalUpdates").firstOrNull()
                val changes = if (generalUpdatesDiv != null) {
                    parseBalanceChangesFromDiv(generalUpdatesDiv)
                } else {
                    emptyList()
                }

                logger.info("Hero: $heroName - Changes: ${changes.size}")

                if (changes.isNotEmpty()) {
                    updates.add(
                        HeroUpdateDto(
                            heroName = heroName,
                            developerComment = developerComment,
                            changes = changes
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to parse hero div", e)
            }
        }

        return updates
    }

    private fun parseBalanceChangesFromDiv(generalUpdatesDiv: Element): List<BalanceChangeDto> {
        val changes = mutableListOf<BalanceChangeDto>()
        val children = generalUpdatesDiv.children().toList()
        var currentAbility: String? = null

        children.forEach { element ->
            when (element.tagName()) {
                "p" -> {
                    val text = element.text()
                    if (text.isNotBlank() && !text.startsWith("Developer")) {
                        currentAbility = text
                    }
                }
                "ul" -> {
                    element.select("li").forEach { li ->
                        val changeText = li.text()
                        if (changeText.isNotBlank()) {
                            changes.add(parseChange(currentAbility, changeText))
                        }
                    }
                }
            }
        }

        return changes
    }

    private fun parseBalanceChanges(heroHeader: Element): List<BalanceChangeDto> {
        val changes = mutableListOf<BalanceChangeDto>()
        var currentElement = heroHeader.nextElementSibling()
        var currentAbility: String? = null

        while (currentElement != null) {
            when {
                // 다음 영웅 섹션이면 중단
                currentElement.tagName() == "h5" -> break

                // 능력 이름 (일반 paragraph)
                currentElement.tagName() == "p" -> {
                    val text = currentElement.text()
                    // Developer Comment가 아니고, 리스트 아이템도 아니면 능력 이름
                    if (!text.startsWith("Developer") && text.isNotBlank() && !text.startsWith("*")) {
                        currentAbility = text
                    }
                }

                // 변경사항 리스트
                currentElement.tagName() == "ul" -> {
                    currentElement.select("li").forEach { li ->
                        val changeText = li.text()
                        if (changeText.isNotBlank()) {
                            changes.add(parseChange(currentAbility, changeText))
                        }
                    }
                }
            }

            currentElement = currentElement.nextElementSibling()
        }

        return changes
    }

    fun parseChange(abilityName: String?, changeText: String): BalanceChangeDto {
        val changeType = determineChangeType(changeText)
        val (prevValue, newValue, unit) = extractValues(changeText)

        return BalanceChangeDto(
            abilityName = abilityName,
            changeType = changeType,
            description = changeText,
            previousValue = prevValue,
            newValue = newValue,
            unit = unit
        )
    }

    fun determineChangeType(text: String): ChangeType {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("increased") -> ChangeType.BUFF
            lowerText.contains("reduced") || lowerText.contains("decreased") -> ChangeType.NERF
            lowerText.contains("fixed") -> ChangeType.BUG_FIX
            lowerText.contains("new") && (lowerText.contains("ability") || lowerText.contains("feature")) -> ChangeType.NEW_ABILITY
            else -> ChangeType.ADJUSTMENT
        }
    }

    fun extractValues(text: String): Triple<String?, String?, String?> {
        // "from X to Y" 패턴 매칭
        val fromToPattern = """from\s+(\d+\.?\d*)\s+to\s+(\d+\.?\d*)""".toRegex()
        val match = fromToPattern.find(text)

        return if (match != null) {
            val prev = match.groupValues[1]
            val new = match.groupValues[2]
            val unit = extractUnit(text)
            Triple(prev, new, unit)
        } else {
            Triple(null, null, null)
        }
    }

    private fun extractUnit(text: String): String? {
        val units = listOf(
            "damage", "health", "meters", "seconds", "ammo",
            "percent", "%", "degrees", "HP", "DPS"
        )
        return units.find { text.contains(it, ignoreCase = true) }
    }

    fun extractDate(text: String): LocalDate {
        // "January 20, 2026" 형식 파싱
        val datePattern = """([A-Z][a-z]+)\s+(\d{1,2}),\s+(\d{4})""".toRegex()
        val match = datePattern.find(text)

        return if (match != null) {
            val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
            LocalDate.parse(match.value, formatter)
        } else {
            logger.warn("Could not parse date from: $text, using current date")
            LocalDate.now()
        }
    }

    private fun findDeveloperComment(heroHeader: Element): String? {
        var element = heroHeader.nextElementSibling()

        while (element != null && element.tagName() != "h5") {
            val text = element.text()
            if (text.startsWith("Developer Comment", ignoreCase = true)) {
                return text.removePrefix("Developer Comment:").trim()
            }
            element = element.nextElementSibling()
        }

        return null
    }

    private fun cleanHeroName(name: String): String {
        // "D.Va", "Soldier: 76" 등 특수 케이스 처리
        return name.trim()
    }
}
