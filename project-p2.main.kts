import khoury.CapturedResult
import khoury.EnabledTest
import khoury.captureResults
import khoury.fileExists
import khoury.fileReadAsList
import khoury.isAnInteger
import khoury.linesToString
import khoury.reactConsole
import khoury.runEnabledTests
import khoury.testSame

// (just useful values for
// the separation characters)
val sepCard = "|"
val sepTag = ","

// a card has a front, back, and associated tags
data class TaggedFlashCard(val front: String, val back: String, val tags: List<String>) {
    fun isTagged(tag: String): Boolean = tags.any { it == tag }

    fun fileFormat(): String = "${front}${sepCard}${back}${sepCard}${tags.joinToString(sepTag)}"
}

val qCB = "What is the capital of Cuba?"
val aCB = "Havana"

val qJP = "What is the capital of Japan?"
val aJP = "Tokyo"

val qMN = "What is the capital of Mongolia?"
val aMN = "Ulaanbaatar"

val qCmaj9 = "What are the notes in Cmaj9?"
val aCmaj9 = "CEGBD"

val tagGeo = "geography"
val tagEasy = "easy"
val tagHard = "hard"
val tagUnused = "unused"

val tfcCB = TaggedFlashCard(qCB, aCB, listOf(tagGeo))
val tfcJP = TaggedFlashCard(qJP, aJP, listOf(tagGeo, tagEasy))
val tfcMN = TaggedFlashCard(qMN, aMN, listOf(tagGeo, tagHard))
val tfcMusic = TaggedFlashCard(qCmaj9, aCmaj9, emptyList<String>())

val tfcListAll = listOf(tfcJP, tfcCB, tfcMN, tfcMusic)
val tfcListCB = listOf(tfcCB)
val tfcListEmpty: List<TaggedFlashCard> = listOf()

// takes the formatted string and produces a corresponding tagged flashcard
fun stringToTaggedFlashCard(fcString: String): TaggedFlashCard {
    val splitString = fcString.split("|")
    return TaggedFlashCard(splitString[0], splitString[1], splitString[2].split(sepTag))
}

@EnabledTest
fun testStringToTaggedFlashCard() {

    testSame(
        stringToTaggedFlashCard("||"),
        TaggedFlashCard("", "", listOf("")),
        "all empty",
    )

    testSame(
        stringToTaggedFlashCard("front|back|"),
        TaggedFlashCard("front", "back", listOf("")),
        "empty tags",
    )

    testSame(
        stringToTaggedFlashCard("front|back|1,2,3"),
        TaggedFlashCard("front", "back", listOf("1", "2", "3")),
        "front back 123",
    )

}

// checks whether the filename as a string is valid and if it is takes the contents
// of the file and makes a corresponding flashcard list
fun readTaggedFlashCardsFile(fileName: String): List<TaggedFlashCard> {
    val fileStrings = fileReadAsList(fileName)
    if (fileExists(fileName)) {
        return fileStrings.map(::stringToTaggedFlashCard)
    } else {
        return listOf()
    }
}

@EnabledTest
fun testReadTaggedFlashCardsFile() {
    val tfcC3 = TaggedFlashCard("c", "3", listOf("hard", "science"))
    val tfcD4 = TaggedFlashCard("d", "4", listOf("hard"))
    testSame(
        readTaggedFlashCardsFile("BADFILE.exe"),
        listOf(),
        "empty tags",
    )

    testSame(
        readTaggedFlashCardsFile("example_tagged.txt"),
        listOf(tfcC3, tfcD4),
        "front back 123",
    )

}

// The deck is either exhausted,
// showing the question, or
// showing the answer
enum class DeckState {
    EXHAUSTED,
    QUESTION,
    ANSWER,
}

// Basic functionality of any deck
interface IDeck {
    // The state of the deck
    fun getState(): DeckState

    // The currently visible text
    // (or null if exhausted)
    fun getText(): String?

