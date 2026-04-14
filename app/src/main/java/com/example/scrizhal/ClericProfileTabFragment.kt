package com.example.scrizhal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ClericProfileTabFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cleric_profile_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clericId = requireArguments().getInt(ARG_CLERIC_ID)
        val prefManager = SharedPrefManager(requireContext())
        val cleric = prefManager.getClericList().firstOrNull { it.id == clericId } ?: return

        view.findViewById<TextView>(R.id.tvClericName).text = cleric.name
        view.findViewById<TextView>(R.id.tvClericRank).text = ClericsListAdapter.getDisplayRank(cleric)
        view.findViewById<TextView>(R.id.tvClericChurch).text =
            if (cleric.church.isNotBlank()) cleric.church else "Храм не указан"
        view.findViewById<TextView>(R.id.tvClericInfo).text =
            cleric.description.ifBlank { "Информация не указана" }
        val assignments = prefManager.getAssignmentsForCleric(clericId)
        view.findViewById<TextView>(R.id.tvTotalLiturgies).text = assignments.size.toString()

        view.findViewById<TextView>(R.id.btnClericLogout).setOnClickListener {
            FirestoreManager.unregisterToken("cleric_$clericId")
            prefManager.clearCurrentClericId()
            requireActivity().finish()
        }
    }

    companion object {
        private const val ARG_CLERIC_ID = "cleric_id"
        fun newInstance(clericId: Int): ClericProfileTabFragment =
            ClericProfileTabFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CLERIC_ID, clericId) }
            }
    }
}
