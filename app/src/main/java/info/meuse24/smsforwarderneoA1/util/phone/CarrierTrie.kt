package info.meuse24.smsforwarderneoA1.util.phone

/**
 * Trie node for carrier prefix lookup.
 */
class CarrierNode {
    val children = Array<CarrierNode?>(10) { null }
    var carrier: String? = null
}

/**
 * Trie data structure for efficient carrier prefix matching.
 *
 * Used to identify mobile carriers based on phone number prefixes.
 * Supports longest prefix matching for overlapping prefixes.
 */
class CarrierTrie {
    private val root = CarrierNode()

    /**
     * Inserts a prefix-carrier mapping into the trie.
     */
    fun insert(prefix: String, carrier: String) {
        var current = root
        for (digit in prefix) {
            val index = digit - '0'
            if (current.children[index] == null) {
                current.children[index] = CarrierNode()
            }
            current = current.children[index] ?: throw IllegalStateException("CarrierNode sollte nicht null sein")
        }
        current.carrier = carrier
    }

    /**
     * Finds the longest matching prefix for a phone number.
     *
     * @param number The phone number to search
     * @return Pair of (carrier name, matched prefix) or (null, default prefix)
     */
    fun findLongestPrefix(number: String): Pair<String?, String> {
        var current = root
        var lastCarrier: String? = null
        var prefixLength = 0
        var lastValidPrefix = 0

        for ((index, digit) in number.withIndex()) {
            val digitIndex = digit - '0'
            val next = current.children[digitIndex] ?: break
            current = next
            if (current.carrier != null) {
                lastCarrier = current.carrier
                lastValidPrefix = index + 1
            }
            prefixLength++
        }

        return if (lastCarrier != null) {
            lastCarrier to number.substring(0, lastValidPrefix)
        } else {
            null to number.substring(0, 3.coerceAtMost(number.length))
        }
    }
}
