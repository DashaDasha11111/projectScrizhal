package com.example.scrizhal

import java.time.LocalDate

object Awards {
    data class AwardRow(
        val year: Int,
        val title: String,
        val status: String
    )

    data class AwardMilestone(
        val title: String,
        val date: LocalDate
    )

    // 2.3.1 (очередные награды пресвитеров)
    private val ladderPriest = listOf(
        "Набедренник",
        "Камилавка",
        "Наперсный крест (золотой)",
        "Палица",
        "Крест с украшениями",
        "Протоиерей",
        "Литургия с отверстыми вратами (Иже Херувимы)",
        "Литургия с отверстыми вратами (Отче наш)",
        "Митра",
        "Второй крест с украшениями"
    )

    // 2.3.11: для монашествующих вместо митры — архимандрит; камилавки/протоиерея нет.
    private val ladderMonk = listOf(
        "Набедренник",
        "Наперсный крест (золотой)",
        "Палица",
        "Крест с украшениями",
        "Литургия с отверстыми вратами (Иже Херувимы)",
        "Литургия с отверстыми вратами (Отче наш)",
        "Архимандрит",
        "Второй крест с украшениями"
    )

    fun ladder(isMonk: Boolean): List<String> = if (isMonk) ladderMonk else ladderPriest

    fun normalizedAwardName(raw: String): String {
        return when (raw.trim()) {
            "Золотой крест" -> "Наперсный крест (золотой)"
            "Наперсный крест" -> "Наперсный крест (золотой)"
            "Наперсный крест золотого цвета" -> "Наперсный крест (золотой)"
            "Служение с отв. вратами" -> "Литургия с отверстыми вратами (Иже Херувимы)"
            "Служение литургии" -> "Литургия с отверстыми вратами (Отче наш)"
            else -> raw.trim()
        }
    }

    private fun maxOf(a: LocalDate, b: LocalDate): LocalDate = if (a.isAfter(b)) a else b

    private fun serviceMinYearsFromOrdination(title: String, isMonk: Boolean): Int? {
        // Минимальная выслуга (годы) "должна составлять не менее ..."
        return when (title) {
            "Палица" -> 15
            "Крест с украшениями" -> 20
            "Протоиерей" -> if (isMonk) null else 25
            "Митра" -> if (isMonk) null else 40
            "Архимандрит" -> if (isMonk) 40 else null
            "Второй крест с украшениями" -> 50
            else -> null
        }
    }

    private fun gapYearsAfter(prevTitle: String, nextTitle: String, isMonk: Boolean): Int {
        // Интервалы "не ранее чем через ... лет после ..." (в годах)
        return when (nextTitle) {
            "Набедренник" -> if (isMonk) 5 else 3 // после хиротонии
            "Камилавка" -> 3 // после набедренника (у монашествующих ступени нет)
            "Наперсный крест (золотой)" -> if (isMonk) 5 else 4
            "Палица" -> 5
            "Крест с украшениями" -> 5
            "Протоиерей" -> 5
            "Литургия с отверстыми вратами (Иже Херувимы)" -> if (isMonk) 10 else 5
            "Литургия с отверстыми вратами (Отче наш)" -> 5
            "Митра" -> 5
            "Архимандрит" -> 5
            "Второй крест с украшениями" -> 10
            else -> 5
        }
    }

    private fun computeNextDateFromPrev(
        isMonk: Boolean,
        ordinationDate: LocalDate,
        prevTitle: String,
        prevDate: LocalDate,
        nextTitle: String
    ): LocalDate {
        val byGap = prevDate.plusYears(gapYearsAfter(prevTitle, nextTitle, isMonk).toLong())
        val byService = serviceMinYearsFromOrdination(nextTitle, isMonk)?.let { ordinationDate.plusYears(it.toLong()) }
        return if (byService != null) maxOf(byGap, byService) else byGap
    }

