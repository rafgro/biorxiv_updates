package com.rafgro.biorxivupdates

import android.content.SharedPreferences
import androidx.lifecycle.*
import java.util.HashMap

class FeedViewModel() : ViewModel(), LifecycleObserver, DatabaseCallback<String>, NetworkCallback<String> {

    // Preferences initialized from the main activity
    lateinit var sharedPreferences: SharedPreferences

    // Booleans governing most important method: loadPublications
    var needToLoad: Boolean = true
    var downloadOnStart: Boolean = false

    // LiveData observed by the main activity
    val publications = MutableLiveData< List<ModelPublications.OnePublication> >()
    val stateOfFeed = MutableLiveData<StateOfFeed>()

    // Two repository classes
    val repoDatabase = RepoDatabase()
    val repoNetwork = RepoNetwork()

    // Initialization of callbacks from repositories
    init {
        repoDatabase.mCallback = this
        repoNetwork.mCallback = this
        needToLoad = true
    }

    // Main external point of entrance, starts download/refresh/first load, depending on variables and args
    fun loadPublications(typeOfFeed: Int, forceDownload: Boolean = false) {

        // If someone cleared keywords, we need this simple workaround to finish clearing
        if( ModelPublications.UPVOTED_KEYWORDS == "emptied" ) {
            sharedPreferences.edit().putString("upvotedKeywords", "-").apply()
            ModelPublications.UPVOTED_KEYWORDS = "-"
        }
        if( ModelPublications.DOWNVOTED_KEYWORDS == "emptied" ) {
            sharedPreferences.edit().putString("downvotedKeywords", "-").apply()
            ModelPublications.DOWNVOTED_KEYWORDS = "-"
        }

        // Updating keywords, as they can frequently change from the main screen
        ModelPublications.UPVOTED_KEYWORDS = sharedPreferences.getString("upvotedKeywords", "-")
        ModelPublications.DOWNVOTED_KEYWORDS = sharedPreferences.getString("downvotedKeywords", "-")

        // Loading categories settings from shared preferences
        loadCategoriesPreferences()

        // Variants of starting of the loading (mainly on another threads)
        if( ModelPublications.CATEGORIES.isNotEmpty() ) {

            // Download forced by the user or settings
            if( forceDownload ) {
                stateOfFeed.value = StateOfFeed(SpecificStateOfFeed.LOADING_GENERAL)
                repoNetwork.downloadPublications()
                needToLoad = false
            }

            // Loading only database
            else if( needToLoad && !downloadOnStart ) {
                stateOfFeed.value = StateOfFeed(SpecificStateOfFeed.LOADING_GENERAL)
                repoDatabase?.apply {
                    loadDatabase(typeOfFeed)
                }
                needToLoad = false
            }

            // Downloading on start of application
            else if( needToLoad && downloadOnStart ) {
                stateOfFeed.value = StateOfFeed(SpecificStateOfFeed.LOADING_GENERAL)
                repoNetwork.downloadPublications()
                needToLoad = false
            }

            // There are many cases where no if was true
            // Without download forcing and without need to load, we don't start anything
            // That's to avoid too frequent refreshes of the feed
            // (Any change to publication list is observed by the main activity!)
        }

    }

    // Awful but necessary
    private fun loadCategoriesPreferences() {

        val listOfPreferenceKeys = listOf (
            "cat_animal%20behavior%20and%20cognition",
            "cat_biochemistry",
            "cat_bioengineering",
            "cat_bioinformatics",
            "cat_biophysics",
            "cat_cancer%20biology",
            "cat_cell%20biology",
            "cat_clinical%20trials",
            "cat_developmental%20biology",
            "cat_ecology",
            "cat_epidemiology",
            "cat_evolutionary%20biology",
            "cat_genetics",
            "cat_genomics",
            "cat_immunology",
            "cat_microbiology",
            "cat_molecular%20biology",
            "cat_neuroscience",
            "cat_paleontology",
            "cat_pathology",
            "cat_pharmacology%20and%20toxicology",
            "cat_physiology",
            "cat_plant%20biology",
            "cat_scientific%20communication",
            "cat_synthetic%20biology",
            "cat_systems%20biology",
            "cat_zoology"
        )

        ModelPublications.CATEGORIES.clear()
        listOfPreferenceKeys.forEach {
            ModelPublications.CATEGORIES.put( it, sharedPreferences.getInt(it,0) )
        }
        downloadOnStart = sharedPreferences.getBoolean("download_switch", true)
    }

    // Middle ground between the feed and the details, uploaded to model object
    private fun createHashMapOfPubs() {
        val ITEM_MAP: MutableMap<String, ModelPublications.OnePublication> = HashMap()
        publications.value?.let {
            it.forEach {
                ITEM_MAP.put(it.id, it)
            }
            ModelPublications.ITEM_MAP = ITEM_MAP
        }
    }

    /* Database */
    // After download, we need to upload the list to the database
    override fun updateDatabase( list: List<ModelPublications.OnePublication> ) {
        repoDatabase?.apply {
            updateDatabase(list)
        }
    }

    // Basically refreshes the feed with new data
    override fun updateAfterLoad() {
        publications.value = ModelPublications.sort( ModelPublications.ITEMS )
        createHashMapOfPubs()
        ModelPublications.ITEMS.clear()
        stateOfFeed.value = StateOfFeed( SpecificStateOfFeed.LOADED )
    }

    /* Network */
    // Resolve the result of download try
    override fun updatedNetworkState() {
        when( repoNetwork.state ) {
            SpecificStateOfDownload.FAILURE ->
                stateOfFeed.value = StateOfFeed( SpecificStateOfFeed.FAILED, repoNetwork.errormessage )
            SpecificStateOfDownload.FINISHED -> {
                updateDatabase( ModelPublications.sort( repoNetwork.temporaryPublications ) )
                //stateOfFeed.value = StateOfFeed( SpecificStateOfFeed.LOADED )
            }
            else -> {
                val todo = 0
            }
        }
    }

}

interface DatabaseCallback<T> {

    fun updateDatabase( list: List<ModelPublications.OnePublication> )

    fun updateAfterLoad()

}

interface NetworkCallback<T> {

    fun updatedNetworkState()

}