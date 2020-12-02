package com.rafgro.biorxivupdates

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager

import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.activity_item_list.*
import kotlinx.android.synthetic.main.item_list_content.view.*
import kotlinx.android.synthetic.main.item_list.*
import java.text.SimpleDateFormat
import java.util.*


val FEED_PERSONALIZED = 0
val FEED_RAW = 1
val FEED_BOOKMARKS = 2
var typeOfFeed: Int = FEED_PERSONALIZED
var firstRun: Boolean = false


/**
 * The main activity
 */
class ItemListActivity : AppCompatActivity() {

    //UI elements
    private lateinit var drawerLayout: DrawerLayout
    private var actionbar: ActionBar? = null
    private lateinit var fabanimation: ObjectAnimator

    companion object {
        var settingsWantRefresh: Boolean = false
    }

    // ViewModel handles all the logic
    private val viewModel: FeedViewModel by lazy {
        ViewModelProviders.of(this).get(FeedViewModel::class.java)
    }

    // Two observers of the feed
    // 1. - State differentiating between loading and the rest
    private val stateObserver = Observer <StateOfFeed> { updateState( viewModel.stateOfFeed.value ) }
    // 2. - Direct list of publications shown to the user
    private val feedObserver = Observer <List<ModelPublications.OnePublication>> {
        updateFeed( viewModel.publications.value )
    }

