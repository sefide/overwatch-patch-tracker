package com.overwatch.patchtracker.controller

import com.overwatch.patchtracker.domain.ChangeType
import com.overwatch.patchtracker.domain.HeroUpdate
import com.overwatch.patchtracker.repository.HeroUpdateRepository
import com.overwatch.patchtracker.service.OverwatchPatchService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = ["*"]) // 프론트엔드 연결용
class PatchController(
    private val patchService: OverwatchPatchService,
    private val heroUpdateRepository: HeroUpdateRepository
) {
    
    /**
     * GET /api/heroes
     * 모든 영웅 목록 조회 (업데이트가 있는 영웅만)
     */
    @GetMapping("/heroes")
    fun getAllHeroes(): ResponseEntity<HeroListResponse> {
        val allUpdates = heroUpdateRepository.findAll()
        val heroNames = allUpdates.map { it.heroName }.distinct().sorted()
        
        return ResponseEntity.ok(HeroListResponse(
            count = heroNames.size,
            heroes = heroNames
        ))
    }
    
    /**
     * GET /api/heroes/{name}
     * 특정 영웅의 모든 업데이트 조회
     */
    @GetMapping("/heroes/{name}")
    fun getHeroUpdates(
        @PathVariable name: String,
        @RequestParam(required = false) limit: Int? = null
    ): ResponseEntity<HeroUpdatesResponse> {
        val updates = heroUpdateRepository.findByHeroNameOrderByPatchDateDesc(name)
        
        val limitedUpdates = if (limit != null) updates.take(limit) else updates
        
        return ResponseEntity.ok(HeroUpdatesResponse(
            heroName = name,
            totalUpdates = updates.size,
            updates = limitedUpdates.map { it.toDto() }
        ))
    }
    
    /**
     * GET /api/patches
     * 모든 패치 목록 조회 (최신순)
     */
    @GetMapping("/patches")
    fun getAllPatches(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false, defaultValue = "50") limit: Int
    ): ResponseEntity<PatchListResponse> {
        val updates = if (startDate != null && endDate != null) {
            heroUpdateRepository.findByPatchDateBetweenOrderByPatchDateDesc(startDate, endDate)
        } else {
            heroUpdateRepository.findAllByOrderByPatchDateDesc()
        }
        
        val groupedByDate = updates.groupBy { it.patchDate }
            .map { (date, updates) ->
                PatchSummary(
                    date = date,
                    version = updates.firstOrNull()?.patchVersion ?: "Unknown",
                    heroCount = updates.size,
                    heroes = updates.map { it.heroName }
                )
            }
            .take(limit)
        
        return ResponseEntity.ok(PatchListResponse(
            count = groupedByDate.size,
            patches = groupedByDate
        ))
    }
    
    /**
     * GET /api/patches/latest
     * 최신 패치 정보 조회
     */
    @GetMapping("/patches/latest")
    fun getLatestPatch(): ResponseEntity<LatestPatchResponse> {
        val latestUpdates = heroUpdateRepository.findAllByOrderByPatchDateDesc()
            .takeWhile { it.patchDate == heroUpdateRepository.findAllByOrderByPatchDateDesc().firstOrNull()?.patchDate }
        
        if (latestUpdates.isEmpty()) {
            return ResponseEntity.notFound().build()
        }
        
        val latestDate = latestUpdates.first().patchDate
        val version = latestUpdates.first().patchVersion
        
        return ResponseEntity.ok(LatestPatchResponse(
            date = latestDate,
            version = version,
            heroCount = latestUpdates.size,
            updates = latestUpdates.map { it.toDto() }
        ))
    }
    
    /**
     * GET /api/stats/buffs-nerfs
     * 영웅별 버프/너프 통계
     */
    @GetMapping("/stats/buffs-nerfs")
    fun getBuffNerfStats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<StatsResponse> {
        val updates = if (startDate != null && endDate != null) {
            heroUpdateRepository.findByPatchDateBetweenOrderByPatchDateDesc(startDate, endDate)
        } else {
            heroUpdateRepository.findAll()
        }
        
        val heroStats = updates.groupBy { it.heroName }
            .map { (heroName, heroUpdates) ->
                val allChanges = heroUpdates.flatMap { it.changes }
                
                HeroStats(
                    heroName = heroName,
                    totalChanges = allChanges.size,
                    buffs = allChanges.count { it.changeType == ChangeType.BUFF },
                    nerfs = allChanges.count { it.changeType == ChangeType.NERF },
                    adjustments = allChanges.count { it.changeType == ChangeType.ADJUSTMENT },
                    bugFixes = allChanges.count { it.changeType == ChangeType.BUG_FIX },
                    updateCount = heroUpdates.size
                )
            }
            .sortedByDescending { it.totalChanges }
        
        return ResponseEntity.ok(StatsResponse(
            totalHeroes = heroStats.size,
            dateRange = if (startDate != null && endDate != null) {
                DateRange(startDate, endDate)
            } else null,
            stats = heroStats
        ))
    }
    
    /**
     * POST /api/scrape/latest
     * 최신 패치 노트 스크래핑 (수동 트리거)
     */
    @PostMapping("/scrape/latest")
    fun scrapeLatest(): ResponseEntity<ScrapeResponse> {
        val savedCount = patchService.scrapeAndSaveLatest()
        
        return ResponseEntity.ok(ScrapeResponse(
            success = true,
            message = "Successfully scraped and saved $savedCount hero updates",
            savedCount = savedCount
        ))
    }
    
    /**
     * POST /api/scrape/month
     * 특정 연월 패치 노트 스크래핑
     */
    @PostMapping("/scrape/month")
    fun scrapeMonth(
        @RequestParam year: Int,
        @RequestParam month: Int
    ): ResponseEntity<ScrapeResponse> {
        if (year < 2016 || year > 2030 || month < 1 || month > 12) {
            return ResponseEntity.badRequest().body(
                ScrapeResponse(
                    success = false,
                    message = "Invalid year or month",
                    savedCount = 0
                )
            )
        }
        
        val savedCount = patchService.scrapeAndSave(year, month)
        
        return ResponseEntity.ok(ScrapeResponse(
            success = true,
            message = "Successfully scraped $year-$month and saved $savedCount hero updates",
            savedCount = savedCount
        ))
    }
    
    /**
     * GET /api/health
     * 서버 상태 확인
     */
    @GetMapping("/health")
    fun healthCheck(): ResponseEntity<HealthResponse> {
        val totalUpdates = heroUpdateRepository.count()
        val latestUpdate = heroUpdateRepository.findAllByOrderByPatchDateDesc().firstOrNull()
        
        return ResponseEntity.ok(HealthResponse(
            status = "UP",
            totalUpdates = totalUpdates,
            latestPatchDate = latestUpdate?.patchDate
        ))
    }
}

