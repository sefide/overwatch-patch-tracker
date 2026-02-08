package com.overwatch.patchtracker

import com.overwatch.patchtracker.domain.ChangeType
import com.overwatch.patchtracker.service.OverwatchPatchScraper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.jsoup.Jsoup
import java.time.LocalDate

class OverwatchPatchScraperTest : FunSpec({
    
    val scraper = OverwatchPatchScraper()
    
    test("패치 노트 페이지 접근 테스트") {
        val url = "https://overwatch.blizzard.com/en-us/news/patch-notes"
        
        val document = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; OverwatchPatchTracker/1.0)")
            .timeout(10000)
            .get()
        
        document shouldNotBe null
        println("페이지 타이틀: ${document.title()}")
    }
    
    test("최신 패치 노트 스크래핑") {
        val patchNotes = scraper.scrapeLatestPatch()
        
        println("\n=== 스크래핑 결과 ===")
        println("발견된 패치 수: ${patchNotes.size}")
        
        patchNotes.forEach { patch ->
            println("\n날짜: ${patch.date}")
            println("버전: ${patch.version}")
            println("업데이트된 영웅 수: ${patch.heroUpdates.size}")
            
            patch.heroUpdates.take(3).forEach { heroUpdate ->
                println("\n  영웅: ${heroUpdate.heroName}")
                if (heroUpdate.developerComment != null) {
                    println("  개발자 코멘트: ${heroUpdate.developerComment.take(100)}...")
                }
                println("  변경사항 수: ${heroUpdate.changes.size}")
                
                heroUpdate.changes.take(2).forEach { change ->
                    println("    - [${change.changeType}] ${change.abilityName ?: "General"}")
                    println("      ${change.description}")
                    if (change.previousValue != null && change.newValue != null) {
                        println("      (${change.previousValue} → ${change.newValue} ${change.unit ?: ""})")
                    }
                }
            }
        }
    }
    
    test("특정 연월 패치 노트 스크래핑") {
        val patchNotes = scraper.scrapePatchNotes(2026, 1)
        
        println("\n=== 2026년 1월 패치 노트 ===")
        patchNotes.forEach { patch ->
            println("\n${patch.date} - ${patch.version}")
            println("영웅 업데이트: ${patch.heroUpdates.map { it.heroName }.joinToString(", ")}")
        }
    }
    
    test("변경 타입 감지 테스트") {
        val testCases = mapOf(
            "Damage increased from 70 to 75" to ChangeType.BUFF,
            "Damage reduced from 80 to 70" to ChangeType.NERF,
            "Cooldown decreased from 5 to 4 seconds" to ChangeType.BUFF,
            "Fixed an issue where..." to ChangeType.BUG_FIX,
            "New ability added" to ChangeType.NEW_ABILITY,
            "Ammo changed from 24 to 30" to ChangeType.ADJUSTMENT
        )
        
        println("\n=== 변경 타입 감지 테스트 ===")
        testCases.forEach { (text, expectedType) ->
            val result = scraper.determineChangeType(text)
            println("$text -> $result (예상: $expectedType)")
            result shouldBe expectedType
        }
    }
    
    test("수치 추출 테스트") {
        val testCases = listOf(
            "Damage reduced from 80 to 70" to Triple("80", "70", "damage"),
            "Cooldown increased from 4 to 5 seconds" to Triple("4", "5", "seconds"),
            "Ammo increased from 24 to 30" to Triple("24", "30", "ammo"),
            "Health decreased from 250 to 225" to Triple("250", "225", "health")
        )
        
        println("\n=== 수치 추출 테스트 ===")
        testCases.forEach { (text, expected) ->
            val (prev, new, unit) = scraper.extractValues(text)
            println("$text")
            println("  추출: $prev → $new ($unit)")
            println("  예상: ${expected.first} → ${expected.second} (${expected.third})")
            
            prev shouldBe expected.first
            new shouldBe expected.second
            unit shouldContain expected.third
        }
    }
    
    test("날짜 파싱 테스트") {
        val testDates = listOf(
            "January 20, 2026" to LocalDate.of(2026, 1, 20),
            "December 18, 2025" to LocalDate.of(2025, 12, 18),
            "March 5, 2024" to LocalDate.of(2024, 3, 5)
        )
        
        println("\n=== 날짜 파싱 테스트 ===")
        testDates.forEach { (text, expected) ->
            val result = scraper.extractDate(text)
            println("$text -> $result (예상: $expected)")
            result shouldBe expected
        }
    }
    
    test("영웅별 변경 통계") {
        val patchNotes = scraper.scrapeLatestPatch()
        
        val heroStats = patchNotes
            .flatMap { it.heroUpdates }
            .groupBy { it.heroName }
            .mapValues { (_, updates) ->
                val totalChanges = updates.sumOf { it.changes.size }
                val buffs = updates.flatMap { it.changes }.count { it.changeType == ChangeType.BUFF }
                val nerfs = updates.flatMap { it.changes }.count { it.changeType == ChangeType.NERF }
                Triple(totalChanges, buffs, nerfs)
            }
            .toList()
            .sortedByDescending { it.second.first }
        
        println("\n=== 영웅별 변경 통계 ===")
        heroStats.take(15).forEach { (hero, stats) ->
            val (total, buffs, nerfs) = stats
            println("$hero: 총 $total개 변경 (버프: $buffs, 너프: $nerfs)")
        }
    }
})
