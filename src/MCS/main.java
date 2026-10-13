package MCS;

import MCS.customContent.*;
import MCS.customDifficulty.*;
import MCS.customMusic.*;
import MCS.ui.*;
import arc.*;
import arc.files.*;
import arc.func.*;
import arc.struct.*;
import mindustry.mod.*;
import java.lang.reflect.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;

public class main extends Mod{
    public static Fi saveFolder = dataDirectory.child("MCS-data");
    public static PlanetCustomRulesMap rulesMap;
    public static ContentManager contentManager;
    public static MCS_UI MCSui;

    public static boolean isFoo = false;

    public main(){
        rulesMap = new PlanetCustomRulesMap();
        contentManager = new ContentManager();
        MCSui = new MCS_UI();

        Events.run(ClientLoadEvent.class, () -> {
            isFoo();
            replace();
            if(!saveFolder.exists()) saveFolder.mkdirs();
            rulesMap.load();
            contentManager.load();
            MCSui.load();
        });
    }

    public void isFoo(){
        try{
            Class.forName("mindustry.client.Main", false, getClass().getClassLoader());
            isFoo = true;
        }catch(Throwable t){
            isFoo = false;
        }
    }

    private void replace(){
        control.sound.ambientMusic.clear();
        control.sound.darkMusic.clear();
        control.sound.bossMusic.clear();
        removeAllListener(control.sound.getClass().getName());
        control.sound = new CustomSoundControl();
    }

    /**
     * tool functions
     */
    @SuppressWarnings("unchecked")
    public static void removeAllListener(String keyWord){
        if(isFoo) return;
        try{
            Field eventsField = Events.class.getDeclaredField("events");
            eventsField.setAccessible(true);
            ObjectMap<Object, Seq<Cons<?>>> events = (ObjectMap<Object, Seq<Cons<?>>>)eventsField.get(null);

            for(var current : events.values()){
                Seq<Cons<?>> targets = new Seq<>();
                for(var listener : current){
                    if(listener.getClass().getName().startsWith(keyWord)){
                        targets.add(listener);
                    }
                }
                Core.app.post(() -> current.removeAll(targets));
            }
        }catch(Exception e){
            ui.showException(e);
        }
    }
}
