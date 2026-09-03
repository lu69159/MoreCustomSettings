package MCS.game.enumClass;

import arc.Core;

public enum ContentManageMode{
    planet,
    mod;

    public static final ContentManageMode[] all = values();

    public String localized(){
        return Core.bundle.get("contentManager.mode." + name());
    }
    public String toolTip(){return Core.bundle.get("contentManager.mode." + name() + ".toolTip"); }
}
