package com.overwatch.patchtracker.dto

import com.overwatch.patchtracker.domain.ChangeType
import java.time.LocalDate

data class PatchNoteDto(
    val date: LocalDate,
    val version: String,
    val heroUpdates: List<HeroUpdateDto>
)

data class HeroUpdateDto(
    val heroName: String,
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
