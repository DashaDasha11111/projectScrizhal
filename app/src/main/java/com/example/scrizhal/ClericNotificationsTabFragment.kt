package com.example.scrizhal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration

class ClericNotificationsTabFragment : Fragment() {

    private var assignmentsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cleric_notifications_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val clericId = requireArguments().getInt(ARG_CLERIC_ID)

        val tvEmpty = view.findViewById<TextView>(R.id.tvNotificationsEmpty)
        tvEmpty.text = "Загрузка..."
        tvEmpty.visibility = View.VISIBLE

        // Кнопка "очистить" теперь не нужна (данные живут в Firestore), скрываем её
        view.findViewById<TextView>(R.id.btnClearNotifications).visibility = View.GONE

        // Подписываемся на назначения из Firestore — это и есть уведомления клирика
        assignmentsListener = FirestoreManager.listenToAssignmentsForCleric(clericId) { assignments ->
            if (!isAdded) return@listenToAssignmentsForCleric
            requireActivity().runOnUiThread {
                if (!isAdded) return@runOnUiThread
                refreshList(view, assignments)
            }
        }
    }

    private fun refreshList(view: View, assignments: List<Map<String, Any>>) {
        val container = view.findViewById<LinearLayout>(R.id.notificationsContainer) ?: return
        container.removeAllViews()

        val tvEmpty = view.findViewById<TextView>(R.id.tvNotificationsEmpty)
        tvEmpty.text = "Нет уведомлений"
        tvEmpty.visibility = if (assignments.isEmpty()) View.VISIBLE else View.GONE

        for (a in assignments) {
            val v = layoutInflater.inflate(R.layout.item_cleric_notification, container, false)
            v.findViewById<TextView>(R.id.tvNotifText).text = "Вы назначены на праздничную литургию"
            val date = a["date"] as? String ?: ""
            val churchName = a["churchName"] as? String ?: ""
            val feastName = a["feastName"] as? String ?: ""
            v.findViewById<TextView>(R.id.tvNotifMeta).text = "$date • $churchName • $feastName"
            container.addView(v)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        assignmentsListener?.remove()
        assignmentsListener = null
    }

    companion object {
        private const val ARG_CLERIC_ID = "cleric_id"
        fun newInstance(clericId: Int): ClericNotificationsTabFragment =
            ClericNotificationsTabFragment().apply {
                arguments = Bundle().apply { putInt(ARG_CLERIC_ID, clericId) }
            }
    }
}