// Extension function
private fun HeroUpdate.toDto() = HeroUpdateDto(
    id = this.id,
    heroName = this.heroName,
    patchDate = this.patchDate,
    patchVersion = this.patchVersion,
    developerComment = this.developerComment,
    changes = this.changes.map { change ->
        BalanceChangeDto(
            abilityName = change.abilityName,
            changeType = change.changeType,
            description = change.description,
            previousValue = change.previousValue,
            newValue = change.newValue,
            unit = change.unit
        )
    }
)

// Response DTOs
data class HeroListResponse(
    val count: Int,
    val heroes: List<String>
)

data class HeroUpdatesResponse(
    val heroName: String,
    val totalUpdates: Int,
    val updates: List<HeroUpdateDto>
)

data class HeroUpdateDto(
    val id: Long?,
    val heroName: String,
    val patchDate: LocalDate,
    val patchVersion: String,
    val developerComment: String?,
    val changes: List<BalanceChangeDto>
)

data class BalanceChangeDto(
    val abilityName: String?,
    val changeType: ChangeType,
    val description: String,
    val previousValue: String?,
    val newValue: String?,
    val unit: String?
)

data class PatchListResponse(
    val count: Int,
    val patches: List<PatchSummary>
)

data class PatchSummary(
    val date: LocalDate,
    val version: String,
    val heroCount: Int,
    val heroes: List<String>
)

data class LatestPatchResponse(
    val date: LocalDate,
    val version: String,
    val heroCount: Int,
    val updates: List<HeroUpdateDto>
)

data class StatsResponse(
    val totalHeroes: Int,
    val dateRange: DateRange?,
    val stats: List<HeroStats>
)

data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class HeroStats(
    val heroName: String,
    val totalChanges: Int,
    val buffs: Int,
    val nerfs: Int,
    val adjustments: Int,
    val bugFixes: Int,
    val updateCount: Int
)

data class ScrapeResponse(
    val success: Boolean,
    val message: String,
    val savedCount: Int
)

data class HealthResponse(
    val status: String,
    val totalUpdates: Long,
    val latestPatchDate: LocalDate?
)
