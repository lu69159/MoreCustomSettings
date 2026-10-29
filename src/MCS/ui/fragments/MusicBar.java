package MCS.ui.fragments;

import MCS.MCS_EventType;
import MCS.customMusic.*;
import arc.Events;
import arc.math.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class MusicBar{
    public boolean openList = false, openVol = false;
    private float barX = -1f, barY = -1f;
    private Table bar, list, vol;

    public MusicBar(){
        Events.run(EventType.WorldLoadEvent.class, this::rebuild);
        Events.run(EventType.ResizeEvent.class, this::rebuild);
        Events.run(MCS_EventType.RebuildMusicBarEvent.class, this::rebuild);
    }

    public void build(Group parent){
        var sound = (CustomSoundControl)control.sound;

        ImageButton moveButton = new ImageButton(Icon.move, Styles.clearNonei){{
            touchable = Touchable.enabled;
        }};
        Slider musicSlider = new Slider(0f, sound.getCurrent() == null ? 0f : sound.getCurrent().getLength(), 0.1f, false){{
            changed(() -> {
                if(sound.getCurrent() != null && !isDragging()) sound.getCurrent().setPosition(getValue());
            });
            update(() -> {
                setRange(0f, sound.getCurrent() == null ? 0f : sound.getCurrent().getLength());
                if(!isDragging()) setValue(sound.getCurrent() == null ? 0f : sound.getCurrent().getPosition(), false);
            });
            visible(() -> sound.getCurrent() != null && settings.getInt("musicvol") > 0);
        }};
        Slider disabledMusicSlider = new Slider(0f, 0f, 1f, false){{
            touchable = Touchable.disabled;
            visible(() -> !musicSlider.visible);
        }};
        Label musicTimeLabel = new Label(() -> musicSlider.visible ? UI.formatTime(musicSlider.getValue() * 60f) : UI.formatTime(0)){{
            touchable = Touchable.disabled;
            setAlignment(Align.center);
            setStyle(Styles.outlineLabel);
        }};
        Slider volSlider = new Slider(0, 500, 1, false){{
            moved(value -> state.rules.musicVolume = ((int)value) / 100f);
            update(() -> {
                if(!isDragging()) setValue(state.rules.musicVolume * 100);
            });
        }};
        Label volLabel = new Label(() -> (int)volSlider.getValue() + "%"){{
            touchable = Touchable.disabled;
            setAlignment(Align.center);
            setStyle(Styles.outlineLabel);
        }};

        float barScl = settings.getInt("musicBarScl", 100)/100f;
        Table musicBarTable = new Table(){{
            setWidth(barScl * Scl.scl(580f));
            setHeight(barScl * Scl.scl(170f));
            x = barX < 0 ? settings.getFloat("MCS-musicBarX",graphics.getWidth() / 4f)  : barX;
            y = barY < 0 ? settings.getFloat("MCS-musicBarY",graphics.getHeight() * 7/8f) : barY;
            background(Styles.black3);
            labelWrap(() -> sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ?
                    "@empty" : sound.musicLoader.getMusicName(sound.getLastRandomPlayed().file) : sound.musicLoader.getMusicName(sound.getCurrent().file)).padLeft(10f).padRight(10f).growX().left().row();
            stack(disabledMusicSlider, musicSlider, musicTimeLabel).touchable(() -> {
                if(settings.getBool("instantChangeBossMusic", false) && state.boss() != null) return Touchable.disabled;
                else return Touchable.childrenOnly;
            }).padLeft(10f).padRight(10f).growX().row();
            table(buttons -> {
                buttons.defaults().size(barScl * 60f);

                buttons.button(Icon.leftOpen, Styles.clearNonei, () -> {
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = sound.allInGameMusic.indexOf(m) - 1;
                        if(index < -1){
                            sound.playMusic(sound.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index == -1 ? sound.allInGameMusic.size - 1 : index;
                            sound.playMusic(sound.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(10f);

                var stylePlay = new ImageButton.ImageButtonStyle(Styles.clearNonei);
                stylePlay.imageUp = Icon.play;
                stylePlay.imageChecked = Icon.pause;
                buttons.button(Icon.play, stylePlay, () -> {
                    if(state.rules.disableMusic){
                        state.rules.disableMusic = false;
                        if(sound.getCurrent() == null){
                            if((sound.getLastRandomPlayed() == null)) sound.playRandom();
                            else sound.playMusic(sound.getLastRandomPlayed(),true);
                        }
                    }else if(sound.getCurrent() == null){
                        sound.playRandom();
                    }else{
                        state.rules.disableMusic = true;
                        sound.getCurrent().stop();
                    }

                }).checked(chk -> !state.rules.disableMusic && sound.getCurrent() != null).left().padRight(10f);

                buttons.button(Icon.rightOpen, Styles.clearNonei, () -> {
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = sound.allInGameMusic.indexOf(m) + 1;
                        if(index < 1){
                            sound.playMusic(sound.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index < sound.allInGameMusic.size ? index : 0;
                            sound.playMusic(sound.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(10f);

                buttons.button(Icon.menu, Styles.clearNonei, () -> {
                    openVol = false;
                    openList = !openList;
                }).checked(chk -> openList).left().padRight(10f);

                buttons.button(sound.mode.icon, Styles.clearNonei, () -> {
                    sound.mode = MusicMode.values()[(sound.mode.ordinal() + 1) % MusicMode.values().length];
                    settings.put("MCS-musicMode", sound.mode.name());
                }).tooltip(t -> {
                    t.label(() -> sound.mode.toolTip());
                }).update(b -> b.getStyle().imageUp = sound.mode.icon).left().padRight(10f);;

                buttons.button(Icon.volumeDown, Styles.clearNonei, () -> {
                    openList = false;
                    openVol = !openVol;
                }).checked(chk -> openVol).update(b -> {
                    float v = state.rules.musicVolume;
                    b.getStyle().imageUp = v==0 ? Icon.volumeOff : v<1.5f ? Icon.volumeDown : Icon.volumeUp;
                }).tooltip("@musicBar.ruleVol", true).left().padRight(10f);

                buttons.add(moveButton).tooltip("@musicBar.dragToMove", true).left();
            }).width(barScl * (7*60f + 6*10f)).fillY().center();
            visible(() -> ui.hudfrag.shown && settings.getBool("enableMusicBar", false) && state.isGame() && !state.isEditor());
        }};

        if(musicBarTable.y > parent.getHeight() - musicBarTable.getHeight()){
            musicBarTable.y = parent.getHeight() - musicBarTable.getHeight();
            barY = musicBarTable.y;
        }
        if(musicBarTable.x > parent.getWidth() - musicBarTable.getWidth()){
            musicBarTable.x = parent.getWidth() - musicBarTable.getWidth();
            barX = musicBarTable.x;
        }
        Table musicVolTable = new Table(){{
            setWidth(musicBarTable.getWidth());
            setHeight(Scl.scl(60f));
            x = musicBarTable.x;
            y = musicBarTable.y - getHeight();
            background(Styles.black5);
            stack(volSlider, volLabel).padLeft(10f).padRight(10f).growX();
            visible(() -> musicBarTable.visible && openVol);
        }};

        Table musicListTable = new Table(){{
            setWidth(musicBarTable.getWidth());
            float h = sound.allInGameMusic.size == 0 ? Math.min(Scl.scl(32f), graphics.getHeight() / 4f) : Math.min(sound.allInGameMusic.size * Scl.scl(32f), graphics.getHeight() / 4f);
            setHeight(h);
            x = musicBarTable.x;
            y = musicBarTable.y - h;
            pane(list -> {
                list.background(Styles.black5);
                boolean found = false;
                for(var music : sound.allInGameMusic){
                    String name = sound.musicLoader.getMusicName(music.file);
                    list.table(Styles.none, mt -> {
                        mt.labelWrap(name).left().fillX().expandX();
                        mt.button(Icon.play, Styles.clearNonei, () -> {
                            if(state.rules.disableMusic) state.rules.disableMusic = false;
                            sound.playMusic(music, true);
                        }).height(32f).disabled(dis -> sound.getCurrent() == music || (settings.getBool("instantChangeBossMusic", false) && state.boss() != null)).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
                if(!found) list.table(Styles.none, mt -> mt.labelWrap("@musicList.empty")).growX().left().row();

            }).scrollX(false).grow();
            visible(() -> musicBarTable.visible && openList);
        }};

        bar = musicBarTable;
        list = musicListTable;
        vol = musicVolTable;

        moveButton.addListener((new ElementGestureListener(){
            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                musicBarTable.moveBy(deltaX, deltaY);

                if(musicBarTable.y > parent.getHeight() - musicBarTable.getHeight()){
                    musicBarTable.y = parent.getHeight() - musicBarTable.getHeight();
                }else if(musicBarTable.y < 0){
                    musicBarTable.y = 0;
                }
                if(musicBarTable.x > parent.getWidth() - musicBarTable.getWidth()){
                    musicBarTable.x = parent.getWidth() - musicBarTable.getWidth();
                }else if(musicBarTable.x < 0){
                    musicBarTable.x = 0;
                }

                barX = musicBarTable.x;
                barY = musicBarTable.y;
                limit(parent);
                settings.putFloat("MCS-musicBarX", barX);
                settings.putFloat("MCS-musicBarY", barY);
            }
        }));

        parent.addChild(musicBarTable);
        parent.addChild(musicListTable);
        parent.addChild(musicVolTable);
        limit(parent);
    }

    public void rebuild(){
        app.post(() -> {
            ui.hudGroup.removeChild(bar);
            ui.hudGroup.removeChild(list);
            ui.hudGroup.removeChild(vol);
            build(ui.hudGroup);
        });

    }

    public void reset(){
        settings.remove("MCS-musicBarX");
        settings.remove("MCS-musicBarY");
        rebuild();
    }

    private void limit(Group parent){
        if(bar.y > parent.getHeight() / 2f){
            list.y = bar.y - list.getHeight();
            vol.y = bar.y - vol.getHeight();
        }else{
            list.y = bar.y + bar.getHeight();
            vol.y = bar.y + bar.getHeight();
        }
        if(list.x != bar.x) list.x = bar.x;
        if(vol.x != bar.x) vol.x = bar.x;
    }
}