    // The number of question/answer pairs
    // (does not change when question are
    // cycled to the end of the deck)
    fun getSize(): Int

    // Shifts from question -> answer
    // (if not QUESTION state, returns the same IDeck)
    fun flip(): IDeck

    // Shifts from answer -> next question (or exhaustion);
    // if the current question was correct it is discarded,
    // otherwise cycled to the end of the deck
    // (if not ANSWER state, returns the same IDeck)
    fun next(correct: Boolean): IDeck
}

// takes in a list of tagged flashcards and a boolean which determines
// whether the flashcard is on the front utilizing the IDeck interface
data class TFCListDeck(val tfcList: List<TaggedFlashCard>, val isFront: Boolean) : IDeck {
    // checks if list is empty and if not checks boolean input and returns
    // appropriate deck state
    override fun getState(): DeckState {
        if (tfcList.isEmpty()) {
            return DeckState.EXHAUSTED
        } else if (isFront) {
            return DeckState.QUESTION
        } else {
            return DeckState.ANSWER
        }
    }

    // checks if list is empty and returns null is empty, and if not checks
    // boolean input and returns appropriate string
    override fun getText(): String? {
        if (tfcList.isEmpty()) {
            return null
        } else if (isFront) {
            return tfcList.first().front
        } else {
            return tfcList.first().back
        }
    }

    // returns list size as integer
    override fun getSize(): Int = tfcList.size

    // if on front of flashcard goes to back and vice versa
    override fun flip(): IDeck {
        return when (isFront) {
            true -> TFCListDeck(tfcList, !isFront)
            false -> TFCListDeck(tfcList, isFront)
        }
    }

    // drops current card and returns next card
    // if user gets question wrong, adds flashcard to back of list
    override fun next(correct: Boolean): IDeck {
        return when (correct) {
            true -> TFCListDeck(tfcList.drop(1), true)
            false -> TFCListDeck(tfcList.drop(1) + tfcList.first(), true)
        }
    }
}

val tfcListDeckFront = TFCListDeck(tfcListAll, true)
val tfcListDeckBack = TFCListDeck(tfcListCB, false)
val tfcListDeckEmpty = TFCListDeck(tfcListEmpty, true)

@EnabledTest
fun testTFCListDeckGetState() {

    testSame(
        tfcListDeckFront.getState(),
        DeckState.QUESTION,
        "question",
    )

    testSame(
        tfcListDeckBack.getState(),
        DeckState.ANSWER,
        "answer",
    )

    testSame(
        tfcListDeckEmpty.getState(),
        DeckState.EXHAUSTED,
        "exhausted",
    )
}

@EnabledTest
fun testTFCListDeckGetText() {

    testSame(
        tfcListDeckFront.getText(),
        qJP,
        "JP front text",
    )

    testSame(
        tfcListDeckBack.getText(),
        aCB,
        "CB back text",
    )

    testSame(
        tfcListDeckEmpty.getText(),
        null,
        "empty & null",
    )
}

@EnabledTest
fun testTFCListDeckGetSize() {

    testSame(
        tfcListDeckFront.getSize(),
        4,
        "3 tfc",
    )

    testSame(
        tfcListDeckBack.getSize(),
        1,
        "1 tfc",
    )

    testSame(
        tfcListDeckEmpty.getSize(),
        0,
        "empty",
    )
}

@EnabledTest
fun testTFCListDeckFlip() {
    testSame(
        TFCListDeck(tfcListCB, true).flip(),
        TFCListDeck(tfcListCB, false),
        "front -> back",
    )

    testSame(
        TFCListDeck(tfcListAll, false).flip(),
        TFCListDeck(tfcListAll, false),
        "back -> back",
    )
}

