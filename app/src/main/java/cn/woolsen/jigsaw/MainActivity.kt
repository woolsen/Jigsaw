package cn.woolsen.jigsaw

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        jigsaw.setOnSuccessListener {
            Toast.makeText(this, "Win!!!", Toast.LENGTH_SHORT).show()
        }
        btn_start.setOnClickListener {
            jigsaw.start()
        }

        tv_column.text = jigsaw.columnCount.toString()
        tv_row.text = jigsaw.rowCount.toString()

        btn_reset.setOnClickListener {
            jigsaw.reset()
        }

        btn_column_minus.setOnClickListener {
            if (jigsaw.columnCount == 3) {
                Toast.makeText(this, "列数不能小于3", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            jigsaw.columnCount = jigsaw.columnCount - 1
            tv_column.text = jigsaw.columnCount.toString()
        }
        btn_column_plus.setOnClickListener {
            jigsaw.columnCount = jigsaw.columnCount + 1
            tv_column.text = jigsaw.columnCount.toString()
        }
        btn_row_minus.setOnClickListener {
            if (jigsaw.rowCount == 3) {
                Toast.makeText(this, "行数不能小于3", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            jigsaw.rowCount = jigsaw.rowCount - 1
            tv_row.text = jigsaw.rowCount.toString()
        }
        btn_row_plus.setOnClickListener {
            jigsaw.rowCount = jigsaw.rowCount + 1
            tv_row.text = jigsaw.rowCount.toString()
        }
    }
}