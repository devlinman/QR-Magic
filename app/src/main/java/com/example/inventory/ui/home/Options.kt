package com.example.inventory.ui.home

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity

class DBOptions()
fun FragmentActivity.openFilePicker(onFileChosen: (Uri) -> Unit) {
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "text/csv" // Set the MIME type to filter CSV files

    val chooseFile = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                onFileChosen(it)
                Toast.makeText(this, "Selected file: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    chooseFile.launch(intent)
}