@EnabledTest
fun testTFCListDeckNext() {
    testSame(
        TFCListDeck(tfcListCB, false).next(true),
        TFCListDeck(tfcListCB.drop(1), true),
        "front -> next back (correct)",
    )

    testSame(
        TFCListDeck(tfcListCB, false).next(false),
        TFCListDeck(tfcListCB.drop(1) + tfcListCB.first(), true),
        "front -> next back (incorrect)",
    )

    testSame(
        TFCListDeck(listOf(tfcJP, tfcCB), false).next(false),
        TFCListDeck(listOf(tfcCB, tfcJP), true),
        "front -> next back (incorrect, list size 2)",
    )
}

// takes in a list of tagged flashcards and a boolean which determines
// whether the flashcard is on the front utilizing the IDeck interface
data class PerfectSquaresDeck(val numInput: Int, val isFront: Boolean, val sequence: List<Int>) : IDeck {
    override fun getState(): DeckState {
        if (sequence.isEmpty()) {
            return DeckState.EXHAUSTED
        } else if (isFront) {
            return DeckState.QUESTION
        } else {
            return DeckState.ANSWER
        }
    }

    override fun getText(): String? {
        if (sequence.isEmpty()) {
            return null
        } else if (isFront) {
            return "$numInput^2 = ?"
        } else {
            return "${numInput * numInput}"
        }
    }

    override fun getSize(): Int = sequence.size

    override fun flip(): IDeck {
        return when (isFront) {
            true -> PerfectSquaresDeck(numInput, !isFront, sequence)
            false -> PerfectSquaresDeck(numInput, isFront, sequence)
        }
    }

    override fun next(correct: Boolean): IDeck {
        return when (correct) {
            true -> PerfectSquaresDeck(numInput - 1, !isFront, sequence.drop(1))
            false -> PerfectSquaresDeck(numInput - 1, !isFront, sequence.drop(1) + listOf(sequence.first()))
        }
    }
}

val perfectSquaresDeckFront3 = PerfectSquaresDeck(3, true, listOf(1, 2, 3))
val perfectSquaresDeckBack3 = PerfectSquaresDeck(3, false, listOf(1, 2, 3))
val perfectSquaresDeckEmpty = PerfectSquaresDeck(0, true, listOf())

@EnabledTest
fun testPerfectSquaredDeckGetState() {

    testSame(
        perfectSquaresDeckFront3.getState(),
        DeckState.QUESTION,
        "question",
    )

    testSame(
        perfectSquaresDeckBack3.getState(),
        DeckState.ANSWER,
        "answer",
    )

    testSame(
        perfectSquaresDeckEmpty.getState(),
        DeckState.EXHAUSTED,
        "exhausted",
    )
}

@EnabledTest
fun testPerfectSquaredDeckGetText() {

    testSame(
        perfectSquaresDeckFront3.getText(),
        "3^2 = ?",
        "3 squared question",
    )

    testSame(
        perfectSquaresDeckBack3.getText(),
        "9",
        "3 squared answer",
    )

    testSame(
        perfectSquaresDeckEmpty.getText(),
        null,
        "empty & null",
    )
}

@EnabledTest
fun testPerfectSquaredDeckGetSize() {

    testSame(
        perfectSquaresDeckFront3.getSize(),
        3,
        "size 3 (front)",
    )

    testSame(
        perfectSquaresDeckBack3.getSize(),
        3,
        "size 3 (back)",
    )

    testSame(
        perfectSquaresDeckEmpty.getSize(),
        0,
        "empty list",
    )
}

@EnabledTest
fun testPerfectSquaredDeckFlip() {
    testSame(
        perfectSquaresDeckFront3.flip(),
        perfectSquaresDeckBack3,
        "front -> back",
    )

    testSame(
        perfectSquaresDeckBack3.flip(),
        perfectSquaresDeckBack3,
        "back -> back",
    )
}

@EnabledTest
fun testPerfectSquaredDeckNext() {
    testSame(
        perfectSquaresDeckBack3.next(true),
        PerfectSquaresDeck(2, true, listOf(2, 3)),
        "correct",
    )

    testSame(
        perfectSquaresDeckBack3.next(false),
        PerfectSquaresDeck(2, true, listOf(2, 3, 1)),
        "incorrect",
    )
}

