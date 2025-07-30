import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gymappv10.ClassModel
import com.example.gymappv10.R

class ClassAdapter(
    private var classList: List<ClassModel>,
    private val onItemClick: (ClassModel) -> Unit
) : RecyclerView.Adapter<ClassAdapter.ClassViewHolder>() {

    inner class ClassViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val className: TextView = itemView.findViewById(R.id.className)
        val classTimeAndDuration: TextView = itemView.findViewById(R.id.classTimeAndDuration)
        val classDate: TextView = itemView.findViewById(R.id.classDate)
        val classLocation: TextView = itemView.findViewById(R.id.classLocation)
        val classInstructor: TextView = itemView.findViewById(R.id.classInstructor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_class_card, parent, false)
        return ClassViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClassViewHolder, position: Int) {
        val classItem = classList[position]

        holder.className.text = classItem.name.ifBlank { "Unnamed Class" }

        // Combine time and duration
        val time = classItem.time
        val duration = classItem.duration
        holder.classTimeAndDuration.text = "$time (${duration} min)"

        holder.classDate.text = classItem.dateAdded.ifBlank { "-" }
        holder.classLocation.text = classItem.venue.ifBlank { "Location TBD" }
        holder.classInstructor.text = "By ${classItem.instructor.ifBlank { "TBD" }}"

        holder.itemView.setOnClickListener { onItemClick(classItem) }
    }

    override fun getItemCount(): Int = classList.size

    fun updateList(newList: List<ClassModel>) {
        classList = newList
        notifyDataSetChanged()
    }
}
