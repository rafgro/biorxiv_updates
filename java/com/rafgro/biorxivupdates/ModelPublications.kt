package com.rafgro.biorxivupdates

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import androidx.room.*
import androidx.room.OnConflictStrategy.*
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.*


/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 */
object ModelPublications {

    val ITEMS: MutableList<OnePublication> = ArrayList()
    var ITEM_MAP: MutableMap<String, OnePublication> = HashMap()
    val CATEGORIES: MutableMap<String, Int> = HashMap()
    var UPVOTED_KEYWORDS: String = "-"
    var DOWNVOTED_KEYWORDS: String = "-"

    /* Sorting function for a list of publications */
    fun sort(list: List<OnePublication>) : List<OnePublication> {
        val tempList = list.toMutableList()

        //first, go with id which is exactly correlated with posting time
        if( typeOfFeed == FEED_RAW ) {
            tempList.sortWith(compareByDescending { it.id })
        }

        //exclude categories that are excluded in user preferences
        val allowed = returnCategories().replace("%20"," ")
        tempList.removeAll {
            val cats = it.category.split(", ")
            !(allowed.contains(cats[0], ignoreCase = true))
        }

        if( typeOfFeed == FEED_PERSONALIZED ) {

            //then, calculate score for each publication - universal factors
            tempList.forEach {

                // 0 - base score of ten
                it.score = 10

                // 1 - number of authors
                val noOfAuthors = "\\.\\,\\ ".toRegex().findAll(it.authorlong).count() + 1
                if( noOfAuthors <= 2 ) it.score += 20
                if( noOfAuthors <= 5 ) it.score += 10
                else if( noOfAuthors >= 20 ) it.score += 30
                else if( noOfAuthors >= 40 ) it.score += 60

                // 2 - short titles
                val lengthOfTitle = it.title.length
                if( lengthOfTitle < 95 ) it.score += 20

                // 3 - number of capitalized letters
                var numberOfCapitalizedTitle = 0
                it.title.toList().forEach { if( it.isUpperCase() ) { numberOfCapitalizedTitle++ } }
                if( numberOfCapitalizedTitle <= 6 ) it.score += 10

            }

            //then, calculate score based on individual preferences
            val upKeys: MutableList<String> = UPVOTED_KEYWORDS.split(" ").toMutableList()
            upKeys.removeAll {
                (it.length < 3)
            }
            if( upKeys.size > 3 ) {
                tempList.forEach { it ->
                    upKeys.forEach { key ->
                        if( it.title.contains( key, ignoreCase = true) ) {
                            it.score += 5
                        }
                    }
                }
            }
            val downKeys: MutableList<String> = DOWNVOTED_KEYWORDS.split(" ").toMutableList()
            downKeys.removeAll {
                (it.length < 3)
            }
            if( downKeys.size > 3 ) {
                tempList.forEach { it ->
                    downKeys.forEach { key ->
                        if( it.title.contains( key, ignoreCase = true) ) {
                            it.score -= 5
                        }
                    }
                }
            }

            //then, add influence of priorities between categories
            tempList.forEach {

                var priority = 1
                val cats = it.category.split(", ")
                if( cats.size == 1 ) {
                    val catone = CATEGORIES["cat_"+cats[0].replace(" ","%20")]
                    if(catone != null) priority = catone
                } else {
                    var sum = 0
                    cats.forEach {
                        val catone = CATEGORIES["cat_"+it.replace(" ","%20")]
                        if(catone != null) priority += catone
                        sum++
                    }
                    if( sum > 0 ) priority /= sum
                }

                var convertedPriority = 1.0
                when( priority ) {
                    1 -> convertedPriority = 0.4
                    2 -> convertedPriority = 0.45
                    3 -> convertedPriority = 0.5
                    4 -> convertedPriority = 0.55
                    5 -> convertedPriority = 0.6
                    6 -> convertedPriority = 0.65
                    7 -> convertedPriority = 0.7
                    8 -> convertedPriority = 0.75
                    9 -> convertedPriority = 0.85
                    10 -> convertedPriority = 0.95
                }
                val tempone = it.score.toFloat() * convertedPriority
                it.score = tempone.toInt()

            }

            //then, sort publications by score with twist for _freshness_
            fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
                val formatter = SimpleDateFormat(format, locale)
                return formatter.format(this)
            }
            val currentdatestring = Calendar.getInstance().time.toString("yyyy-MM-dd")
            val currentdate = Calendar.getInstance()

            tempList.forEach {
                //today
                if( it.rawdate == currentdatestring ) it.score *= 4
                else {
                    val pubDate = Calendar.getInstance()
                    pubDate.time = SimpleDateFormat("yyyy-MM-dd").parse(it.rawdate)
                    val diff = currentdate.timeInMillis - pubDate.timeInMillis
                    val days = diff / (24 * 60 * 60 * 1000)
                    //yesterday
                    if( days == 1L ) it.score *= 3
                    //two days ago
                    else if( days == 2L ) it.score = (it.score * 2).toInt()
                    //week old
                    else if( days >= 7L ) it.score /= 3
                }

            }

            //final starting
            tempList.sortWith( compareByDescending { it.score } )
        }

