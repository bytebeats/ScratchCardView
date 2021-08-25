package me.bytebeats.scratchcard.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import me.bytebeats.views.scratch.OnScratchListener
import me.bytebeats.views.scratch.ScratchCardView
import me.bytebeats.views.scratch.ScratchImageView
import me.bytebeats.views.scratch.ScratchTextView
import me.bytebeats.views.scratchcard.app.R

class MainActivity : AppCompatActivity() {
    private val scratchCardView by lazy { findViewById<ScratchCardView>(R.id.scratch_card_view) }
    private val scratchTextView by lazy { findViewById<ScratchTextView>(R.id.scratch_text_view) }
    private val scratchImageView by lazy { findViewById<ScratchImageView>(R.id.scratch_image_view) }
    private val reset by lazy { findViewById<Button>(R.id.btn_reset) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        scratchCardView.onScratchListener = object : ScratchCardView.OnScratchListener {
            override fun onScratch(cardView: ScratchCardView?, visiblePercent: Float) {
                Log.i(TAG, "percent: $visiblePercent")
            }
        }
        scratchTextView.onScratchListener = object : OnScratchListener<ScratchTextView> {
            override fun onScratchChanged(view: ScratchTextView, visiblePercent: Float) {
                Log.i(TAG, "text percent: $visiblePercent")
            }

            override fun onRevealed(view: ScratchTextView) {
                Log.i(TAG, "text revealed")
                view.reveal()
            }
        }
        scratchImageView.onScratchListener = object : OnScratchListener<ScratchImageView> {
            override fun onScratchChanged(view: ScratchImageView, visiblePercent: Float) {
                Log.i(TAG, "image percent: $visiblePercent")
            }

            override fun onRevealed(view: ScratchImageView) {
                Log.i(TAG, "image revealed")
                view.reveal()
            }
        }
        reset.setOnClickListener {
            scratchTextView.reveal()
            scratchImageView.reveal()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}