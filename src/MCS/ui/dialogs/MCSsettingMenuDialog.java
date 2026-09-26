package MCS.ui.dialogs;

import java.lang.reflect.*;
import MCS.game.*;
import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.ai.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static MCS.main.*;
import static MCS.game.enumClass.MCSeventType.*;
import static arc.Core.*;
import static mindustry.ui.dialogs.SettingsMenuDialog.*;
import static mindustry.Vars.*;

public class MCSsettingMenuDialog {
    private BaseDialog blockStringDialog, unitStringDialog, musicListDialog;
    private musicSquareSearchDialog musicSearchDialog;
    private ContentManagerDialog contentManagerDialog;

    public MCSsettingMenuDialog(){
        Events.on(MusicImportDialogShowEvent.class, e -> {
            new MusicImportDialog((e.from == null ? "@importMusic" : e.isCopied ? "@musicList.copy" : "@musicList.move"), e.from, e.isCopied).show();
        });
    }

    public Cons<SettingsTable> settingBuilder = t -> {
        t.pref(new TitleSetting("@settingtitle.music"));

        t.checkPref("instantChangeBossMusic", false);
        t.checkPref("enableMusicBar", false, b -> Events.fire(new MusicBarChangeEvent(b)));
        t.sliderPref("musicBarScl",100, 50, 200, 5, i -> i + "%", changed -> MCSui.musicBar.rebuild());
        t.checkPref("enableCustomMusic", false, b -> Events.fire(new CustomMusicChangeEvent(b)));
        t.pref(new ButtonSetting("@importMusic", Icon.play, () -> Events.fire(new MusicImportDialogShowEvent())));
        if(!mobile){
            t.pref(new ButtonSetting("@openMusicFolder", Icon.folder, () -> {
                if (!musicLoader.musicFolder.exists()) musicLoader.loadFolder();
                app.openFolder(musicLoader.musicFolder.absolutePath());
            }));
        }
        t.pref(new ButtonSetting("@clearMusic", Icon.trash,
                () -> ui.showConfirm("@clearMusic", "@clearMusic.confirm", () -> {
                    musicLoader.delete();
                    MCSui.musicBar.rebuild();
                })
        ));
        t.pref(new ButtonSetting("@musicList", Icon.list, () -> musicListDialog.show()));
        t.pref(new ButtonSetting("@musicSquare.search", Icon.zoom, () -> musicSearchDialog.show()));

        t.pref(new TitleSetting("@settingtitle.campaignDifficulty"));

        t.checkPref("enablecustomcampaigndifficulty", false, b -> {
            if(b){
                ui.campaignRules = new CustomCampaignRulesDialog();
                spawner = new CustomWaveSpawner();
                if(state.isCampaign() && !net.client()){
                    rulesMap.get(state.getPlanet()).apply(state.getPlanet(), state.rules);
                    Call.setRules(state.rules);
                }
            }else{
                ui.campaignRules = new CampaignRulesDialog();
                spawner = new WaveSpawner();
                if(state.isCampaign() && !net.client()){
                    new CustomCampaignRules(state.getPlanet()).apply(state.getPlanet(), state.rules);//TEST
                    state.getPlanet().campaignRules.apply(state.getPlanet(), state.rules);
                    Call.setRules(state.rules);
                }
            }
        });

        //forceCampaignDifficulty
        t.checkPref("forceCampaignDifficulty", false, b -> {
            if(b){
                for(var p : content.planets()){
                    if(!p.allowCampaignRules){
                        settings.put(p.name + "-forceCD", true);
                        p.allowCampaignRules = true;
                    }
                }
            }
            else{
                for(var p : content.planets()){
                    if(settings.getBool(p.name + "-forceCD")){
                        settings.remove(p.name + "-forceCD");
                        p.allowCampaignRules = false;
                    }
                }
            }
        });

        t.pref(new TitleSetting("@settingtitle.contentManager"));

        t.checkPref("enableContentManager", false, b -> Events.fire(new ContentManagerChangeEvent(b)));
        t.pref(new ButtonSetting("@contentManager", Icon.fileText, () -> contentManagerDialog.show()));

        t.pref(new TitleSetting("@settingtitle.buildAttacked"));

        t.checkPref("enableBuildAttackFrag", false, b -> MCSui.attacked.blockEnabled = b);
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> blockStringDialog.show()));
        t.checkPref("bannedAttackedBlocksWhitelist", false, b -> MCSui.attacked.blockWhitelist = b);
        t.pref(new ButtonSetting("@bannedAttackedBlocks", Icon.cancel, () -> MCSui.attacked.bannedAttackBlocksDialog.show(MCSui.attacked.bannedAttackBlocks)));

        t.pref(new TitleSetting("@settingtitle.unitAttacked"));

        t.checkPref("enableUnitAttackFrag", false, u -> MCSui.attacked.unitEnabled = u);
        t.sliderPref("unitHealthPercent", 50, 0, 100, 1, i -> i + "%");
        t.pref(new ButtonSetting("@editAttackedString", Icon.pencil, () -> unitStringDialog.show()));
        t.checkPref("bannedAttackedUnitsWhitelist", false, u -> MCSui.attacked.unitWhitelist = u);
        t.pref(new ButtonSetting("@bannedAttackedUnits", Icon.cancel, () -> MCSui.attacked.bannedAttackUnitsDialog.show(MCSui.attacked.bannedAttackUnits)));

        t.pref(new GithubLink("Github"));
    };

    public void load(){
        blockStringDialog = new BaseDialog("@settings");
        blockStringDialog.buttons.defaults().size(105f, 64f);
        blockStringDialog.cont.table(t -> {
            t.field(settings.getString("blockStringMCS", bundle.get("buildAttacked")), s -> MCSui.attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                MCSui.attacked.blockString = MCSui.attacked.tmpString;
                MCSui.attacked.blockChanged = true;
                settings.put("blockStringMCS", MCSui.attacked.blockString);
                blockStringDialog.hide();
            }).padLeft(10f);
            t.button("@back", Icon.left, blockStringDialog::hide).padLeft(10f);
        });
        blockStringDialog.addCloseListener();

        unitStringDialog = new BaseDialog("@settings");
        unitStringDialog.buttons.defaults().size(105f, 64f);
        unitStringDialog.cont.table(t -> {
            t.field(settings.getString("unitStringMCS", bundle.get("unitAttacked")), s -> MCSui.attacked.tmpString = s).width(400f).center().padLeft(10f);
            t.button("@confirm", Icon.ok, () -> {
                MCSui.attacked.unitString = MCSui.attacked.tmpString;
                MCSui.attacked.unitEnabled = true;
                settings.put("unitStringMCS", MCSui.attacked.unitString);
                unitStringDialog.hide();
            }).padLeft(10f);
            t.button("@back", Icon.left, unitStringDialog::hide).padLeft(10f);
        });
        unitStringDialog.addCloseListener();

        musicListDialog = new BaseDialog("@musicList"){{
            shown(() -> rebuildMusicList());
            onResize(() -> rebuildMusicList());
        }};
        musicListDialog.addCloseButton();

        musicSearchDialog = new musicSquareSearchDialog();
        musicSearchDialog.setup();

        contentManagerDialog = new ContentManagerDialog("@contentManager");

        try{
            var icon = Core.atlas.find("mcs-setting");
            icon.scale = 0.2f;
            ui.settings.addCategory(bundle.get("morecustomsettings"), new TextureRegionDrawable(icon).tint(Pal.accent), settingBuilder);
            replaceResetButton();

            Seq<Music> a = new Seq<>(control.sound.ambientMusic), b = new Seq<>(control.sound.bossMusic), d = new Seq<>(control.sound.darkMusic);
            control.sound.ambientMusic.clear();
            control.sound.darkMusic.clear();
            control.sound.bossMusic.clear();
            control.sound = new CustomSoundControl(){{
                ambientMusic = a;
                bossMusic = b;
                darkMusic = d;
            }};
        }catch(Exception ex){
            throw new RuntimeException(ex);
        }

        if(settings.getBool("enablecustomcampaigndifficulty")){
            ui.campaignRules = new CustomCampaignRulesDialog();
            spawner = new CustomWaveSpawner();
        }
    }

    private void buildMusicButtons(Table t, Music m, Runnable r){
        t.labelWrap(musicLoader.getMusicName(m.file)).left().fillX().expandX();
        t.button("@musicList.rename", Icon.pencilSmall, () -> new MusicRenameDialog("@musicList.rename", m).show()).padLeft(10);
        t.button("@musicList.move", Icon.moveSmall, () -> Events.fire(new MusicImportDialogShowEvent(m, false))).padLeft(10);
        t.button("@musicList.copy", Icon.copySmall, () -> Events.fire(new MusicImportDialogShowEvent(m, true))).padLeft(10);
        t.button("@delete", Icon.trashSmall, r).padLeft(10);
    }
    private void buildMusicList(Table t, String name){
        t.add("@importMusic." + name).color(Pal.accent).padTop(10f).left().row();
        boolean found = false;
        if(name.equals("planet")){
            t.image().color(Pal.accent).height(3).left().fillX().padBottom(3).row();
            for(var p : content.planets()){
                if(!p.accessible) continue;
                t.add("[#" + p.iconColor + "]" + Iconc.planet + p.localizedName).padTop(5).left().row();
                if(musicLoader.planetMusicMap.get(p) != null){
                    t.table(Styles.grayPanel, mt -> {
                        var m = musicLoader.planetMusicMap.get(p);
                        buildMusicButtons(mt, m, () -> {
                            settings.remove("MCSplanetMusicName-" + p.name);
                            m.file.delete();
                            musicLoader.planetMusicMap.remove(p);
                            musicLoader.load();
                            rebuildMusicList();
                        });
                    }).growX().left().row();
                }else{
                    t.add("@musicList.empty").padLeft(10).left().row();
                }
            }
            found = true;
        }else if(name.equals("menu") || name.equals("editor")){
            var m = new Music[]{ name.equals("menu") ? musicLoader.menuMusic : musicLoader.editorMusic };
            String settingName = name.equals("menu") ? "MCSmenuMusicName" : "MCSeditorMusicName";
            if(m[0] != null){
                t.table(Styles.grayPanel, mt -> {
                    buildMusicButtons(mt, m[0], () -> {
                        settings.remove(settingName);
                        m[0].file.delete();
                        musicLoader.load();
                        rebuildMusicList();
                    });
                }).growX().left().row();
                found = true;
            }
        }else{
            var seq = name.equals("ambient") ? musicLoader.ambientMusic : name.equals("dark") ? musicLoader.darkMusic : musicLoader.bossMusic;
            if(seq.size > 0){
                for(var m : seq){
                    t.table(Styles.grayPanel, mt -> {
                        buildMusicButtons(mt, m, () -> {
                            m.file.delete();
                            musicLoader.load();
                            MCSui.musicBar.rebuild();
                            rebuildMusicList();
                        });
                    }).growX().left().row();
                    found = true;
                }
            }
        }
        if(!found) t.add("@musicList.empty").padLeft(10).left().row();
    }
    public void rebuildMusicList(){
        musicListDialog.cont.clearChildren();
        musicListDialog.cont.pane(t -> {
            buildMusicList(t, "ambient");
            buildMusicList(t, "dark");
            buildMusicList(t, "boss");
            buildMusicList(t, "menu");
            buildMusicList(t, "editor");
            buildMusicList(t, "planet");
        }).width(graphics.getWidth() / Scl.scl() * 0.75f).growY(); //.growX().growY();
    }

    private void replaceResetButton(){
        var cat = ui.settings.getCategories().find(sc -> sc.name.equals(bundle.get("morecustomsettings")));
        cat.table = new SettingsTable(){
            @Override
            public void rebuild() {
                fooSetRebuilding(this, true);
                try{
                    clearChildren();
                    for(Setting setting : list){
                        setting.add(this);
                    }
                    button(bundle.get("settings.reset", "Reset to Defaults"), () -> {
                    for(Setting setting : list) {
                        if (setting.name != null && setting.title != null) {
                            settings.remove(setting.name);
                        }
                    }

                    musicLoader.reset();
                    rulesMap.reset();
                    MCSui.reset();

                    if(ui.campaignRules instanceof CustomCampaignRulesDialog) ui.campaignRules = new CampaignRulesDialog();
                    if(spawner instanceof CustomWaveSpawner) spawner = new WaveSpawner();

                    for(var p : content.planets()){
                        if(settings.getBool(p.name + "-forceCD")){
                            settings.remove(p.name + "-forceCD");
                            p.allowCampaignRules = false;
                        }
                    }

                    rebuild();
                }).margin(14f).width(240f).pad(6f);
                }finally {
                    fooSetRebuilding(this, false);
                }
            }
        };
        cat.builder.get(cat.table);
    }

    private static void fooSetRebuilding(SettingsTable table, boolean value){ //Fixed crush in Foo
        if(!isFoo) return;
        try {
            Field field = SettingsTable.class.getDeclaredField("isRebuilding");
            field.setAccessible(true);
            field.setBoolean(table, value);
        }catch(Throwable ignored) {}
    }

    /**
     * Dialogs
     */
    public static class MusicImportDialog extends BaseDialog{
        @Nullable Music from;
        private boolean isCopied;
        private static BaseDialog musicInGameDialog, planetMusicListDialog;

        public MusicImportDialog(String title, Music from, boolean isCopied) {
            super(title);
            this.from = from;
            this.isCopied = isCopied;
            addCloseButton();

            musicInGameDialog = new BaseDialog(title);
            musicInGameDialog.addCloseButton();
            musicInGameDialog.cont.table(Tex.button, t -> {
                t.defaults().size(200f, 60f).left();

                t.button("@importMusic.ambient", Styles.flatt, () -> {
                    Events.fire(new ImportMusicEvent(from,"a", isCopied));
                    musicInGameDialog.hide();
                    hide();
                }).disabled(b -> from != null && from.file.parent().name().equals("a"));
                t.row();
                t.button("@importMusic.dark", Styles.flatt, () -> {
                    Events.fire(new ImportMusicEvent(from,"d", isCopied));
                    musicInGameDialog.hide();
                    hide();
                }).disabled(b -> from != null && from.file.parent().name().equals("d"));
                t.row();
                t.button("@importMusic.boss", Styles.flatt, () -> {
                    Events.fire(new ImportMusicEvent(from,"b", isCopied));
                    musicInGameDialog.hide();
                    hide();
                }).disabled(b -> from != null && from.file.parent().name().equals("b"));
                t.row();
            });
            planetMusicListDialog = new BaseDialog(title);
            planetMusicListDialog.addCloseButton();
            planetMusicListDialog.cont.pane(t -> {
                t.defaults().size(200f, 60f).left();

                for(var planet : content.planets()){
                    if(!planet.accessible) continue;
                    t.button(planet.localizedName, Icon.planet.tint(planet.iconColor), () -> {
                        Events.fire(new ImportNamedMusicEvent(from, planet.name, isCopied));
                        planetMusicListDialog.hide();
                        hide();
                    }).disabled(b -> from != null && musicLoader.getFileName(from.file).equals(planet.name));
                    t.row();
                }
            });

            cont.table(Tex.button, t -> {
                t.defaults().size(200f, 60f).left();

                t.button("@importMusic.inGame", Styles.flatt, () -> musicInGameDialog.show());
                t.row();
                t.button("@importMusic.menu", Styles.flatt, () -> {
                    Events.fire(new ImportNamedMusicEvent(from, "menu", isCopied));
                    hide();
                }).disabled(b -> from != null && musicLoader.getFileName(from.file).equals("menu"));
                t.row();
                t.button("@importMusic.editor", Styles.flatt, () -> {
                    Events.fire(new ImportNamedMusicEvent(from, "editor", isCopied));
                    hide();
                }).disabled(b -> from != null && musicLoader.getFileName(from.file).equals("editor"));
                t.row();
                t.button("@importMusic.planet", Styles.flatt, () -> planetMusicListDialog.show());
                t.row();
            });
        }
    }
    public static class MusicRenameDialog extends BaseDialog{
        Music music;
        String tmpMusicName = "";

        public MusicRenameDialog(String title, Music music){
            super(title);
            this.music = music;

            buttons.defaults().size(105f, 64f);
            cont.table(t -> {
                t.field(musicLoader.getMusicName(music.file), s -> tmpMusicName = s).width(Math.max(graphics.getWidth() / 3f / Scl.scl(1f), 400f / Scl.scl(1f))).center().padLeft(10f);
                t.button("@confirm", Icon.ok, () -> {
                    musicLoader.renameMusic(music, tmpMusicName);
                    MCSui.menu.rebuildMusicList();
                    MCSui.musicBar.rebuild();
                    hide();
                }).padLeft(10f);
                t.button("@back", Icon.left, this::hide).padLeft(10f);
            });
            addCloseListener();
        }
    }

    /**
     * Settings
     */
    public static class TitleSetting extends SettingsTable.Setting {
        public TitleSetting(String text) {
            super("");
            this.title = text;
        }

        public void add(SettingsTable table) {
            table.add(this.title).color(Pal.accent).padTop(25.0F).padRight(110.0F).padBottom(-5.0F).left().pad(5.0F);
            table.row();
            table.image().color(Pal.accent).height(3.0F).padRight(110.0F).padBottom(25.0F).left().fillX().padBottom(5.0F);
            table.row();
        }
    }
    public static class ButtonSetting extends SettingsTable.Setting{
        @Nullable Drawable icon;
        @Nullable Runnable onClick;
        public ButtonSetting(String name) {
            super(name);
        }

        public ButtonSetting(String name, Drawable icon, Runnable onClick){
            this(name);
            this.icon = icon;
            this.onClick = onClick;
        }

        @Override
        public void add(SettingsTable table) {
            table.button(name, icon, onClick).marginLeft(5f).growX().row();
        }
    }

    public static class GithubLink extends SettingsTable.Setting{
        public GithubLink(String name) { super(name); }

        @Override
        public void add(SettingsTable table) {
            table.add(new Table(t -> {
                t.button(Icon.github, new ImageButton.ImageButtonStyle(), () -> {
                    String url = "https://github.com/lu69159/MoreCustomSettings";
                    if (!app.openURI(url)) {
                        ui.showInfoFade("@linkfail");
                        app.setClipboardText(url);
                    }
                });
            })).row();
        }
    }
}
