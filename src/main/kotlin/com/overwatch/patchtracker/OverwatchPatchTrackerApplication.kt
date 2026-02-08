package com.overwatch.patchtracker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OverwatchPatchTrackerApplication

fun main(args: Array<String>) {
    runApplication<OverwatchPatchTrackerApplication>(*args)
}
