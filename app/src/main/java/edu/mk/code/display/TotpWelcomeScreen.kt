package edu.mk.code.display

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.scottyab.rootbeer.RootBeer
import edu.mk.code.R
import edu.mk.code.prompts.AlertPrompt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TotpWelcomeScreen : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetIn()
        }
    }

    @Composable
    private fun GetIn() {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val safe = !RootBeer(this@TotpWelcomeScreen).isRooted
                    if (safe) {
                        val intent = Intent(this@TotpWelcomeScreen, TotpListActivity::class.java)
                        startActivity(intent)
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            AlertPrompt(this@TotpWelcomeScreen)
                                .inform(R.string.get_in_unsafe_title, R.string.get_in_unsafe_message)
                        }
                    }
                },
            ) {
                Text(getString(R.string.get_in_button))
            }
        }
    }
}
