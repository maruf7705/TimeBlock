package com.blocks.tracker.notification

data class CheckInMessage(val title: String, val body: String)

object CheckInMessageManager {

    val firstNotificationPool = listOf(
        CheckInMessage(
            title = "৪৫ মিনিট পার হলো",
            body = "এই সময়টায় কী করছিলে? ১০ সেকেন্ডেই লিখে ফেলো।"
        ),
        CheckInMessage(
            title = "এক মুহূর্ত থামো",
            body = "এখনকার কাজটার নাম কী?"
        ),
        CheckInMessage(
            title = "ছোট্ট check-in",
            body = "একই কাজ চলছে, নাকি নতুন কিছু শুরু করলে?"
        ),
        CheckInMessage(
            title = "তোমার সময়ের হিসাব",
            body = "গত ৪৫ মিনিট এখনো টাটকা মনে আছে। এখনই লিখলে এক ট্যাপেই হয়ে যায়।"
        ),
        CheckInMessage(
            title = "সময় এগোচ্ছে",
            body = "এই সময়টা কীভাবে কাটালে?"
        )
    )

    val followupPool = listOf(
        CheckInMessage(
            title = "এখনও খালি আছে",
            body = "শেষ কাজটার নাম দিলেই হবে।"
        ),
        CheckInMessage(
            title = "একটু সময় হবে?",
            body = "এক ট্যাপে শেষ।"
        ),
        CheckInMessage(
            title = "সময়টা হারিয়ে যাচ্ছে",
            body = "এখন লিখলে মনে থাকবে।"
        )
    )

    val specialOverTwoHours = CheckInMessage(
        title = "অনেকক্ষণ কিছু লেখা হয়নি",
        body = "সমস্যা নেই। শেষ কাজটার নাম দাও, বাকিটা app সামলে নেবে।"
    )

    val specialUntitledBlock = CheckInMessage(
        title = "একটা block-এর নাম বাকি",
        body = "নামটা দিলে দিনের ছবি সম্পূর্ণ হয়।"
    )

    val specialAfterNinePm = CheckInMessage(
        title = "দিনটা কেমন গেল?",
        body = "শেষ কাজটা লিখে দিনটা গুছিয়ে নাও।"
    )

    fun getMessage(
        isFirst: Boolean,
        hasUntitledBlocks: Boolean,
        noLogOverTwoHours: Boolean,
        isAfterNinePm: Boolean,
        lastTitle: String
    ): CheckInMessage {
        // Check special cases first
        val specialCandidates = mutableListOf<CheckInMessage>()

        if (isAfterNinePm && lastTitle != specialAfterNinePm.title) {
            specialCandidates.add(specialAfterNinePm)
        }
        if (hasUntitledBlocks && lastTitle != specialUntitledBlock.title) {
            specialCandidates.add(specialUntitledBlock)
        }
        if (noLogOverTwoHours && lastTitle != specialOverTwoHours.title) {
            specialCandidates.add(specialOverTwoHours)
        }

        if (specialCandidates.isNotEmpty()) {
            return specialCandidates.first()
        }

        // Standard rotation: pick from pool avoiding repetition
        val pool = if (isFirst) firstNotificationPool else followupPool
        val filtered = pool.filter { it.title != lastTitle }
        return if (filtered.isNotEmpty()) {
            filtered.random()
        } else {
            pool.random()
        }
    }
}
