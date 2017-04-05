package net.maxsmr.commonutils.android.gui.views;

import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;

public class TouchCheckChangeDisabler implements CompoundButton.OnCheckedChangeListener, View.OnTouchListener {

        protected final CompoundButton buttonView;

        protected boolean wasChecked;

        boolean isTouched = false;

        public TouchCheckChangeDisabler(CompoundButton buttonView) {
            this.buttonView = buttonView;
            this.wasChecked = buttonView.isChecked();
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            isTouched = true;
            return false;
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (buttonView != this.buttonView) {
                throw new RuntimeException("incorrect buttonView: " + buttonView);
            }
            if (isTouched) {
                if (isChecked != wasChecked) {
                    buttonView.setChecked(wasChecked);
                }
                isTouched = false;
            }
            wasChecked = buttonView.isChecked();
        }
    }