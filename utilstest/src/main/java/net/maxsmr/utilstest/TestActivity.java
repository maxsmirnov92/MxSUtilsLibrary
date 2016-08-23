package net.maxsmr.utilstest;

import me.ilich.juggler.gui.JugglerActivity;
import me.ilich.juggler.states.State;
import me.ilich.juggler.states.VoidParams;

public class TestActivity extends JugglerActivity {


    @Override
    protected State createState() {
        return new TState(VoidParams.instance());
    }
}
