package com.maxli.coursegpa

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel(application: Application) : ViewModel() {

    val allCourses: LiveData<List<Course>>
    private val courseRepository: CourseRepository
    val searchResults: MutableLiveData<List<Course>>

    val allTrivialQuestions: LiveData<List<TrivialQuestion>>
    private val trivialQuestionRepository: TrivialQuestionRepository

    init {
        val courseDb = CourseRoomDatabase.getInstance(application)
        val courseDao = courseDb.courseDao()
        courseRepository = CourseRepository(courseDao)

        allCourses = courseRepository.allCourses
        searchResults = courseRepository.searchResults

        val trivialQuestionDao = courseDb.trivialQuestionDao()
        trivialQuestionRepository = TrivialQuestionRepository(trivialQuestionDao)
        allTrivialQuestions = trivialQuestionRepository.allQuestions
    }

    fun insertCourse(course: Course) {
        courseRepository.insertCourse(course)
    }

    fun findCourse(name: String) {
        courseRepository.findCourse(name)
    }

    fun deleteCourse(name: String) {
        courseRepository.deleteCourse(name)
    }

    fun insertTrivialQuestion(trivialQuestion: TrivialQuestion) {
        trivialQuestionRepository.insertQuestion(trivialQuestion)
    }

    fun deleteAllTrivialQuestions() {
        trivialQuestionRepository.deleteAllQuestions()
    }

    fun loadTriviaQuestions() {
        deleteAllTrivialQuestions()
        insertTrivialQuestion(TrivialQuestion("What percentage of grads finds success?", "98%", "96%", "90%", "92%", "B"))
        insertTrivialQuestion(TrivialQuestion("What year was Roger Williams University founded?", "1956", "1947", "2000", "1981", "A"))
        insertTrivialQuestion(TrivialQuestion("Where is Roger Williams University located?", "Rhode Island", "New York", "Florida", "New Hampshire", "A"))
        insertTrivialQuestion(TrivialQuestion("How much is awarded annually in financial aid to students?", "78 million", "68 million", "500 thousand", "5 million", "A"))
        insertTrivialQuestion(TrivialQuestion("What field is Roger Williams University best known for?", "Engineering", "Arts", "Law", "Medicine", "C"))
        insertTrivialQuestion(TrivialQuestion("What is Roger Williams University mascot?", "Ace the Pacer", "Swoop the Hawk", "Arnie the Aardvark", "Mack the Warrior", "B"))
        insertTrivialQuestion(TrivialQuestion("What are Roger Williams University colors?", "Red, White, Blue", "Navy blue, Gold, Silver", "Navy blue, Red, Silver", "Navy blue, Gold, Light blue", "D"))
        insertTrivialQuestion(TrivialQuestion("What is the tuition cost annually?", "$50,000", "$43,587", "$45,648", "$47,294", "C"))
        insertTrivialQuestion(TrivialQuestion("How many students attend the school?", "4,000-4,500", "5,000-5,500", "10,000-10,500", "7,000-8,000", "A"))
        insertTrivialQuestion(TrivialQuestion("What is the percentage of students to be first to attend college?", "40%", "27.6%", "20.3%", "15.7%", "B"))
    }
}
