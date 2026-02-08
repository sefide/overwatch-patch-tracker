package com.overwatch.patchtracker.domain

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "hero_updates")
data class HeroUpdate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    val heroName: String,
    val patchDate: LocalDate,
    val patchVersion: String,
    
    @OneToMany(mappedBy = "heroUpdate", cascade = [CascadeType.ALL], orphanRemoval = true)
    val changes: MutableList<BalanceChange> = mutableListOf(),
    
    @Column(columnDefinition = "TEXT")
    val developerComment: String? = null,
    
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as HeroUpdate
        
        if (id != null && id == other.id) return true
        return heroName == other.heroName && patchDate == other.patchDate
    }
    
    override fun hashCode(): Int {
        return heroName.hashCode() * 31 + patchDate.hashCode()
    }
    
    override fun toString(): String {
        return "HeroUpdate(id=$id, heroName='$heroName', patchDate=$patchDate, patchVersion='$patchVersion')"
    }
}

@Entity
@Table(name = "balance_changes")
data class BalanceChange(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hero_update_id")
    val heroUpdate: HeroUpdate,
    
    val abilityName: String?, // "Rocket Punch", "The Viper" 등
    
    @Enumerated(EnumType.STRING)
    val changeType: ChangeType, // BUFF, NERF, ADJUSTMENT, BUG_FIX
    
    @Column(columnDefinition = "TEXT")
    val description: String,
    
    val previousValue: String? = null, // "80"
    val newValue: String? = null, // "70"
    val unit: String? = null // "damage", "meters", "seconds" 등
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as BalanceChange
        
        return id != null && id == other.id
    }
    
    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
    
    override fun toString(): String {
        return "BalanceChange(id=$id, abilityName=$abilityName, changeType=$changeType, description='$description')"
    }
}

enum class ChangeType {
    BUFF,      // 증가/강화
    NERF,      // 감소/약화
    ADJUSTMENT, // 변경 (버프도 너프도 아닌)
    BUG_FIX,   // 버그 수정
    NEW_ABILITY // 새로운 능력
}
