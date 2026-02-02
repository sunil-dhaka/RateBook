package com.example.ratebook

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ratebook.databinding.ActivityMainBinding
import com.example.ratebook.ui.categories.CategoryListFragment
import com.example.ratebook.ui.units.UnitListFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_products, R.id.nav_categories, R.id.nav_units, R.id.nav_settings),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.nav_products -> {
                    binding.appBarMain.fab.show()
                    binding.appBarMain.fab.setImageResource(R.drawable.ic_add)
                }
                R.id.nav_categories -> {
                    binding.appBarMain.fab.show()
                    binding.appBarMain.fab.setImageResource(R.drawable.ic_add)
                }
                R.id.nav_units -> {
                    binding.appBarMain.fab.show()
                    binding.appBarMain.fab.setImageResource(R.drawable.ic_add)
                }
                else -> {
                    binding.appBarMain.fab.hide()
                }
            }
        }

        binding.appBarMain.fab.setOnClickListener {
            val currentDestination = navController.currentDestination?.id
            when (currentDestination) {
                R.id.nav_products -> {
                    navController.navigate(R.id.nav_product_edit)
                }
                R.id.nav_categories -> {
                    val fragment = navHostFragment.childFragmentManager
                        .primaryNavigationFragment as? CategoryListFragment
                    fragment?.showAddDialog()
                }
                R.id.nav_units -> {
                    val fragment = navHostFragment.childFragmentManager
                        .primaryNavigationFragment as? UnitListFragment
                    fragment?.showAddDialog()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
