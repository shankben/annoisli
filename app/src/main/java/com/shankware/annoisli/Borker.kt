package com.shankware.annoisli

import kotlin.random.Random

object Borker {
    fun bork(probability: Double = 0.01) {
        if (Random.nextFloat() < probability) {
            throw Error("Borked!")
        }
    }
}