// the only required capability for a menu option
// is to be able to render a title
interface IMenuOption {
    fun menuTitle(): String
}


// a menu option with a single value and name
data class NamedMenuOption<T>(val option: T, val name: String) : IMenuOption {
    override fun menuTitle(): String = name
}

// individual examples, as well as a list
// (an example for a list of menu options!)
val opt1A = NamedMenuOption(1, "apple")
val opt2B = NamedMenuOption(2, "banana")
val optsExample = listOf(opt1A, opt2B)

// Some useful outputs
val menuPrompt = "Enter your choice (or 0 to quit)"
val menuQuit = "You quit"
val menuChoicePrefix = "You chose: "

// Provides an interactive opportunity for the user to choose
// an option or quit.
fun <T : IMenuOption> chooseMenuOption(options: List<T>): T? {
    // takes the list and formats it into a user-friendly string
    fun renderDeckOptions(ignoredState: Int): String {
        val builder = StringBuilder()
        for (i in options.indices) {
            builder.append("${i + 1}. ${options[i].menuTitle()}\n")
        }
        builder.append("\n$menuPrompt")
        return builder.toString()
    }

    // takes user input and checks if it is a valid interger and a valid
    // input, if not returns -2
    fun transitionOptionChoice(
        ignoredState: Int,
        kbInput: String,
    ): Int {
        if ((isAnInteger(kbInput)) && (kbInput.toInt() in 0..options.size)) {
            val typedInt = kbInput.toInt()
            return typedInt - 1
        } else {
            return -2
        }
    }

    // if user input was valid then returns true, if not returns false
    fun validChoiceEntered(state: Int): Boolean {
        return ((state in options.indices) || (state == -1))
    }

    // if user input was 0 then state is -1 and menu quit is printed
    // if not chosen menu title is shown in a user-friendly way
    fun renderChoice(state: Int): String {
        return when (state == -1) {
            true -> menuQuit
            false -> "${menuChoicePrefix}${options[state].menuTitle()}"
        }
    }

    val chosenOption =
        reactConsole(
            initialState = -2,
            stateToText = ::renderDeckOptions,
            nextState = ::transitionOptionChoice,
            isTerminalState = ::validChoiceEntered,
            terminalStateToText = ::renderChoice,
        )

    return when (chosenOption == -1) {
        true -> null
        false -> options[chosenOption]
    }

    // - call reactConsole (with appropriate handlers)
    // - return the selected option (or null for quit)
}

@EnabledTest
fun testChooseMenuOption() {
    testSame(
        captureResults(
            { chooseMenuOption(listOf(opt1A)) },
            "howdy",
            "0",
        ),
        CapturedResult(
            null,
            "1. ${opt1A.name}",
            "",
            menuPrompt,
            "1. ${opt1A.name}",
            "",
            menuPrompt,
            menuQuit,
        ),
        "quit",
    )

    testSame(
        captureResults(
            { chooseMenuOption(optsExample) },
            "hello",
            "10",
            "-3",
            "1",
        ),
        CapturedResult(
            opt1A,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "${menuChoicePrefix}${opt1A.name}",
        ),
        "1",
    )

    testSame(
        captureResults(
            { chooseMenuOption(optsExample) },
            "3",
            "-1",
            "2",
        ),
        CapturedResult(
            opt2B,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "1. ${opt1A.name}", "2. ${opt2B.name}", "", menuPrompt,
            "${menuChoicePrefix}${opt2B.name}",
        ),
        "2",
    )
}


typealias PositivityClassifier = (String) -> Boolean

data class LabeledExample<E, L>(val example: E, val label: L)

// dataset for computer

