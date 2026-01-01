package com.example.medicare.ui

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.medicare.R
open class BaseActivity : AppCompatActivity() {

    override fun setContentView(layoutResID: Int) {
        super.setContentView(R.layout.activity_base)

        val container = findViewById<FrameLayout>(R.id.contentContainer)
        layoutInflater.inflate(layoutResID, container, true)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitleTextColor(getColor(R.color.primaryGreen))
        toolbar.overflowIcon?.setTint(getColor(R.color.primaryGreen))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        for (i in 0 until menu.size()) {
            menu.getItem(i).icon?.setTint(getColor(R.color.primaryGreen))
        }
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_home -> {
                startActivity(Intent(this, DashboardActivity::class.java))
                true
            }
            R.id.menu_emergency -> {
                startActivity(Intent(this, EmergencyActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

