package com.rafgro.biorxivupdates

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.*
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList

interface Api {
    //urls
    @GET("biorxiv_xml.php")
    fun returnRSS(@Query("subject") subject: String): Call<String>
    val pubs: Call<String>
}

enum class SpecificStateOfDownload {
    EMPTY, STARTED, FAILURE, FINISHED
}

class RepoNetwork {

    var mCallback: NetworkCallback<String>? = null
    var errormessage : String = " "
    var state : SpecificStateOfDownload = SpecificStateOfDownload.EMPTY
    val temporaryPublications: MutableList<ModelPublications.OnePublication> = ArrayList()

    fun downloadPublications() {
        state = SpecificStateOfDownload.STARTED

        temporaryPublications.clear()

        val categories = ModelPublications.returnCategories().replace("cat_","").split(",")
        var lastDetector = 0
        categories.forEach {
            var retrofit: Retrofit = Retrofit.Builder()
                .baseUrl("http://connect.biorxiv.org/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

            var api = retrofit.create(Api::class.java)
            val catString = it.replace("cat_","")
            var call = api.returnRSS(catString)
            call.enqueue(object : Callback<String> {

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    processPublicationsFromRSS( response?.body(), catString )
                    lastDetector++
                    if( lastDetector == categories.size ) {
                        state = SpecificStateOfDownload.FINISHED
                        mCallback?.apply {
                            updatedNetworkState()
                        }
                    }
                }

                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    state = SpecificStateOfDownload.FAILURE

                    if( t is HttpException ) {
                        errormessage = "bioRxiv server is unavailable"
                    } else if( t is IOException ) {
                        errormessage = "No network connection"
                    } else {
                        errormessage = "Download failed"
                    }

                    mCallback?.apply {
                        updatedNetworkState()
                    }

                }
            })
        }
    }

    fun processPublicationsFromRSS( xml: String?, category: String ) {

        if( xml.isNullOrEmpty() ) {

        }
        else {
            val tempcategory = category.replace("%20"," ").replace("cat_","")

            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setFeature(Xml.FEATURE_RELAXED, true)

            xpp.setInput(StringReader(xml.replace("dc:","dc").replace("\n","")))
            var eventType = xpp.eventType
            var limiter = 0
            var inside = false
            var temptitle = "Empty title "
            var tempabstract = "Empty abstract."
            var tempauthor = "R Author"
            var tempauthorsnumber = 0
            var tempallauthors = "R Authors"
            var tempsinglequote = "Empty"
            var templink = "http://newsciencesolutions.com/"
            var tempdateinfo = " "
            var tempdateraw = " "
            var tempid = " "

            while (eventType != XmlPullParser.END_DOCUMENT && limiter < 1000) {
                limiter++
                when (eventType) {
                    XmlPullParser.START_TAG -> if (xpp.name.equals("item", ignoreCase = true)) {
                        inside = true
                        tempauthor = "Author"
                        tempauthorsnumber = 0
                        tempallauthors = "Authors"
                    }
                    else if(xpp.name.equals("title", ignoreCase = true) ) { temptitle = xpp.nextText() }
                    else if(xpp.name.equals("description", ignoreCase = true) ) {
                        tempabstract = xpp.nextText().replace(" \\body","").replace("nn"," ")
                        val divided = tempabstract.split(". ")
                        if( divided.size > 2 ) tempsinglequote = divided[ divided.size - 1 ]
                    }
                    else if(xpp.name.equals("link", ignoreCase = true) ) { templink = xpp.nextText() }
                    else if(xpp.name.equals("dcidentifier", ignoreCase = true) ) { tempid = xpp.nextText() }
                    else if(xpp.name.equals("dccreator", ignoreCase = true) ) {
                        tempauthorsnumber++
                        if(tempauthorsnumber == 1) {
                            tempauthor = xpp.nextText()
                            tempallauthors = tempauthor
                        }
                        else {
                            tempallauthors += ", " + xpp.nextText()
                        }
                    }
                    else if(xpp.name.equals("dcdate", ignoreCase = true) ) {
                        val pubdate = xpp.nextText()
                        tempdateraw = pubdate
                    }
                    XmlPullParser.END_TAG -> if (xpp.name.equals("item", ignoreCase = true) && inside) {
                        var authorshortstr: String = ""
                        if( tempauthorsnumber == 1 && tempallauthors.indexOf(".,") > 1 ) {
                            tempauthorsnumber += "\\.\\,\\ ".toRegex().findAll(tempallauthors).count()
                            tempauthor = tempallauthors.substring( 0, tempallauthors.indexOf(".,")+1 )
                            authorshortstr = "$tempauthor (et $tempauthorsnumber al)"
                        } else if( tempauthorsnumber == 1 ) {
                            authorshortstr = "$tempauthor"
                        } else {
                            authorshortstr = "$tempauthor (et $tempauthorsnumber al)"
                        }
                        var duplicated = false
                        temporaryPublications.forEach {
                            if( it.id == tempid ) {
                                duplicated = true
                                if( it.category != tempcategory ) it.category = it.category + ", " + tempcategory
                            }
                        }
                        if( duplicated == false ) {
                            temporaryPublications.add(
                                ModelPublications.OnePublication(
                                    id = tempid, category = tempcategory, title = temptitle,
                                    abstract_text = tempabstract, authorshort = authorshortstr,
                                    authorlong = tempallauthors, singlequote = tempsinglequote, rawdate = tempdateraw,
                                    link = templink
                                )
                            )
                        }
                    }
                }
                eventType = xpp.next()
            }
        }
    }
}