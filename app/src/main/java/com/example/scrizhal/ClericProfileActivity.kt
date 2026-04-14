package com.example.scrizhal

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
class ClericProfileActivity : AppCompatActivity() {

    private lateinit var prefManager: SharedPrefManager
    private var clericId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cleric_profile)

        prefManager = SharedPrefManager(this)
        clericId = prefManager.getCurrentClericId() ?: run {
            Toast.makeText(this, "Клирик не найден, войдите заново", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val cleric = prefManager.getClericList().firstOrNull { it.id == clericId }
        if (cleric == null) {
            Toast.makeText(this, "Клирик не найден в списке", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 3
            override fun createFragment(position: Int): Fragment = when (position) {
                0 -> ClericProfileTabFragment.newInstance(clericId)
                1 -> ClericLiturgiesTabFragment.newInstance(clericId)
                else -> ClericNotificationsTabFragment.newInstance(clericId)
            }
        }

        val navIds = listOf(
            R.id.navClericProfile,
            R.id.navClericLiturgies,
            R.id.navClericNotifications
        )

        fun selectTab(index: Int) {
            viewPager.currentItem = index
            navIds.forEachIndexed { i, id ->
                val iv = findViewById<ImageView>(id)
                when (id) {
                    R.id.navClericProfile -> iv.setImageResource(
                        if (i == index) R.drawable.ic_nav_list_active else R.drawable.ic_nav_list
                    )
                    R.id.navClericLiturgies -> iv.setImageResource(
                        if (i == index) R.drawable.ic_nav_done_active else R.drawable.ic_nav_done
                    )
                    R.id.navClericNotifications -> iv.setImageResource(
                        if (i == index) R.drawable.ic_nav_inbox_active else R.drawable.ic_nav_inbox
                    )
                }
            }
        }

        navIds.forEachIndexed { index, id ->
            findViewById<ImageView>(id).setOnClickListener { selectTab(index) }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                selectTab(position)
            }
        })

        selectTab(0)
    }

    class AssignmentVH(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val tvFeast: TextView = view.findViewById(R.id.tvAssignmentFeast)
        val tvDate: TextView = view.findViewById(R.id.tvAssignmentDate)
        val tvStatus: TextView = view.findViewById(R.id.tvAssignmentStatus)
    }
}