    /**
     * Автоматический расчет минимально возможных дат награждения по вашей логике (2.3.3–2.3.12).
     * Возвращает "план" дат наград, начиная от хиротонии в пресвитера.
     */
    fun buildTimeline(isMonk: Boolean, ordinationDate: LocalDate): List<AwardMilestone> {
        val milestones = mutableListOf<AwardMilestone>()
        fun add(title: String, date: LocalDate) {
            milestones += AwardMilestone(title, date)
        }

        val start = ordinationDate

        if (!isMonk) {
            // 2.3.3 Набедренник: >= 3 лет после хиротонии
            val nab = start.plusYears(3)
            add("Набедренник", nab)

            // 2.3.4 Камилавка: >= 3 лет после набедренника
            val kam = nab.plusYears(3)
            add("Камилавка", kam)

            // 2.3.5 Крест (золото): >= 4 лет после камилавки
            val crossGold = kam.plusYears(4)
            add("Наперсный крест (золотой)", crossGold)

            // 2.3.6 Палица: >= 5 лет после креста (золото) и выслуга >= 15 лет
            val pal = maxOf(crossGold.plusYears(5), start.plusYears(15))
            add("Палица", pal)

            // 2.3.7 Крест с украшениями: >= 5 лет после палицы и выслуга >= 20 лет
            val deco = maxOf(pal.plusYears(5), start.plusYears(20))
            add("Крест с украшениями", deco)

            // 2.3.8 Протоиерей: >= 5 лет после креста с украшениями и выслуга >= 25 лет
            val proto = maxOf(deco.plusYears(5), start.plusYears(25))
            add("Протоиерей", proto)

            // 2.3.9 "Иже Херувимы": >= 5 лет после протоиерея
            val cher = proto.plusYears(5)
            add("Литургия с отверстыми вратами (Иже Херувимы)", cher)

            // 2.3.10 "Отче наш": >= 5 лет после "Иже Херувимы"
            val otche = cher.plusYears(5)
            add("Литургия с отверстыми вратами (Отче наш)", otche)

            // 2.3.11 Митра: >= 5 лет после "Отче наш" и выслуга >= 40 лет
            val mitra = maxOf(otche.plusYears(5), start.plusYears(40))
            add("Митра", mitra)

            // 2.3.12 Второй крест: >= 10 лет после митры и выслуга >= 50 лет
            val second = maxOf(mitra.plusYears(10), start.plusYears(50))
            add("Второй крест с украшениями", second)
        } else {
            // 2.3.3 Набедренник: >= 5 лет после хиротонии
            val nab = start.plusYears(5)
            add("Набедренник", nab)

            // 2.3.5 Крест (золото): >= 5 лет после набедренника
            val crossGold = nab.plusYears(5)
            add("Наперсный крест (золотой)", crossGold)

            // 2.3.6 Палица: >= 5 лет после креста (золото) и выслуга >= 15 лет
            val pal = maxOf(crossGold.plusYears(5), start.plusYears(15))
            add("Палица", pal)

            // 2.3.7 Крест с украшениями: >= 5 лет после палицы и выслуга >= 20 лет
            val deco = maxOf(pal.plusYears(5), start.plusYears(20))
            add("Крест с украшениями", deco)

            // 2.3.9 для монашествующих: >= 10 лет после креста с украшениями
            val cher = deco.plusYears(10)
            add("Литургия с отверстыми вратами (Иже Херувимы)", cher)

            // 2.3.10 "Отче наш": >= 5 лет после "Иже Херувимы"
            val otche = cher.plusYears(5)
            add("Литургия с отверстыми вратами (Отче наш)", otche)

            // 2.3.11 Архимандрит: >= 5 лет после "Отче наш" и выслуга >= 40 лет
            val arch = maxOf(otche.plusYears(5), start.plusYears(40))
            add("Архимандрит", arch)

            // 2.3.12 Второй крест: >= 10 лет после архимандрита и выслуга >= 50 лет
            val second = maxOf(arch.plusYears(10), start.plusYears(50))
            add("Второй крест с украшениями", second)
        }

        return milestones
    }

    data class NextAward(
        val title: String,
        val date: LocalDate
    )

