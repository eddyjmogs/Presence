package com.eddy.presence.ui.reminder

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.eddy.presence.ui.theme.PresenceTheme

class FocusSoftReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UI implemented in Step 12
    }

    companion object {
        private const val EXTRA_CONTEXT_NAME = "extra_context_name"

        fun launch(context: Context, contextName: String) {
            context.startActivity(
                Intent(context, FocusSoftReminderActivity::class.java)
                    .putExtra(EXTRA_CONTEXT_NAME, contextName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }
}
