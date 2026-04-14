package com.example.scrizhal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration

class ClericLiturgiesTabFragment : Fragment() {

    private var assignmentsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cleric_liturgies_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clericId = requireArguments().getInt(ARG_CLERIC_ID)

        val rv = view.findViewById<RecyclerView>(R.id.rvAssignments)
        rv.layoutManager = LinearLayoutManager(requireContext())

        val tvEmpty = view.findViewById<TextView>(R.id.tvAssignmentsEmpty)
        tvEmpty.text = "Загрузка..."
        tvEmpty.visibility = View.VISIBLE

        // Подписываемся на назначения из Firestore — данные от митрополита приходят в реальном времени
        assignmentsListener = FirestoreManager.listenToAssignmentsForCleric(clericId) { assignments ->
            if (!isAdded) return@listenToAssignmentsForCleric
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread

                rv.adapter = object : RecyclerView.Adapter<ClericProfileActivity.AssignmentVH>() {
                    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) =
                        ClericProfileActivity.AssignmentVH(
                            layoutInflater.inflate(R.layout.item_liturgy_assignment, parent, false)
                        )
                    override fun getItemCount(): Int = assignments.size
                    override fun onBindViewHolder(
                        holder: ClericProfileActivity.AssignmentVH,
                        position: Int
                    ) {
                        val a = assignments[position]
                        holder.tvFeast.text = a["feastName"] as? String ?: ""
                        holder.tvDate.text = a["date"] as? String ?: ""
                        holder.tvStatus.text = a["status"] as? String ?: ""
                    }
                }

                tvEmpty.text = "Нет назначений"
                tvEmpty.visibility = if (assignments.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        assignmentsListener?.remove()
        assignmentsListener = null
    }

    companion object {
        private const val ARG_CLERIC_ID = "cleric_id"
        fun newInstance(clericId: Int): ClericLiturgiesTabFragment =
            ClericLiturgiesTabFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CLERIC_ID, clericId) }
            }
    }
}
