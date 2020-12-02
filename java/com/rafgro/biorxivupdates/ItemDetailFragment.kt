package com.rafgro.biorxivupdates

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_item_detail.*
import kotlinx.android.synthetic.main.item_detail.view.*

/**
 *
 */
class ItemDetailFragment : androidx.fragment.app.Fragment() {

    private var item: ModelPublications.OnePublication? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            if (it.containsKey(ARG_ITEM_ID)) {
                item = ModelPublications.ITEM_MAP[it.getString(ARG_ITEM_ID)]
                activity?.toolbar_layout?.title = item!!.authorshort
                (activity as ItemDetailActivity).detail_url = item!!.link
                //activity?.toolbar_meta?.text = "${item?.meta} ${item?.infodate}"
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.item_detail, container, false)

        item?.let {
            rootView.item_detail_layout.detail_title.text = it.title
            rootView.item_detail_layout.detail_abstract.text = it.abstract_text
            rootView.item_detail_layout.detail_meta.text = it.authorlong
        }

        return rootView
    }

    companion object {
        const val ARG_ITEM_ID = "item_id"
    }
}