        //cut after 100
        if( tempList.size > 120 ) tempList.dropLast( tempList.size - 100 )

        return tempList.toList()
    }

    /* Returning categories for download task */
    fun returnCategories() : String {
        var toreturn: String = ""
        CATEGORIES.forEach { (key, value) ->
            if( value > 0 ) {
                toreturn = toreturn + key + ","
            }
        }
        if( toreturn.length > 5 ) toreturn = toreturn.substring(4,toreturn.length-1)
        return toreturn
    }

    /* Helper function for database: adding unduplicated items */
    fun addItem(item: OnePublication) {
        //prevent double-category occurences
        var duplicated = false
        ITEMS.forEach {
            if( it.id == item.id ) {
                duplicated = true
                if( it.category != item.category ) it.category = it.category + ", " + item.category
            }
        }

        if( duplicated == false ) {
            ITEMS.add(item)
            //ITEM_MAP.put(item.id, item)
        }
    }

    @Entity(tableName = "publications_table")
    data class OnePublication(
        @PrimaryKey @ColumnInfo(name = "id")
        val id: String = "http://biorxiv.org/",
        @ColumnInfo(name = "title")
        val title: String = "Empty",
        @ColumnInfo(name = "category")
        var category: String = "Empty",
        @ColumnInfo(name = "Unknown")
        val abstract_text: String = "Empty",
        @ColumnInfo(name = "authorshort")
        val authorshort: String = "Empty",
        @ColumnInfo(name = "authorlong")
        val authorlong: String = "Empty",
        @ColumnInfo(name = "singlequote")
        val singlequote: String = "Empty",
        @ColumnInfo(name = "rawdate")
        val rawdate: String = "2019-01-01",
        @ColumnInfo(name = "link")
        val link: String = "Empty",
        var score: Int = 0) {
        override fun toString(): String = title
    }
}

@Dao
interface PubDatabaseDao {

    @Insert(onConflict = REPLACE)
    fun insert(pub: ModelPublications.OnePublication)

    @Update(onConflict = REPLACE)
    fun update(pub: ModelPublications.OnePublication)

    @Transaction
    fun upsert(pub: ModelPublications.OnePublication) {
        try {
            insert(pub)
        } catch(t: Throwable) {
            try {
                update(pub)
            }
            catch(t: Throwable) { }
        }
    }

    @Query("SELECT * from publications_table WHERE id = :key")
    fun get(key: Long): ModelPublications.OnePublication?

    @Query("DELETE FROM publications_table")
    fun clear()

    @Query("SELECT * FROM publications_table ORDER BY id DESC")
    fun getAll(): List<ModelPublications.OnePublication>

    @Query("SELECT * FROM publications_table ORDER BY id DESC LIMIT 500")
    fun getLimited(): List<ModelPublications.OnePublication>

}

@Database(entities = [ModelPublications.OnePublication::class], version = 1, exportSchema = false)
abstract class PubDatabase : RoomDatabase() {

    abstract val pubDatabaseDao: PubDatabaseDao

    companion object {

        @Volatile
        private var INSTANCE: PubDatabase? = null

        fun getInstance(context: Context): PubDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if( instance == null ) {
                    instance = Room.databaseBuilder(context.applicationContext, PubDatabase::class.java, "publications_database")
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}