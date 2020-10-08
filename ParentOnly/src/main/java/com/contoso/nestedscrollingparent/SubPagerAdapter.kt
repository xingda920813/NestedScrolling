package com.contoso.nestedscrollingparent

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.contoso.nestedscrollingparent.model.PageVO

class SubPagerAdapter(
    fm: FragmentManager,
    private val itemList: List<PageVO>
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        val fragment: Fragment = SubFragment()
        val bundle = Bundle()
        bundle.putInt("color", itemList[position].color)
        bundle.putInt("position", position)
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int {
        return itemList.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return itemList[position].title
    }
}