val datasetYN: List<LabeledExample<String, Boolean>> =
    listOf(
        LabeledExample("yes", true),
        LabeledExample("y", true),
        LabeledExample("indeed", true),
        LabeledExample("aye", true),
        LabeledExample("oh yes", true),
        LabeledExample("affirmative", true),
        LabeledExample("roger", true),
        LabeledExample("uh huh", true),
        LabeledExample("true", true),
        // just a visual separation of
        // the positive/negative examples
        LabeledExample("no", false),
        LabeledExample("n", false),
        LabeledExample("nope", false),
        LabeledExample("negative", false),
        LabeledExample("nay", false),
        LabeledExample("negatory", false),
        LabeledExample("uh uh", false),
        LabeledExample("absolutely not", false),
        LabeledExample("false", false),
    )

// // Heuristically determines if the supplied string
// // is positive based upon the first letter being Y
fun isPositiveSimple(s: String): Boolean {
    return s.uppercase().startsWith("Y")
}

// // tests that an element of the dataset matches
// // with expectation of its correctness on a
// // particular classifier
fun helpTestElement(
    index: Int,
    expectedIsCorrect: Boolean,
    isPos: PositivityClassifier,
) {
    testSame(
        isPos(datasetYN[index].example),
        when (expectedIsCorrect) {
            true -> datasetYN[index].label
            false -> !datasetYN[index].label
        },
        when (expectedIsCorrect) {
            true -> datasetYN[index].example
            false -> "${ datasetYN[index].example } <- WRONG"
        },
    )
}

@EnabledTest
fun testIsPositiveSimple() {
    val classifier = ::isPositiveSimple

    // correctly responds with positive
    for (i in 0..1) {
        helpTestElement(i, true, classifier)
    }

    // incorrectly responds with negative
    for (i in 2..8) {
        helpTestElement(i, false, classifier)
    }

    // correctly responds with negative, sometimes
    // due to luck (i.e., anything not starting
    // with the letter Y is assumed negative)
    for (i in 9..17) {
        helpTestElement(i, true, classifier)
    }
}

typealias EvaluationFunction<T> = (T) -> Int

// produces (up to) the top-k items in the supplied
// list according to the supplied evaluation function
fun <T> topK(
    possibilities: List<T>,
    k: Int,
    evalFunc: EvaluationFunction<T>,
): List<T> {
    val evalScores =
        possibilities.map {
            evalFunc(it)
        }

    val itemWithScores =
        possibilities.zip(evalScores)

    val sortedByEval =
        itemWithScores.sortedByDescending {
            it.second
        }

    val sortedWithoutScores =
        sortedByEval.map {
            it.first
        }

    return sortedWithoutScores.take(k)
}

@EnabledTest
fun testTopK() {
    val emptyInts: List<Int> = listOf()

    testSame(
        topK(emptyInts, 5, { it }),
        emptyInts,
        "empty",
    )

    testSame(
        topK(listOf(9, 8, 1, 3, 2, 0), 5, { -it }),
        listOf(0, 1, 2, 3, 8),
        "sort by low",
    )

    testSame(
        topK(listOf("abc", "abcd", "a", "ab"), 3, { it.length }),
        listOf("abcd", "abc", "ab"),
        "sort by char len",
    )

}

// calculates the minimum number of single-character edits
// between the supplied strings
fun levenshteinDistance(
    a: String,
    b: String,
): Int {
    // shorthand for producing all the letters of
    // a string except the first
    fun tail(s: String): String = s.drop(1)

    val lev = ::levenshteinDistance

    return when {
        b.isEmpty() -> a.length
        a.isEmpty() -> b.length
        a[0] == b[0] -> lev(tail(a), tail(b))
        else ->
            1 +
                minOf(
                    lev(tail(a), b),
                    lev(a, tail(b)),
                    lev(tail(a), tail(b)),
                )
    }
}

