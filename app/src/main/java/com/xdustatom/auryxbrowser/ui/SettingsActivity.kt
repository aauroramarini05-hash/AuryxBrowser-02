package com.xdustatom.auryxbrowser.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.xdustatom.auryxbrowser.AuryxApp
import com.xdustatom.auryxbrowser.R
import com.xdustatom.auryxbrowser.data.BookmarkEntity
import com.xdustatom.auryxbrowser.util.SettingsStore
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: SettingsStore

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val bookmarks = (application as AuryxApp).db.bookmarkDao().getAll()
            val arr = JSONArray()
            bookmarks.forEach {
                arr.put(JSONObject().put("title", it.title).put("url", it.url))
            }
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(arr.toString()) }
            Toast.makeText(this@SettingsActivity, "Export completato", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@launch
            val arr = JSONArray(text)
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                (application as AuryxApp).db.bookmarkDao().insert(
                    BookmarkEntity(title = item.getString("title"), url = item.getString("url"))
                )
            }
            Toast.makeText(this@SettingsActivity, "Import completato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = SettingsStore(this)

        val engines = listOf("duckduckgo", "google", "bing")
        val spinner = findViewById<Spinner>(R.id.searchEngineSpinner)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, engines)
        spinner.setSelection(engines.indexOf(settings.searchEngine).coerceAtLeast(0))
        spinner.setOnItemSelectedListener { _, _, position, _ -> settings.searchEngine = engines[position] }

        bindSwitch(R.id.desktopSwitch, settings.desktopModeDefault) { settings.desktopModeDefault = it }
        bindSwitch(R.id.jsSwitch, settings.javascriptEnabled) { settings.javascriptEnabled = it }
        bindSwitch(R.id.adblockSwitch, settings.adblockEnabled) { settings.adblockEnabled = it }
        bindSwitch(R.id.dntSwitch, settings.doNotTrack) { settings.doNotTrack = it }
        bindSwitch(R.id.popupSwitch, settings.blockPopups) { settings.blockPopups = it }
        bindSwitch(R.id.thirdPartySwitch, settings.blockThirdPartyCookies) { settings.blockThirdPartyCookies = it }
        bindSwitch(R.id.themeSwitch, settings.darkTheme) {
            settings.darkTheme = it
            AppCompatDelegate.setDefaultNightMode(if (it) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO)
        }

        findViewById<Button>(R.id.exportBtn).setOnClickListener { exportLauncher.launch("bookmarks.json") }
        findViewById<Button>(R.id.importBtn).setOnClickListener { importLauncher.launch(arrayOf("application/json")) }
    }

    private fun bindSwitch(id: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
        findViewById<Switch>(id).apply {
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        }
    }
}

private fun Spinner.setOnItemSelectedListener(block: (Spinner, android.view.View?, Int, Long) -> Unit) {
    onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
            block(this@setOnItemSelectedListener, view, position, id)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
    }
}
