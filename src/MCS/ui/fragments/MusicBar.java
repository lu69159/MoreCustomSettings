package MCS.ui.fragments;

import MCS.game.CustomSoundControl;
import MCS.game.enumClass.*;
import arc.Events;
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
import static MCS.main.*;

public class MusicBar{
    public boolean posted = false;
    public boolean openList = false;
    private float barX = -1f, barY = -1f;
    private Table bar, list;

    public MusicBar(){
        Events.on(EventType.WorldLoadEvent.class, e -> {
            if(!posted){
                app.post(() -> {
                    build(ui.hudGroup);
                });
                posted = true;
            }
        });
    }

    private boolean shouldUseSlider(){
        return control.sound.getCurrent() != null && musicLoader.allInGameMusic.contains(control.sound.getCurrent());
    }

    public void build(Group parent){
        ImageButton moveButton = new ImageButton(Icon.move, Styles.clearNonei);
        moveButton.touchable = Touchable.enabled;

        Slider disabledMusicSlider = new Slider(0f, 0f, 1f, false);
        disabledMusicSlider.visible(() -> !shouldUseSlider());

        Slider musicSlider = new Slider(0f, shouldUseSlider() ? control.sound.getCurrent().getLength() : 0f, 0.1f, false);
        musicSlider.moved(value -> {
            if(control.sound.getCurrent() != null) control.sound.getCurrent().setPosition(value);
        });
        musicSlider.update(() -> {
            musicSlider.setRange(0f, shouldUseSlider() ? control.sound.getCurrent().getLength() : 0f);
            if(!musicSlider.isDragging()) musicSlider.setValue(shouldUseSlider() ? control.sound.getCurrent().getPosition() : 0f, false);
        });
        musicSlider.visible(this::shouldUseSlider);

        Label musicTimeLabel = new Label(() -> musicSlider.visible ? UI.formatTime(musicSlider.getValue() * 60f) : UI.formatTime(0));
        musicTimeLabel.setAlignment(Align.center);
        musicTimeLabel.setStyle(Styles.outlineLabel);
        musicTimeLabel.touchable = Touchable.disabled;

        float barScl = settings.getInt("musicBarScl", 100)/100f;
        Table musicBarTable = new Table(){{
            setWidth(barScl * Scl.scl(510f));
            setHeight(barScl * Scl.scl(150f));
            x = barX < 0 ? settings.getFloat("MCS-musicBarX",graphics.getWidth() / 4f)  : barX;
            y = barY < 0 ? settings.getFloat("MCS-musicBarY",graphics.getHeight() * 7/8f) : barY;
            background(Styles.black3);
            labelWrap(() -> control.sound.getCurrent() == null ? ((CustomSoundControl)control.sound).getLastRandomPlayed() == null ?
                    "@empty" : musicLoader.getFileName(((CustomSoundControl)control.sound).getLastRandomPlayed().file) : musicLoader.getFileName(control.sound.getCurrent().file)).padLeft(10f).padRight(10f).growX().left().row();
            stack(disabledMusicSlider, musicSlider, musicTimeLabel).padLeft(10f).padRight(10f).growX().row();
            table(buttons -> {
                buttons.defaults().size(barScl * 60f);

                buttons.button(Icon.leftOpen, Styles.clearNonei, () -> {
                    var sound = (CustomSoundControl)control.sound;
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = musicLoader.allInGameMusic.indexOf(m) - 1;
                        if(index < -1){
                            sound.playMusic(musicLoader.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index == -1 ? musicLoader.allInGameMusic.size - 1 : index;
                            sound.playMusic(musicLoader.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(10f);

                var stylePlay = new ImageButton.ImageButtonStyle(Styles.clearNonei);
                stylePlay.imageUp = Icon.play;
                stylePlay.imageChecked = Icon.pause;
                buttons.button(Icon.play, stylePlay, () -> {
                    if(state.rules.disableMusic){
                        state.rules.disableMusic = false;
                        if(control.sound.getCurrent() == null){
                            var sound = (CustomSoundControl)control.sound;
                            if((sound.getLastRandomPlayed() == null)) sound.playRandom();
                            else sound.playMusic(sound.getLastRandomPlayed(),true);
                        }
                    }else if(control.sound.getCurrent() == null){
                        control.sound.playRandom();
                    }else{
                        state.rules.disableMusic = true;
                        control.sound.getCurrent().stop();
                    }

                }).checked(chk -> !state.rules.disableMusic && control.sound.getCurrent() != null).left().padRight(10f);

                buttons.button(Icon.rightOpen, Styles.clearNonei, () -> {
                    var sound = (CustomSoundControl)control.sound;
                    var m = sound.getCurrent() == null ? sound.getLastRandomPlayed() == null ? null : sound.getLastRandomPlayed() : sound.getCurrent();

                    if(state.rules.disableMusic) state.rules.disableMusic = false;
                    if(m == null){
                        sound.playRandom();
                    }else{
                        int index = musicLoader.allInGameMusic.indexOf(m) + 1;
                        if(index < 1){
                            sound.playMusic(musicLoader.allInGameMusic.random(),true);
                        }else{
                            int nextIndex = index < musicLoader.allInGameMusic.size ? index : 0;
                            sound.playMusic(musicLoader.allInGameMusic.get(nextIndex), true);
                        }
                    }
                }).disabled(dis -> settings.getBool("instantChangeBossMusic", false) && state.boss() != null).left().padRight(10f);

                buttons.button(Icon.menu, Styles.clearNonei, () -> {
                    openList = !openList;
                }).checked(chk -> openList).left().padRight(10f);

                buttons.button(((CustomSoundControl)control.sound).mode.icon, Styles.clearNonei, () -> {
                    var sound = (CustomSoundControl)control.sound;
                    sound.mode = MusicMode.values()[(sound.mode.ordinal() + 1) % MusicMode.values().length];
                    settings.put("MCS-musicMode", sound.mode.name());
                }).tooltip(t -> {
                    t.label(() -> ((CustomSoundControl)control.sound).mode.toolTip());
                })
                .update(b -> b.getStyle().imageUp = ((CustomSoundControl)control.sound).mode.icon).left().padRight(10f);;

                buttons.add(moveButton).tooltip("@musicBar.dragToMove", true).left();
            }).width(barScl * (6*60f + 5*10f)).fillY().center();
            visible(() -> settings.getBool("enableMusicBar", false) && state.isGame() && !state.isEditor());
        }};

        if(musicBarTable.y > parent.getHeight() - musicBarTable.getHeight()){
            musicBarTable.y = parent.getHeight() - musicBarTable.getHeight();
            barY = musicBarTable.y;
        }
        if(musicBarTable.x > parent.getWidth() - musicBarTable.getWidth()){
            musicBarTable.x = parent.getWidth() - musicBarTable.getWidth();
            barX = musicBarTable.x;
        }

        Table musicListTable = new Table(){{
            setWidth(barScl * Scl.scl(510f));
            float h = musicLoader.allInGameMusic.size == 0 ? Math.min(Scl.scl(32f), graphics.getHeight() / 4f) : Math.min(musicLoader.allInGameMusic.size * Scl.scl(32f), graphics.getHeight() / 4f);
            setHeight(h);
            x = musicBarTable.x;
            y = musicBarTable.y - h;
            pane(list -> {
                list.background(Styles.black5);
                boolean found = false;
                for(var music : musicLoader.allInGameMusic){
                    String name = musicLoader.getFileName(music.file);
                    list.table(Styles.none, mt -> {
                        mt.labelWrap(name).left().fillX().expandX();
                        mt.button(Icon.play, Styles.clearNonei, () -> {
                            if(state.rules.disableMusic) state.rules.disableMusic = false;
                            control.sound.playMusic(music, true);
                        }).height(32f).disabled(dis -> control.sound.getCurrent() == music || (settings.getBool("instantChangeBossMusic", false) && state.boss() != null)).padLeft(10);
                    }).growX().left().row();
                    found = true;
                }
                if(!found) list.table(Styles.none, mt -> mt.labelWrap("@musicList.empty")).growX().left().row();

            }).scrollX(false).visible(() -> musicBarTable.visible && openList).grow();
        }};

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

                if(musicBarTable.y > parent.getHeight() / 2f){
                    musicListTable.y = musicBarTable.y - musicListTable.getHeight();
                }else{
                    musicListTable.y = musicBarTable.y + musicBarTable.getHeight();
                }
                if(musicListTable.x != musicBarTable.x) musicListTable.x = musicBarTable.x;
                settings.putFloat("MCS-musicBarX", barX);
                settings.putFloat("MCS-musicBarY", barY);
            }
        }));

        bar = musicBarTable;
        list = musicListTable;

        parent.addChild(musicBarTable);
        parent.addChild(musicListTable);
    }

    public void reload(){
        if(posted){
            app.post(() -> {
                ui.hudGroup.removeChild(bar);
                ui.hudGroup.removeChild(list);
                build(ui.hudGroup);
            });
        }
    }

    public void reset(){
        settings.remove("MCS-musicBarX");
        settings.remove("MCS-musicBarY");
    }
}
