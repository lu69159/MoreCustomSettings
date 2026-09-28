package MCS.ui;

import MCS.ui.dialogs.*;
import MCS.ui.fragments.*;

public class MCS_UI {
    public MCS_SettingMenuDialog menu;
    public CustomAttackFrag attacked;
    public MusicBar musicBar;

    public MCS_UI(){
        menu = new MCS_SettingMenuDialog();
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