@EnabledTest
fun testLevenshteinDistance() {
    testSame(
        levenshteinDistance("", "howdy"),
        5,
        "'', 'howdy'",
    )

    testSame(
        levenshteinDistance("howdy", ""),
        5,
        "'howdy', ''",
    )

    testSame(
        levenshteinDistance("howdy", "howdy"),
        0,
        "'howdy', 'howdy'",
    )

    testSame(
        levenshteinDistance("kitten", "sitting"),
        3,
        "'kitten', 'sitting'",
    )

    testSame(
        levenshteinDistance("sitting", "kitten"),
        3,
        "'sitting', 'kitten'",
    )
}


typealias DistanceFunction<T> = (T, T) -> Int


data class ResultWithVotes<L>(val label: L, val votes: Int)

// // uses k-nearest-neighbor (kNN) to predict the label
// // for a supplied example given a labeled dataset
// // and distance function
fun <E, L> nnLabel(
    queryExample: E,
    dataset: List<LabeledExample<E, L>>,
    distFunc: DistanceFunction<E>,
    k: Int,
): ResultWithVotes<L> {

    val closestK =
        topK(dataset, k) {
            -distFunc(queryExample, it.example)
        }

    val closestKLabels = closestK.map { it.label }

    val labelsWithCounts =
        closestKLabels.distinct().map {
                label ->
            Pair(
                label,
                closestKLabels.filter({ it == label }).size,
            )
        }

    val topLabelWithCount = topK(labelsWithCounts, 1, { it.second })[0]

    return ResultWithVotes(
        topLabelWithCount.first,
        topLabelWithCount.second,
    )
}

@EnabledTest
fun testNNLabel() {
    val dataset =
        listOf(
            LabeledExample(2, "a"),
            LabeledExample(3, "a"),
            LabeledExample(7, "b"),
            LabeledExample(10, "b"),
        )

    fun myAbsVal(
        a: Int,
        b: Int,
    ): Int {
        val diff = a - b

        return when (diff >= 0) {
            true -> diff
            false -> -diff
        }
    }
}


// we'll generally use k=3 in our classifier
val classifierK = 3

// takes in a string and classifies it as yes or no where true
// is yes and false is no
fun yesNoClassifier(s: String): ResultWithVotes<Boolean> {
    val lowercased = s.lowercase()

    for (el in datasetYN) {
        if (lowercased == el.example) {
            return ResultWithVotes(el.label, classifierK)
        }
    }

    return nnLabel(s, datasetYN, ::levenshteinDistance, classifierK)
}

@EnabledTest
fun testYesNoClassifier() {
    testSame(
        yesNoClassifier("YES"),
        ResultWithVotes(true, 3),
        "YES: 3/3",
    )

    testSame(
        yesNoClassifier("no"),
        ResultWithVotes(false, 3),
        "no: 3/3",
    )

    testSame(
        yesNoClassifier("nadda"),
        ResultWithVotes(false, 2),
        "nadda: 2/3",
    )

    testSame(
        yesNoClassifier("yerp"),
        ResultWithVotes(true, 3),
        "yerp: 3/3",
    )

    testSame(
        yesNoClassifier("ouch"),
        ResultWithVotes(true, 3),
        "ouch: 3/3",
    )

    testSame(
        yesNoClassifier("now"),
        ResultWithVotes(false, 3),
        "now 3/3",
    )
}

fun isPositiveML(s: String): Boolean = yesNoClassifier(s).label

@EnabledTest
fun testIsPositiveML() {
    for (i in 0..8) {
        helpTestElement(i, true, ::isPositiveML)
    }

    for (i in 9..17) {
        helpTestElement(i, true, ::isPositiveML)
    }
}


// represents the result of a study session:
// how many questions were originally in the deck,
// how many total attempts were required to get
// them all correct!
data class StudyDeckResult(val numQuestions: Int, val numAttempts: Int)


// Some useful prompts
val studyThink = "Think of the result? Press enter to continue"
val studyCheck = "Correct? (Y)es/(N)o"

// state that utilizes the IDeck interface and keeps track of number
// of self-reported incorrect answers
data class StudyState(val deckInterface: IDeck, val numWrong: Int)

