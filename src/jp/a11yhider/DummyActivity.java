package jp.a11yhider;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class DummyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.setPadding(48, 96, 48, 48);
        view.setTextSize(16);
        view.setText(
                "A11yHider has no built-in app list.\n\n" +
                "Open your LSPosed/Vector manager, find A11yHider "
                + "in the module list, and add the apps you want to "
                + "target to its scope. No rebuild needed."
        );
        setContentView(view);
    }
}