    // Reactions to observers
    // 1. - Change details of UI to inform the user about the state
    private fun updateState( value: StateOfFeed? ) {
        if( !dontShowMessages ) {
            value?.let {
                when( it.state ) {
                    SpecificStateOfFeed.EMPTY -> stopRotatingArrows()
                    SpecificStateOfFeed.LOADING_GENERAL -> startRotatingArrows()
                    SpecificStateOfFeed.LOADING_DB -> startRotatingArrows()
                    SpecificStateOfFeed.LOADING_NET -> startRotatingArrows()
                    SpecificStateOfFeed.LOADED -> {
                        stopRotatingArrows()
                        Snackbar.make(findViewById(R.id.item_list), "Feed loaded", Snackbar.LENGTH_SHORT)
                            .setAction("Action", null).show()
                    }
                    SpecificStateOfFeed.FAILED -> {
                        stopRotatingArrows()
                        Snackbar.make(findViewById(R.id.item_list), it.message, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                }
            }
        }
        else { dontShowMessages = false }
    }
    private var dontShowMessages: Boolean = false // workaround to don't show snackbars again
    // 2. - Refresh view on change of the feed
    private fun updateFeed( value: List<ModelPublications.OnePublication>? ) {
        item_list.adapter = value?.let { SimpleItemRecyclerViewAdapter(this, it) }
    }

    /* LIFECYCLE */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if( savedInstanceState != null ) dontShowMessages = true //workaround to don't show snackbars again
        setContentView(R.layout.activity_item_list)

        // work with viewmodel and its children if necessary
        viewModel.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        RepoDatabase.database = PubDatabase.getInstance( requireNotNull(this).application ).pubDatabaseDao
        viewModel.publications.observe(this, feedObserver)
        viewModel.stateOfFeed.observe(this, stateObserver)

        // ui inits
        fabanimation = ObjectAnimator.ofFloat(fab, "rotation", 0f, 360f)

        drawerLayout = findViewById(R.id.drawer_layout)
        setSupportActionBar(toolbar)
        actionbar = supportActionBar
        actionbar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        when(typeOfFeed) {
            FEED_PERSONALIZED -> actionbar?.title = "bioRxiv • Personalized Feed"
            FEED_RAW -> actionbar?.title = "bioRxiv • Raw Feed"
            FEED_BOOKMARKS -> actionbar?.title = "bioRxiv • Bookmarks"
        }

        // drawer menu
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            // set item as selected to persist highlight
            menuItem.isChecked = true
            // close drawer when item is tapped
            drawerLayout.closeDrawers()

            // Add code here to update the UI based on the item selected
            when(menuItem.itemId) {
                R.id.nav_personalizedfeed -> changeFeedType(FEED_PERSONALIZED)
                R.id.nav_rawfeed -> changeFeedType(FEED_RAW)
                //R.id.nav_bookmarks -> changeFeedType(FEED_BOOKMARKS)
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java).apply { }
                    this.startActivity(intent)
                }
            }
            true
        }

        // listener for refresh button
        fab.setOnClickListener { view ->
            viewModel.loadPublications(typeOfFeed, forceDownload = true)
        }
    }

    override fun onStart() {
        super.onStart()

        // specific screen for the first launch with empty settings
        if( viewModel.sharedPreferences.getBoolean("firstrun", true) ) {

            viewModel.sharedPreferences.edit().putBoolean("firstrun", false).apply()

            firstRun = true

            val intent = Intent(this, SettingsActivity::class.java).apply { }
            this.startActivity(intent)

        } else {

            // workaround passing need to load from another activity through this to viewmodel
            if( settingsWantRefresh ) {
                viewModel.needToLoad = true
                settingsWantRefresh = false
            }

            // always called, but not always loads publications (needtoload must be true)
            viewModel.loadPublications(typeOfFeed)

        }

    }

    // Reaction to click in drawer menu
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Changing between feed types
    fun changeFeedType(toWhich: Int) {
        when(toWhich) {
            FEED_PERSONALIZED -> {
                typeOfFeed = FEED_PERSONALIZED
                actionbar?.title = "bioRxiv • Personalized Feed"
                viewModel.needToLoad = true
                viewModel.loadPublications(typeOfFeed)
            }
            FEED_RAW -> {
                typeOfFeed = FEED_RAW
                actionbar?.title = "bioRxiv • Raw Feed"
                viewModel.needToLoad = true
                viewModel.loadPublications(typeOfFeed)
            }
            FEED_BOOKMARKS -> {
                typeOfFeed = FEED_BOOKMARKS
                actionbar?.title = "bioRxiv • Bookmarks"
            }
        }
    }


    // Basically setting the UI of the feed
    private fun setupRecyclerView(recyclerView: androidx.recyclerview.widget.RecyclerView, force: Boolean = false) {
        if( recyclerView.adapter == null || force ) {
            viewModel.publications.value?.let {
                recyclerView.adapter = SimpleItemRecyclerViewAdapter(this, it)
            }
        }
    }

    // Rather basic adapter for the list fanciness
    class SimpleItemRecyclerViewAdapter(
        private val parentActivity: ItemListActivity,
        private val values: List<ModelPublications.OnePublication>
    ) :
        androidx.recyclerview.widget.RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private val onClickListener: View.OnClickListener

        fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
            val formatter = SimpleDateFormat(format, locale)
            return formatter.format(this)
        }
        private val currentdatestring = Calendar.getInstance().time.toString("yyyy-MM-dd")
        private val currentdate = Calendar.getInstance()

        init {
            onClickListener = View.OnClickListener { v ->
                val item = v.tag as ModelPublications.OnePublication
                val intent = Intent(v.context, ItemDetailActivity::class.java).apply {
                    putExtra(ItemDetailFragment.ARG_ITEM_ID, item.id.toString())
                }
                v.context.startActivity(intent)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_content, parent, false)
            return ViewHolder(view)
        }

        private fun processDate(rawdate: String) : String {
            if( rawdate == currentdatestring ) return "today"
            else {
                val pubDate = Calendar.getInstance()
                pubDate.time = SimpleDateFormat("yyyy-MM-dd").parse(rawdate)
                val diff = currentdate.timeInMillis - pubDate.timeInMillis
                val days = diff / (24 * 60 * 60 * 1000)
                if( days == 1L ) return "yesterday"
                return "$days days ago"
            }
            return rawdate
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            //holder.idView.text = item.id
            holder.categoryView.text = item.category.toUpperCase()
            holder.titleView.text = item.title
            holder.metaView.text = item.authorshort + " •"
            holder.dateView.text = processDate(item.rawdate)
            holder.quoteView.text = item.singlequote
            if( typeOfFeed == FEED_PERSONALIZED ) {
                var space = ""
                if( item.score < 100 ) space = " "
                else if( item.score < 10 ) space = "  "
                holder.scoreView.text = space + item.score.toString()
            }
            if( item.singlequote.length < 20 ) holder.quoteView.visibility = View.INVISIBLE

            holder.upvoteButton.setOnClickListener { view ->
                if( typeOfFeed == FEED_PERSONALIZED ) {
                    val newscore = item.score + ("\\ ".toRegex().findAll(item.title).count() + 1) * 5
                    holder.scoreView.text = newscore.toString()
                }

                Snackbar.make(view, "You'll see more papers like "+item.title, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                val keywords = parentActivity.viewModel.sharedPreferences.getString("upvotedKeywords", "") + " "+item.title.toLowerCase().replace(":","").replace("?","")
                parentActivity.viewModel.sharedPreferences.edit().putString("upvotedKeywords", keywords).apply()
                ModelPublications.UPVOTED_KEYWORDS = keywords
            }

            holder.downvoteButton.setOnClickListener { view ->
                if( typeOfFeed == FEED_PERSONALIZED ) {
                    val newscore = item.score - ("\\ ".toRegex().findAll(item.title).count() + 1) * 5
                    if( newscore < 0 ) holder.scoreView.text = "0"
                    else holder.scoreView.text = newscore.toString()
                }

                Snackbar.make(view, "You'll see less papers like "+item.title, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

                val keywords = parentActivity.viewModel.sharedPreferences.getString("downvotedKeywords",
                    "") + " "+item.title.toLowerCase().replace(":","").replace("?","")
                parentActivity.viewModel.sharedPreferences.edit().putString("downvotedKeywords", keywords).apply()
                ModelPublications.DOWNVOTED_KEYWORDS = keywords
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener(onClickListener)
            }
        }

        override fun getItemCount() = values.size

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            //val idView: TextView = view.id_text
            val categoryView: TextView = view.category
            val titleView: TextView = view.title
            val metaView: TextView = view.meta
            val dateView: TextView = view.date
            val quoteView: TextView = view.quote
            val scoreView: TextView = view.score
            val upvoteButton: ImageButton = view.upvoteButton
            val downvoteButton: ImageButton = view.downvoteButton
        }
    }

    // Refresh button starts its crazy moves
    private fun startRotatingArrows() {
        if( !fabanimation.isRunning ) {
            fabanimation.repeatMode = ObjectAnimator.RESTART
            fabanimation.repeatCount = 50
            fabanimation.setDuration(800).start()
        }
    }

    // Refresh button stops itself
    private fun stopRotatingArrows() {
        fabanimation.cancel()
        fabanimation.repeatCount = 0
    }
}