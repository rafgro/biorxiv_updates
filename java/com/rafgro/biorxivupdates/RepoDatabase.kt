package com.rafgro.biorxivupdates

import android.os.AsyncTask

class RepoDatabase {
    var mCallback: DatabaseCallback<String>? = null
    private var databaseTask: DatabaseTask? = null

    companion object {
        lateinit var database: PubDatabaseDao
    }

    fun updateDatabase(list: List<ModelPublications.OnePublication>) {
        mCallback?.also { callback ->
            databaseTask = DatabaseTask(callback).apply {
                execute(listOf("update"), list)
            }
        }
    }

    fun loadDatabase(typeOfFeed: Int) {
        mCallback?.also { callback ->
            databaseTask = DatabaseTask(callback).apply {
                execute(listOf("load_$typeOfFeed"))
            }
        }
    }

    private class DatabaseTask(callback: DatabaseCallback<String>)
        : AsyncTask<List<*>, Int, DatabaseTask.Result>() {

        private var mCallback: DatabaseCallback<String>? = null

        init {
            setCallback(callback)
        }

        internal fun setCallback(callback: DatabaseCallback<String>) {
            mCallback = callback
        }

        internal class Result {
            var mResultValue: String? = null
            var mException: Exception? = null

            constructor(resultValue: String) {
                mResultValue = resultValue
            }

            constructor(exception: Exception) {
                mException = exception
            }
        }

        override fun doInBackground(vararg params: List<*>): DatabaseTask.Result? {
            var result: Result? = null

            if(params[0][0] == "update") {

                if( params[1].isNotEmpty() ) {

                    ModelPublications.ITEMS.clear()

                    params[1].forEach {
                        if( it is ModelPublications.OnePublication ) {

                            database.upsert(
                                ModelPublications.OnePublication(
                                    id = it.id, title = it.title, category = it.category, authorshort = it.authorshort,
                                    authorlong = it.authorlong, abstract_text = it.abstract_text,
                                    singlequote = it.singlequote, rawdate = it.rawdate, link = it.link
                                )
                            )

                            ModelPublications.addItem(
                                ModelPublications.OnePublication(
                                    id = it.id, title = it.title, category = it.category, authorshort = it.authorshort,
                                    authorlong = it.authorlong, abstract_text = it.abstract_text,
                                    singlequote = it.singlequote, rawdate = it.rawdate, link = it.link ) )

                        }
                    }

                }

            } else if(params[0][0] == "load_0") {
                ModelPublications.ITEMS.clear()
                    database.getLimited().forEach {
                    ModelPublications.addItem(
                        ModelPublications.OnePublication(
                            id = it.id, title = it.title, category = it.category, authorshort = it.authorshort,
                            authorlong = it.authorlong, abstract_text = it.abstract_text, singlequote = it.singlequote,
                            rawdate = it.rawdate, link = it.link ) )
                }
            } else if(params[0][0] == "load_1") {
                ModelPublications.ITEMS.clear()
                    database.getLimited().forEach {
                    ModelPublications.addItem(
                        ModelPublications.OnePublication(
                            id = it.id, title = it.title, category = it.category, authorshort = it.authorshort,
                            authorlong = it.authorlong, abstract_text = it.abstract_text, singlequote = it.singlequote,
                            rawdate = it.rawdate, link = it.link ) )
                }
            }

            //TODO: cleaning of older pubs

            return result
        }

        override fun onPostExecute(result: Result?) {
            mCallback?.apply {
                updateAfterLoad()
            }
        }

    }

}