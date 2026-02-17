package com.maxli.coursegpa

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maxli.coursegpa.ui.theme.CourseTheme
import androidx.compose.ui.graphics.Color

private val validGrades = setOf(
    "A", "A-",
    "B+", "B", "B-",
    "C+", "C", "C-",
    "D+", "D", "D-",
    "F"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CourseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val owner = LocalViewModelStoreOwner.current
                    owner?.let {
                        val viewModel: MainViewModel = viewModel(
                            it,
                            "MainViewModel",
                            MainViewModelFactory(
                                LocalContext.current.applicationContext as Application
                            )
                        )
                        TabScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun TabScreen(viewModel: MainViewModel) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Acad", "Trivial")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(text = { Text(title) },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index }
                )
            }
        }
        when (tabIndex) {
            0 -> ScreenSetup(viewModel)
            1 -> TrivialScreen(viewModel)
        }
    }
}

@Composable
fun TrivialScreen(viewModel: MainViewModel) {
    val allQuestions by viewModel.allTrivialQuestions.observeAsState(listOf())
    var numberOfQuestions by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf<List<TrivialQuestion>>(emptyList()) }
    val (selectedAnswers, setSelectedAnswers) = remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var score by remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row {
            Button(onClick = { viewModel.loadTriviaQuestions() }) {
                Text("Load")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val num = numberOfQuestions.toIntOrNull()
                if (num != null && num > 0 && num <= allQuestions.size) {
                    questions = allQuestions.shuffled().take(num)
                    setSelectedAnswers(emptyMap())
                    score = null
                }
            }) {
                Text("Go")
            }
        }

        CustomTextField(
            title = "Number of Questions",
            textState = numberOfQuestions,
            onTextChange = { numberOfQuestions = it },
            keyboardType = KeyboardType.Number
        )

        LazyColumn {
            items(questions) { question ->
                QuestionCard(
                    question = question,
                    selectedAnswer = selectedAnswers[question.id],
                    onAnswerSelected = {
                        val newAnswers = selectedAnswers.toMutableMap()
                        newAnswers[question.id] = it
                        setSelectedAnswers(newAnswers)
                    }
                )
            }
        }

        Button(
            onClick = {
                var correct = 0
                questions.forEach { question ->
                    if (selectedAnswers[question.id] == question.correctAnswer) {
                        correct++
                    }
                }
                score = "$correct/${questions.size}"
            },
            enabled = selectedAnswers.size == questions.size && questions.isNotEmpty()
        ) {
            Text("Grade")
        }

        score?.let {
            Text("Your score: $it", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun QuestionCard(question: TrivialQuestion, selectedAnswer: String?, onAnswerSelected: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = question.questionName, fontWeight = FontWeight.Bold)
            val choices = listOf("A", "B", "C", "D")
            val choiceText = listOf(question.choiceA, question.choiceB, question.choiceC, question.choiceD)

            choices.forEachIndexed { index, choice ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (selectedAnswer == choice),
                            onClick = { onAnswerSelected(choice) }
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (selectedAnswer == choice),
                        onClick = { onAnswerSelected(choice) }
                    )
                    Text(text = "$choice. ${choiceText[index]}")
                }
            }
        }
    }
}


@Composable
fun ScreenSetup(viewModel: MainViewModel) {
    val allCourses by viewModel.allCourses.observeAsState(listOf())
    val searchResults by viewModel.searchResults.observeAsState(listOf())

    MainScreen(
        allCourses = allCourses,
        searchResults = searchResults,
        viewModel = viewModel
    )
}

