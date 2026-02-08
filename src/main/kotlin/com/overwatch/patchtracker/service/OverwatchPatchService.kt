package com.overwatch.patchtracker.service

import com.overwatch.patchtracker.domain.BalanceChange
import com.overwatch.patchtracker.domain.HeroUpdate
import com.overwatch.patchtracker.repository.HeroUpdateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OverwatchPatchService(
    private val scraper: OverwatchPatchScraper,
    private val heroUpdateRepository: HeroUpdateRepository
) {
    
    private val logger = LoggerFactory.getLogger(javaClass)
    
    @Transactional
    fun scrapeAndSaveLatest(): Int {
        logger.info("Starting to scrape latest patch notes")
        
        val patchNotes = scraper.scrapeLatestPatch()
        return savePatchNotes(patchNotes)
    }
    
    @Transactional
    fun scrapeAndSave(year: Int, month: Int): Int {
        logger.info("Starting to scrape patch notes for $year-$month")
        
        val patchNotes = scraper.scrapePatchNotes(year, month)
        return savePatchNotes(patchNotes)
    }
    
    private fun savePatchNotes(patchNotes: List<com.overwatch.patchtracker.dto.PatchNoteDto>): Int {
        var savedCount = 0
        
        patchNotes.forEach { patch ->
            patch.heroUpdates.forEach { heroUpdateDto ->
                // 중복 체크
                val exists = heroUpdateRepository.existsByHeroNameAndPatchDate(
                    heroUpdateDto.heroName,
                    patch.date
                )
                
                if (!exists) {
                    val heroUpdate = HeroUpdate(
                        heroName = heroUpdateDto.heroName,
                        patchDate = patch.date,
                        patchVersion = patch.version,
                        developerComment = heroUpdateDto.developerComment
                    )
                    
                    heroUpdateDto.changes.forEach { changeDto ->
                        val change = BalanceChange(
                            heroUpdate = heroUpdate,
                            abilityName = changeDto.abilityName,
                            changeType = changeDto.changeType,
                            description = changeDto.description,
                            previousValue = changeDto.previousValue,
                            newValue = changeDto.newValue,
                            unit = changeDto.unit
                        )
                        heroUpdate.changes.add(change)
                    }
                    
                    heroUpdateRepository.save(heroUpdate)
                    savedCount++
                    logger.info("Saved update for ${heroUpdate.heroName} on ${heroUpdate.patchDate}")
                } else {
                    logger.debug("Skipping duplicate: ${heroUpdateDto.heroName} on ${patch.date}")
                }
            }
        }
        
        logger.info("Saved $savedCount hero updates")
        return savedCount
    }
    
    fun getHeroUpdates(heroName: String): List<HeroUpdate> {
        return heroUpdateRepository.findByHeroNameOrderByPatchDateDesc(heroName)
    }
    
    fun getAllUpdates(): List<HeroUpdate> {
        return heroUpdateRepository.findAllByOrderByPatchDateDesc()
    }
}