    /**
     * Пересчитывает "следующую награду" от хиротонии и фактически введенных наград.
     * - history: список (title,date) ранее полученных наград.
     * - если награда в history стоит позже минимально допустимой даты, берем фактическую дату как опорную.
     */
    fun computeNextFromHistory(
        isMonk: Boolean,
        ordinationDate: LocalDate,
        history: List<Pair<String, LocalDate>>
    ): NextAward? {
        val ladder = ladder(isMonk)
        if (ladder.isEmpty()) return null

        val receivedMap = history.associate { normalizedAwardName(it.first) to it.second }

        // Стартовая точка: "хиротония" как prevDate
        var prevTitle = "Хиротония"
        var prevDate = ordinationDate

        for (title in ladder) {
            val normalizedTitle = normalizedAwardName(title)
            val minDate = if (prevTitle == "Хиротония") {
                // для первой ступени: gapYearsAfter(... "Набедренник" ...) уже учитывает хиротонию
                prevDate.plusYears(gapYearsAfter(prevTitle, normalizedTitle, isMonk).toLong())
            } else {
                computeNextDateFromPrev(isMonk, ordinationDate, prevTitle, prevDate, normalizedTitle)
            }

            val actual = receivedMap[normalizedTitle]
            if (actual != null) {
                // награда уже получена, продвигаемся дальше (опорная дата = max(minDate, actual))
                prevTitle = normalizedTitle
                prevDate = maxOf(minDate, actual)
            } else {
                // следующая награда
                return NextAward(normalizedTitle, minDate)
            }
        }

        return null
    }

    fun receivedFromHistory(
        isMonk: Boolean,
        history: List<Pair<String, LocalDate>>
    ): List<Pair<String, Int>> {
        val ladder = ladder(isMonk)
        val receivedMap = history.associate { normalizedAwardName(it.first) to it.second }
        return ladder.mapNotNull { title ->
            val n = normalizedAwardName(title)
            val d = receivedMap[n] ?: return@mapNotNull null
            n to d.year
        }
    }

    fun forecastFromNext(
        isMonk: Boolean,
        ordinationDate: LocalDate,
        next: NextAward,
        maxRows: Int
    ): List<AwardRow> {
        val ladder = ladder(isMonk)
        val startIdx = ladder.indexOf(next.title).takeIf { it >= 0 } ?: return emptyList()

        val today = LocalDate.now()
        val rows = mutableListOf<AwardRow>()

        var prevTitle = next.title
        var prevDate = next.date
        rows += AwardRow(
            year = prevDate.year,
            title = prevTitle,
            status = if (!prevDate.isAfter(today)) "Готов" else "Ожидание"
        )

        var i = startIdx + 1
        while (rows.size < maxRows && i < ladder.size) {
            val title = ladder[i]
            val date = computeNextDateFromPrev(isMonk, ordinationDate, prevTitle, prevDate, title)
            rows += AwardRow(year = date.year, title = title, status = "Ожидание")
            prevTitle = title
            prevDate = date
            i++
        }

        return rows
    }

    fun receivedAwards(
        nextAwardRaw: String,
        isMonk: Boolean,
        ordinationDate: LocalDate?,
        nextAwardDate: LocalDate?
    ): List<Pair<String, Int>> {
        val ord = ordinationDate ?: return emptyList()
        val timeline = buildTimeline(isMonk, ord)
        val nextAward = normalizedAwardName(nextAwardRaw)
        val cutoff = nextAwardDate ?: timeline.firstOrNull { it.title == nextAward }?.date ?: LocalDate.now()
        return timeline
            .takeWhile { it.date.isBefore(cutoff) }
            .map { it.title to it.date.year }
    }

    fun forecast(
        nextAwardRaw: String,
        isMonk: Boolean,
        ordinationDate: LocalDate?,
        nextAwardDate: LocalDate,
        maxRows: Int = 4
    ): List<AwardRow> {
        val today = LocalDate.now()
        val nextAward = normalizedAwardName(nextAwardRaw)

        val ord = ordinationDate ?: run {
            val l = ladder(isMonk)
            val startIdx = l.indexOf(nextAward).takeIf { it >= 0 } ?: 0
            return (startIdx until l.size).take(maxRows).mapIndexed { idx, i ->
                val year = nextAwardDate.year + idx * 5
                val status = if (idx == 0 && !nextAwardDate.isAfter(today)) "Готов" else "Ожидание"
                AwardRow(year = year, title = l[i], status = status)
            }
        }

        return forecastFromNext(
            isMonk = isMonk,
            ordinationDate = ord,
            next = NextAward(nextAward, nextAwardDate),
            maxRows = maxRows
        )
    }
}

