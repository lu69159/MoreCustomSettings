package MCS.game.enumClass;

import arc.Core;
import arc.scene.style.*;
import mindustry.gen.Icon;

public enum MusicMode{
    seq, //顺序
    loop, //循环
    shuf, //乱序
    normal;

    public final Drawable icon;

    MusicMode(){
        icon = this.name().equals("normal") ? Icon.none : new TextureRegionDrawable(Core.atlas.find("mcs-" + this.name()));
    }

    public String toolTip(){
        return Core.bundle.get("musicBar." + name());
    }
}
