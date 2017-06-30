package lijunguan.github.io.emojirain

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Action1
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    var flag = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val subscribe = Observable.interval(0, 8000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(Action1 {
                    if (flag) {
                        flag = !flag
                        emoji_rain_layout.setEmoji(R.drawable.ic_face_black_36dp)
                    } else {
                        flag = !flag
                        emoji_rain_layout.setEmoji(R.drawable.ic_face_red_a200_36dp)
                    }
                    emoji_rain_layout.startDropping()
                })
    }


}

