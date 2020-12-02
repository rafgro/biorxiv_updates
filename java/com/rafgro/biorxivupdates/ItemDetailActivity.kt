package com.rafgro.biorxivupdates

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_item_detail.*

/**
 *
 */
class ItemDetailActivity : AppCompatActivity() {

    var detail_url: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item_detail)
        setSupportActionBar(detail_toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val fragment = ItemDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        ItemDetailFragment.ARG_ITEM_ID,
                        intent.getStringExtra(ItemDetailFragment.ARG_ITEM_ID)
                    )
                }
            }

            supportFragmentManager.beginTransaction()
                .add(R.id.item_detail_container, fragment)
                .commit()
        }

        /*buttonGoToPDF.setOnClickListener{
            if( detail_url != null ) {
                val webpage: Uri = Uri.parse(detail_url)
                val intent = Intent(Intent.ACTION_VIEW, webpage)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
            }
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.detail_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            android.R.id.home -> {
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back

                NavUtils.navigateUpTo(this, Intent(this, ItemListActivity::class.java))
                true
            }
            R.id.action_gotopdf -> {
                if( detail_url != null ) {
                    val webpage: Uri = Uri.parse(detail_url)
                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
