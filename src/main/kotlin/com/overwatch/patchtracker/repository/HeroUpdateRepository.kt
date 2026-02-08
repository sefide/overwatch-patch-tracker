package com.overwatch.patchtracker.repository

import com.overwatch.patchtracker.domain.HeroUpdate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface HeroUpdateRepository : JpaRepository<HeroUpdate, Long> {
    fun findByHeroName(heroName: String): List<HeroUpdate>
    fun findByHeroNameOrderByPatchDateDesc(heroName: String): List<HeroUpdate>
    fun findByPatchDateBetweenOrderByPatchDateDesc(startDate: LocalDate, endDate: LocalDate): List<HeroUpdate>
    fun existsByHeroNameAndPatchDate(heroName: String, patchDate: LocalDate): Boolean
    fun findAllByOrderByPatchDateDesc(): List<HeroUpdate>
}
