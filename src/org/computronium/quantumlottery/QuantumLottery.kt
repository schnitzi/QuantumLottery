package org.computronium.quantumlottery

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.google.common.math.BigIntegerMath
import java.io.IOException
import java.math.BigInteger
import java.net.URL
import java.text.NumberFormat

/**
 * Picks lottery numbers based on quantum random numbers sourced from https://qrng.anu.edu.au/.es
 */
object QuantumLottery {

    private val FORMAT = NumberFormat.getIntegerInstance()

    @Throws(IOException::class)
    private fun generateLotteryNumbers(balls: Int, max: Int) {
        val combinations = getHowManyCombinations(balls, max)
        println("For $balls balls numbered 1 to $max, there are ${FORMAT.format(combinations)} possible combinations.")

        val bitsNeeded = getBitsNeeded(combinations)
        println("To generate a random number in that range requires $bitsNeeded bits.")

        val randomNumberMax = getRandomNumberMax(bitsNeeded)
        println("This will give us a random number in the range 1 to ${FORMAT.format(randomNumberMax)}.")

        if (randomNumberMax > combinations) {
            println("That's a bit too big so ${FORMAT.format(randomNumberMax - combinations)} numbers will map to the same combination.")
        }

        val bytesNeeded = getBytesNeeded(bitsNeeded)
        println("QRNG generates random bytes, not bits, so we'll need at least $bytesNeeded bytes.")

        val randomBytes: MutableList<Byte> = getRandomBytes(bytesNeeded)
        println("The random bytes we got from the QRNG are $randomBytes.")

        val randomBits = getRandomBits(randomBytes)
        println("These bytes convert to these random bits: $randomBits.")

        val truncatedRandomBits = randomBits.substring(0..bitsNeeded-1)
        println("Truncating to the minimum number of bits we need gives us $truncatedRandomBits.")

        var randomNumber = BigInteger(truncatedRandomBits, 2)
        println("Converted back into an integer, that's ${FORMAT.format(randomNumber)}.")

        if (randomNumber >= combinations) {
            randomNumber %= combinations
            println("Since this is too large, we mod it by ${FORMAT.format(combinations)} to get ${FORMAT.format(randomNumber)}.")
        }

        val lotteryNumbers = getLotteryNumbers(balls, randomNumber)
        println("The corresponding lottery numbers for ${FORMAT.format(randomNumber)} are $lotteryNumbers")
    }

    private fun getRandomBits(randomBytes: MutableList<Byte>): String {
        return randomBytes.joinToString("") { bitString(it) }
    }

    private fun bitString(byte: Byte): CharSequence {
        val longBitString = "00000000" + BigInteger.valueOf(byte.toLong() and 0xFF).toString(2)
        return longBitString.substring(longBitString.length - 8)
    }

    private fun getBytesNeeded(bitsNeeded: Int) = (bitsNeeded-1) / 8 + 1

    private fun getRandomNumberMax(bitsNeeded: Int): BigInteger {
        return BigInteger.valueOf(2).pow(bitsNeeded)
    }

    /**
     *  Determines the set of lottery numbers corresponding to the index which was generated as
     *  a quantum random number, using the algorithm at
     *  {@link https://en.wikipedia.org/wiki/Combinatorial_number_system#Finding_the_k-combination_for_a_given_number}
     *  to pick a combination.
     */
    private fun getLotteryNumbers(balls: Int, randomNumber: BigInteger): MutableList<Int> {
        val lotteryNumbers = mutableListOf<Int>()
        var k = balls
        var position = randomNumber
        for (ball in 1..balls) {
            var i = k-2
            var previousNChooseK: BigInteger
            var nChooseK = BigInteger.ZERO
            do {
                i++
                previousNChooseK = nChooseK
                nChooseK = computeNChooseK(i, k)
            } while (nChooseK <= position)
            lotteryNumbers.add(0, i)
            position -= previousNChooseK
            k--
        }
        return lotteryNumbers
    }

    private fun computeNChooseK(n: Int, k: Int): BigInteger {
        if (k>n) return BigInteger.ZERO
        return BigIntegerMath.factorial(n) / BigIntegerMath.factorial(k) / BigIntegerMath.factorial(n-k)
    }

    private fun getRandomBytes(bytesNeeded: Int): MutableList<Byte> {
        val qrngResult = URL(String.format("https://qrng.anu.edu.au/API/jsonI.php?length=%d&type=uint8", bytesNeeded)).readText()
        val parser = Parser()
        val stringBuilder = StringBuilder(qrngResult)
        val json: JsonObject = parser.parse(stringBuilder) as JsonObject
        val arrayData = json["data"] as JsonArray<Byte>
        return arrayData.value
    }

    private fun getBitsNeeded(combinations: BigInteger): Int {
        var bitsNeeded = 0
        var product = BigInteger.ONE
        while (product < combinations) {
            product = product.multiply(BigInteger.valueOf(2))
            bitsNeeded++
        }
        return bitsNeeded
    }

    private fun getHowManyCombinations(balls: Int, max: Int): BigInteger {
        var numerator: Long = 1
        var denominator: Long = 1
        for (i in 0 until balls) {
            numerator *= (max - i).toLong()
            denominator *= (i + 1).toLong()
        }
        return BigInteger.valueOf(numerator / denominator)
    }

    @Throws(IOException::class)
    @JvmStatic fun main(args: Array<String>) {
        generateLotteryNumbers(6, 59)
    }
}