// if study state is front returns front string and vice versa
fun renderToText(studyState: StudyState): String {
    return when (studyState.deckInterface.getState()) {
        DeckState.QUESTION -> "${studyState.deckInterface.getText()}\n$studyThink"
        DeckState.ANSWER -> "${studyState.deckInterface.getText()}\n$studyCheck"
        DeckState.EXHAUSTED -> ""
    }
}

@EnabledTest
fun testRenderToText() {
    testSame(
        renderToText(StudyState(tfcListDeckFront, 0)),
        "$qJP\n$studyThink",
        "question JP",
    )

    testSame(
        renderToText(StudyState(tfcListDeckBack, 0)),
        "$aCB\n$studyCheck",
        "answer CB",
    )

    testSame(
        renderToText(StudyState(TFCListDeck(tfcListAll.drop(1), true), 0)),
        "$qCB\n$studyThink",
        "question CB",
    )

    testSame(
        renderToText(StudyState(TFCListDeck(tfcListAll.drop(2), false), 1)),
        "$aMN\n$studyCheck",
        "answer MN",
    )
}

// if study state is front returns back, if on back checks user input and
// appropriately adds to num wrong and transitions to next cards front
fun transitionStudyState(
    studyState: StudyState,
    userInput: String,
): StudyState {
    if ((studyState.deckInterface.getState() == DeckState.QUESTION)) {
        return StudyState(studyState.deckInterface.flip(), studyState.numWrong)
    } else if ((studyState.deckInterface.getState() == DeckState.ANSWER) && (isPositiveML(userInput))) {
        return StudyState(studyState.deckInterface.next(true), studyState.numWrong)
    } else {
        return StudyState(studyState.deckInterface.next(false), studyState.numWrong + 1)
    }
}

@EnabledTest
fun testTransitionStudyState() {
    testSame(
        transitionStudyState(StudyState(TFCListDeck(tfcListAll, true), 0), ""),
        StudyState(TFCListDeck(tfcListAll, false), 0),
        "front -> back",
    )

    testSame(
        transitionStudyState(StudyState(TFCListDeck(tfcListAll, false), 0), "yes"),
        StudyState(TFCListDeck(tfcListAll.drop(1), true), 0),
        "back -> next front, correct",
    )

    testSame(
        transitionStudyState(StudyState(TFCListDeck(listOf(tfcJP, tfcCB), false), 0), "nope"),
        StudyState(TFCListDeck(listOf(tfcCB, tfcJP), true), 1),
        "back -> next front, incorrect",
    )
}

// checks list size and if we should terminate react console
fun isListEmpty(studyState: StudyState): Boolean = studyState.deckInterface.getSize() == 0

@EnabledTest
fun testIsListEmpty() {
    testSame(
        isListEmpty(StudyState(TFCListDeck(tfcListAll, false), 0)),
        false,
        "do not terminate",
    )

    testSame(
        isListEmpty(StudyState(TFCListDeck(tfcListEmpty, true), 3)),
        true,
        "terminate",
    )
}

// takes in the IDeck interface, uses reactConsole to study through the deck
// and returns StudyDeckResult, a summary of total questions and total attempts
fun studyDeck2(
    deck: IDeck,
    classifier: PositivityClassifier,
): StudyDeckResult {
    val numWrong =
        reactConsole(
            initialState = StudyState(deck, 0),
            stateToText = ::renderToText,
            nextState = ::transitionStudyState,
            isTerminalState = ::isListEmpty,
        ).numWrong

    println("Questions: ${deck.getSize()}, Attempts: ${deck.getSize() + numWrong}")

    return StudyDeckResult(deck.getSize(), deck.getSize() + numWrong)
}

