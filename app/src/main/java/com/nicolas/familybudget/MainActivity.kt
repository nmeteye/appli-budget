package com.nicolas.familybudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.nicolas.familybudget.data.repository.CategoryRepository
import com.nicolas.familybudget.ui.navigation.AppNavigation
import com.nicolas.familybudget.ui.theme.FamilyBudgetTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var categoryRepository: CategoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Amorce les categories par defaut au premier lancement.
        lifecycleScope.launch { categoryRepository.seedIfEmpty() }

        setContent {
            FamilyBudgetTheme {
                AppNavigation()
            }
        }
    }
}
