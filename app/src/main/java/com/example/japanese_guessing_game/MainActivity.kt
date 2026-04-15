package com.example.japanese_guessing_game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.japanese_guessing_game.data.ReturnValue
import com.example.japanese_guessing_game.ui.theme.Japanese_guessing_gameTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Japanese_guessing_gameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "menu") {
                        composable("menu") {
                            MenuScreen(
                                onNavigate = { dest ->
                                    if (dest.startsWith("game")) {
                                        val parts = dest.split("/")
                                        val type = GameType.valueOf(parts[1])
                                        val direction = GameDirection.valueOf(parts[2])
                                        viewModel.startGame(type, direction)
                                        navController.navigate("game")
                                    } else {
                                        navController.navigate(dest)
                                    }
                                },
                                viewModel = viewModel
                            )
                        }
                        composable("game") {
                            GuessingGameScreen(
                                viewModel = viewModel,
                                onNavigateToMenu = { navController.popBackStack() }
                            )
                        }
                        composable("word_list") {
                            WordListScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuScreen(onNavigate: (String) -> Unit, viewModel: MainViewModel) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showReturnValueDialog by remember { mutableStateOf(false) }

    // 1. Change this from a standard variable to a MutableState
    var returnValue by remember { mutableStateOf(ReturnValue(0, null)) }

    // 2. Create a coroutine scope for the UI
    val scope = rememberCoroutineScope()

    // Need to import kotlinx.coroutines.launch

    if (showAddDialog) {
        AddWordDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { h, m1, m2 ->
                // 3. Launch a coroutine to call the suspend function
                scope.launch {
                    val result = viewModel.addWord(h, m1, m2)

                    returnValue = result
                    showReturnValueDialog = true

                }
            }
        )
    }

    if (showReturnValueDialog) {
        ReturnValueDialog(
            onDismiss = { showReturnValueDialog = false},
            returnValue // Pass the state value
        )
        if (returnValue.statusCode == 201) {
            showAddDialog = false
        }
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Japanese Learning", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        // Translation Buttons
        Button(
            onClick = { onNavigate("game/TRANSLATION/ENGLISH_TO_JAPANESE") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Translation (Eng -> Jap)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onNavigate("game/TRANSLATION/JAPANESE_TO_ENGLISH") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Translation (Jap -> Eng)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Picker Buttons
        Button(
            onClick = { onNavigate("game/PICKER/ENGLISH_TO_JAPANESE") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Picker (Eng -> Jap)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onNavigate("game/PICKER/JAPANESE_TO_ENGLISH") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Picker (Jap -> Eng)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Other Options
        Button(
            onClick = { onNavigate("word_list") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Word List")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.loadWords() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load Data (Sync)")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Insert New Word")
        }
    }
}

@Composable
fun GuessingGameScreen(viewModel: MainViewModel, onNavigateToMenu: () -> Unit) {
    val currentWord by viewModel.currentWord.collectAsState()
    val score by viewModel.score.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val choices by viewModel.choices.collectAsState()
    
    // We need to know the game type to render correct UI
    // Exposing flow or property from VM is better, but function works too
    val gameType = viewModel.getCurrentGameMode()
    val direction = viewModel.getCurrentDirection()

    var answerText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onNavigateToMenu,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Menu")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Score: $score", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(32.dp))

            if (currentWord != null) {
                // Question Text
                Text(
                    text = viewModel.getQuestionText(),
                    style = MaterialTheme.typography.displayMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                if (gameType == GameType.TRANSLATION) {
                    OutlinedTextField(
                        value = answerText,
                        onValueChange = { answerText = it },
                        label = { Text("Enter Answer") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { 
                        viewModel.checkAnswer(answerText)
                        answerText = ""
                    }) {
                        Text("Submit")
                    }
                } else {
                    // Picker Mode
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        choices.forEach { choice ->
                            Button(
                                onClick = { viewModel.checkAnswer(choice) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(choice)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                // Skip Button
                OutlinedButton(onClick = { viewModel.skipWord() }) {
                    Text("Skip / Show Answer")
                }
            } else {
                Text("No words available.")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = feedbackMessage, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun WordListScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val words by viewModel.words.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Word List", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterStart))
            Button(onClick = onBack, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text("Back")
            }
        }
        
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(words) { word ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = word.hiragana, style = MaterialTheme.typography.titleLarge)
                        Text(text = "Meaning 1: ${word.meaning_1}")
                        if (word.meaning_2 != null) {
                            Text(text = "Meaning 2: ${word.meaning_2}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddWordDialog(onDismiss: () -> Unit, onAdd: (String, String, String?) -> Unit) {
    var hiragana by remember { mutableStateOf("") }
    var meaning1 by remember { mutableStateOf("") }
    var meaning2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Word") },
        text = {
            Column {
                OutlinedTextField(
                    value = hiragana,
                    onValueChange = { hiragana = it },
                    label = { Text("Hiragana") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = meaning1,
                    onValueChange = { meaning1 = it },
                    label = { Text("Meaning 1") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = meaning2,
                    onValueChange = { meaning2 = it },
                    label = { Text("Meaning 2 (Optional)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (hiragana.isNotBlank() && meaning1.isNotBlank()) {
                    onAdd(hiragana, meaning1, meaning2.ifBlank { null })
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReturnValueDialog(onDismiss: () -> Unit, returnValue: ReturnValue){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Return value dialog") },
        text = {
            Column {
                Text("Status Code: ${returnValue.statusCode}")
                Text("Message: ${returnValue.text ?: "No data"}")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}