@EnabledTest
fun testStudyDeck2() {
    // makes a captureResults-friendly function :)
    fun helpTest(chosenDeck: TFCListDeck): () -> StudyDeckResult {
        fun studiedDeck2(): StudyDeckResult {
            return studyDeck2(chosenDeck, ::isPositiveSimple)
        }
        return ::studiedDeck2
    }

    val deckAllExample = TFCListDeck(tfcListAll, true)
    val deckOfOneExample = TFCListDeck(tfcListCB, true)

    testSame(
        captureResults(
            helpTest(deckAllExample),
            "",
            "yes",
            "",
            "yeah",
            "",
            "yup",
            "",
            "affirmative",
        ),
        CapturedResult(
            StudyDeckResult(4, 4),
            qJP,
            studyThink,
            aJP,
            studyCheck,
            qCB,
            studyThink,
            aCB,
            studyCheck,
            qMN,
            studyThink,
            aMN,
            studyCheck,
            qCmaj9,
            studyThink,
            aCmaj9,
            studyCheck,
            "",
            "Questions: 4, Attempts: 4"
        ),
        "all correct",
    )

    testSame(
        captureResults(
            helpTest(deckAllExample),
            "",
            "nope",
            "aposidhasjdn (ignore)",
            "yeah",
            "",
            "yup",
            "",
            "affirmative",
            "",
            "yes",
        ),
        CapturedResult(
            StudyDeckResult(4, 5),
            qJP,
            studyThink,
            aJP,
            studyCheck,
            qCB,
            studyThink,
            aCB,
            studyCheck,
            qMN,
            studyThink,
            aMN,
            studyCheck,
            qCmaj9,
            studyThink,
            aCmaj9,
            studyCheck,
            qJP,
            studyThink,
            aJP,
            studyCheck,
            "",
            "Questions: 4, Attempts: 5"
        ),
        "cycle JP flashcard",
    )

    testSame(
        captureResults(
            helpTest(deckOfOneExample),
            "",
            "no",
            "",
            "nope",
            "",
            "nahh",
            "",
            "yess",
        ),
        CapturedResult(
            StudyDeckResult(1, 4),
            qCB,
            studyThink,
            aCB,
            studyCheck,
            qCB,
            studyThink,
            aCB,
            studyCheck,
            qCB,
            studyThink,
            aCB,
            studyCheck,
            qCB,
            studyThink,
            aCB,
            studyCheck,
            "",
            "Questions: 1, Attempts: 4"
        ),
        "cycle cuba thrice (4 attempts)",
    )
}


// some useful labels
val optSimple = "Simple Self-Report Evaluation"
val optML = "ML Self-Report Evaluation"

// lets the user choose a deck and study it, cycles through
// list of deck until quits
fun studyCycle() {
    val sentimentAnalyzers =
        listOf(
            NamedMenuOption(::isPositiveSimple, optSimple),
            NamedMenuOption(::isPositiveML, optML)
        )

    val fileDeck = readTaggedFlashCardsFile("example_tagged.txt")
    val geographyDeck = TFCListDeck(tfcListAll.filter { it.tags.contains(tagGeo) }, true)

    val deckOptions = mutableListOf<NamedMenuOption<IDeck>>()
    if (fileDeck.isNotEmpty()) {
        deckOptions.add(NamedMenuOption(TFCListDeck(fileDeck, true), "File Deck"))
    }
    deckOptions.add(NamedMenuOption(perfectSquaresDeckFront3, "Squared Deck (3)"))
    if (geographyDeck.tfcList.isNotEmpty()) {
        deckOptions.add(NamedMenuOption(geographyDeck, "Geography Deck"))
    }

    fun studySession(): Boolean {
        if (deckOptions.isEmpty()) {
            println("No decks available.")
            return false
        }

        val chosenDeck = chooseMenuOption(deckOptions)
        if (chosenDeck == null) return false

        val chosenAnalysis = chooseMenuOption(sentimentAnalyzers)
        if (chosenAnalysis == null) return false

        studyDeck2(chosenDeck.option, chosenAnalysis.option)
        return true
    }

    while (studySession()) {}
}


fun main() {
    studyCycle()
}

runEnabledTests(this)
main()

