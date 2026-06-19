package com.capstone.smartbite

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.capstone.smartbite.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class  MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Apply saved Dark Mode preference
        val mAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val mUserPreference = UserPreference(this, mAuth.currentUser?.email)
        if (mUserPreference.isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Apply saved Language preference
        val langCode = mUserPreference.getLanguage()
        val locale = java.util.Locale(langCode)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle insets for the main container (Status Bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right
            )
            insets
        }

        val navView: BottomNavigationView = binding.navView

        // Handle insets for the BottomNavigationView (Push it above 3-button navigation & match size)
        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val baseHeight = (50 * resources.displayMetrics.density).toInt()
            
            val params = v.layoutParams
            params.height = baseHeight + systemBars.bottom
            v.layoutParams = params

            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

}

