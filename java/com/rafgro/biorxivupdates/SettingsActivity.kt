package com.rafgro.biorxivupdates

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.preference.RingtonePreference
import android.text.TextUtils
import android.view.MenuItem
import androidx.core.app.NavUtils

/**
 *
 */
class SettingsActivity : AppCompatPreferenceActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if( !firstRun ) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            supportActionBar?.title = "Welcome to biorXiv updates 0.3"
        }
        fragmentManager.beginTransaction().replace(android.R.id.content, GeneralPreferenceFragment()).commit()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            ItemListActivity.settingsWantRefresh = true
            NavUtils.navigateUpTo(this, Intent(this, ItemListActivity::class.java))
            true
        } else {
            false
        }
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            if( !firstRun ) {

                addPreferencesFromResource(R.xml.pref_general)


            } else {
                addPreferencesFromResource(R.xml.pref_firstrun)

                val button = findPreference("buttonSavePreferences")
                button.setOnPreferenceClickListener {
                    firstRun = false
                    ItemListActivity.settingsWantRefresh = true
                    NavUtils.navigateUpTo(activity, Intent(activity, ItemListActivity::class.java))
                    true
                }
            }
            setHasOptionsMenu(true)

        }

        override fun onStart() {
            super.onStart()

            if( !firstRun ) {
                val clearUps = findPreference("buttonClearUpvotes")
                clearUps.summary = "Registered upvotes for keywords: " + ModelPublications.UPVOTED_KEYWORDS
                clearUps.setOnPreferenceClickListener {
                    ModelPublications.UPVOTED_KEYWORDS = "emptied"
                    clearUps.summary = "Registered upvotes for keywords: " + ModelPublications.UPVOTED_KEYWORDS
                    true
                }

                val clearDowns = findPreference("buttonClearDownvotes")
                clearDowns.summary = "Registered downvotes for keywords: " + ModelPublications.DOWNVOTED_KEYWORDS
                clearDowns.setOnPreferenceClickListener {
                    ModelPublications.DOWNVOTED_KEYWORDS = "emptied"
                    clearDowns.summary = "Registered downvotes for keywords: " + ModelPublications.DOWNVOTED_KEYWORDS
                    true
                }
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }


    companion object {

        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val stringValue = value.toString()

            if (preference is ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(
                    if (index >= 0)
                        listPreference.entries[index]
                    else
                        null
                )

            } else if (preference is RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent)

                } else {
                    val ringtone = RingtoneManager.getRingtone(
                        preference.getContext(), Uri.parse(stringValue)
                    )

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null)
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        val name = ringtone.getTitle(preference.getContext())
                        preference.setSummary(name)
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
            true
        }

        /**
         *
         */
        private fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes.
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, "")
            )
        }
    }
}