@Composable
fun MainScreen(
    allCourses: List<Course>,
    searchResults: List<Course>,
    viewModel: MainViewModel
) {
    var courseName by remember { mutableStateOf("") }
    var courseCreditHour by remember { mutableStateOf("") }
    var letterGrade by remember { mutableStateOf("") }

    var calculatedGPA by remember { mutableDoubleStateOf(-1.0) }
    var searching by remember { mutableStateOf(false) }

    val isGradeValid = letterGrade.isNotEmpty() &&
            letterGrade.uppercase() in validGrades

    val navyButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary
    )
    val orangeButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiary,
        contentColor = MaterialTheme.colorScheme.onTertiary
    )

    Column(
        horizontalAlignment = CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        HeaderSection()

        CustomTextField(
            title = "Course Name",
            textState = courseName,
            onTextChange = { courseName = it },
            keyboardType = KeyboardType.Text
        )

        CustomTextField(
            title = "Credit Hour",
            textState = courseCreditHour,
            onTextChange = { courseCreditHour = it },
            keyboardType = KeyboardType.Number
        )

        CustomTextField(
            title = "Letter Grade",
            textState = letterGrade,
            onTextChange = { letterGrade = it.trim().uppercase() },
            keyboardType = KeyboardType.Text,
            isError = letterGrade.isNotEmpty() && !isGradeValid,
            errorMessage = "Valid grades: A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Button(
                    onClick = {
                        if (courseCreditHour.isNotEmpty() && isGradeValid) {
                            viewModel.insertCourse(
                                Course(
                                    courseName,
                                    courseCreditHour.toInt(),
                                    letterGrade
                                )
                            )
                            searching = false
                        }
                    },
                    colors = navyButtonColors,
                    enabled = isGradeValid
                ) { Text("Add") }

                Button(
                    onClick = {
                        searching = true
                        viewModel.findCourse(courseName)
                    },
                    colors = navyButtonColors
                ) { Text("Sch") }

                Button(
                    onClick = {
                        searching = false
                        viewModel.deleteCourse(courseName)
                    },
                    colors = navyButtonColors
                ) { Text("Del") }

                Button(
                    onClick = {
                        searching = false
                        courseName = ""
                        courseCreditHour = ""
                        letterGrade = ""
                    },
                    colors = navyButtonColors
                ) { Text("Clr") }

                Button(
                    onClick = {
                        calculatedGPA = calculateGPA2(allCourses)
                    },
                    colors = orangeButtonColors
                ) { Text("GPA") }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Current GPA", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (calculatedGPA < 0) "--" else "%.2f".format(calculatedGPA),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            val list = if (searching) searchResults else allCourses

            item {
                TitleRow("ID", "Course", "Credit", "Grade")
            }

            items(list) { course ->
                CourseRow(
                    id = course.id,
                    name = course.courseName,
                    creditHour = course.creditHour,
                    letterGrade = course.letterGrade
                )
            }
        }
    }
}

@Composable
private fun HeaderSection() {
    Image(
        painter = painterResource(id = R.drawable.rwulogo),
        contentDescription = "Header Image",
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentScale = ContentScale.Crop
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Course GPA Tracker",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }

    Divider(
        color = MaterialTheme.colorScheme.tertiary,
        thickness = 3.dp
    )
}

// ===== GPA calculation =====
private fun calculateGPA2(courses: List<Course>): Double {
    val gradePoints = mapOf(
        "A" to 4.0, "A-" to 3.67,
        "B+" to 3.33, "B" to 3.0, "B-" to 2.67,
        "C+" to 2.33, "C" to 2.0, "C-" to 1.67,
        "D+" to 1.33, "D" to 1.0, "D-" to 0.67,
        "F" to 0.0
    )

    val totalCreditHours = courses.sumOf { it.creditHour }
    if (totalCreditHours == 0) return 0.0

    val totalPoints = courses.sumOf {
        it.creditHour * (gradePoints[it.letterGrade.uppercase()] ?: 0.0)
    }

    return totalPoints / totalCreditHours
}

@Composable
fun TitleRow(head1: String, head2: String, head3: String, head4: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.secondary)
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp)
        ) {
            val color = MaterialTheme.colorScheme.onSecondary
            Text(head1, modifier = Modifier.weight(0.12f), color = color, fontWeight = FontWeight.Bold)
            Text(head2, modifier = Modifier.weight(0.38f), color = color, fontWeight = FontWeight.Bold)
            Text(head3, modifier = Modifier.weight(0.20f), color = color, fontWeight = FontWeight.Bold)
            Text(head4, modifier = Modifier.weight(0.20f), color = color, fontWeight = FontWeight.Bold)
        }

        Divider(color = MaterialTheme.colorScheme.tertiary, thickness = 2.dp)
    }
}

@Composable
fun CourseRow(id: Int, name: String, creditHour: Int, letterGrade: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Text(id.toString(), modifier = Modifier.weight(0.12f))
            Text(name, modifier = Modifier.weight(0.38f))
            Text(creditHour.toString(), modifier = Modifier.weight(0.20f))
            Text(
                letterGrade,
                modifier = Modifier.weight(0.20f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    title: String,
    textState: String,
    onTextChange: (String) -> Unit,
    keyboardType: KeyboardType,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    Column {
        OutlinedTextField(
            value = textState,
            onValueChange = onTextChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            label = { Text(title) },
            isError = isError,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = Color.White,
                unfocusedLabelColor = MaterialTheme.colorScheme.secondary
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

class MainViewModelFactory(val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MainViewModel(application) as T
    }
}
