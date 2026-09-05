package MCS.ui;

import MCS.ui.dialogs.*;
import MCS.ui.fragments.*;

public class MCSUI{
    public MCSsettingMenuDialog menu;
    public CustomAttackFrag attacked;
    public MusicBar musicBar;

    public MCSUI(){
        menu = new MCSsettingMenuDialog();
        attacked = new CustomAttackFrag();
        musicBar = new MusicBar();
    }

    public void load(){
        attacked.load();
        menu.load();
    }

    public void reset(){
        attacked.reset();
        musicBar.reset();
    }
}
