package MCS.ui.dialogs;

import MCS.customMusic.CustomSoundControl;
import MCS.musicSquare.sources.*;
import arc.*;
import arc.audio.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;
import static MCS.main.*;

public class musicSquareSearchDialog extends BaseDialog {
    ProviderHub resource = new ProviderHub();
    String previewingUrl = "";
    Image loadingSpinner;
    Drawable loading;
    private Table resultTable, sourceTable;
    private Button sourceButton;
    private TextField searchField;
    private String word = "";
    private final Seq<Texture> coverTextures = new Seq<>();
    private int coverGeneration;
    private boolean isLoading = false, shownSource = false;

    public musicSquareSearchDialog() {
        super("@musicSquare.search");
        loading = new TextureRegionDrawable(Core.atlas.find("mcs-loading"));
        setup();
        addCloseButton();
        SearchSettingsDialog settings = new SearchSettingsDialog();
        buttons.button("@musicSquare.searchSettings", Icon.settings, settings::show);
        setFillParent(true);
        resource.onSearchBeginning = this::loading;
        resource.onSearchComplete = this::complete;
        hidden(() -> {
            stopPreviewListening();
            searchField.setText("");
            word = "";
            resource.allResults.clear();
            resultTable.clear();
            clearCoverTextures();
            var sound = (CustomSoundControl)control.sound;
            sound.musicLoader.tmp.deleteDirectory();
            sound.musicLoader.tmp.mkdirs();
        });
    }

    public void setup(){
        cont.table(t -> {
            t.left();
            t.image(Icon.zoom).padRight(8f);
            searchField = t.field("", text -> word = text).growX().get();
            if(!mobile){
                searchField.keyDown(KeyCode.enter, this::search);
            }
            t.button("@musicSquare.searchMusic", Icon.zoom, this::search).size(100f, 50f);

            sourceButton = t.button("@musicSquare.searchProvider", Icon.downOpen, () -> {
                shownSource = !shownSource;
                if(sourceTable == null){
                    sourceTable = new Table(Styles.black3){{
                        setWidth(Scl.scl(200f));
                        setHeight(resource.all.size * 60f);
                    }}.left().top();
                    rebuildSourceTable();
                    sourceTable.visible(() -> shownSource);
                    sourceTable.update(this::positionSourceDropdown);
                    addChild(sourceTable);
                }
                positionSourceDropdown();
            }).checked(b -> {
                ((Image)b.getCells().first().get()).setDrawable(shownSource ? Icon.upOpen : Icon.downOpen);
                return shownSource;
            }).get();
            t.getCell(sourceButton).padLeft(10f).size(100f, 50f);
        }).fillX().padBottom(4).row();

        cont.pane(pane -> {
            pane.top().left();
            resultTable = pane;
        }).grow().row();

        buttons.defaults().size(210f,64f);
    }
    private void rebuildSourceTable(){
        if(sourceTable == null) return;
        sourceTable.clearChildren();
        sourceTable.setWidth(Scl.scl(200f));
        sourceTable.setHeight(Scl.scl((resource.all.size + 1) * 60f));
        sourceTable.pane(p -> {
            for(var base : resource.all){
                Table sourceRow = new Table();
                sourceRow.button(base.name, Icon.none, Styles.flatTogglet, () -> {
                    if(resource.enabled.contains(base)){
                        resource.enabled.remove(base);
                        Core.settings.put("MCS-resource-" + base.name, false);
                    }else{
                        resource.enabled.add(base);
                        Core.settings.put("MCS-resource-" + base.name, true);
                    }
                }).height(60f).padRight(base.isCustom ? 36f : 0f).checked(b -> {
                    boolean chk = resource.enabled.contains(base);
                    ((Image)b.getCells().first().get()).setDrawable(chk ? Icon.okSmall.tint(Color.green) : Icon.cancelSmall.tint(Color.red));
                    return chk;
                }).growX();

                Stack stackedRow = new Stack();
                stackedRow.add(sourceRow);
                if(base.isCustom){
                    Table deleteOverlay = new Table();
                    deleteOverlay.add().growX();
                    deleteOverlay.button(Icon.trashSmall, Styles.flati, () -> {
                        ui.showConfirm("@confirm", "@musicSquare.deleteConfirm", () -> {
                            resource.removeCustomProvider(base);
                            rebuildSourceTable();
                            positionSourceDropdown();
                        });
                    }).size(36f, 60f);
                    stackedRow.add(deleteOverlay);
                }
                p.add(stackedRow).growX().height(60f).row();
            }
            p.table(addRow -> {
                addRow.button("@musicSquare.addProvider", Icon.addSmall, Styles.flatt, () -> {
                    FileChooser.open("json").submitMulti(files -> {
                        for(var fi : files){
                            resource.importCustomProvider(fi);
                        }
                    });
                }).growX().height(60f);
                addRow.button("?", Styles.flatt, () -> {
                    ui.showInfo("TODO");
                    //TODO: 接口说明显示
                }).size(36f, 60f);
            }).growX().height(60f).row();
        }).scrollX(false).grow();
        positionSourceDropdown();
    }
    private void positionSourceDropdown(){
        if(sourceButton == null || sourceTable == null) return;
        var position = sourceButton.localToAscendantCoordinates(this, Tmp.v1.set(0f, 0f));
        sourceTable.setPosition(
            position.x + sourceButton.getWidth() - sourceTable.getWidth(),
            position.y - sourceTable.getHeight()
        );
    }

    private void search(){
        var sound = (CustomSoundControl)control.sound;
        if(!word.isEmpty() && !isLoading){
            sound.stopPreView();
            previewingUrl = "";
            resource.search(word, Core.settings.getInt("musicSquare-limit", 10));
        }
    }

    private void loading(){
        isLoading = true;
        clearCoverTextures();
        resultTable.clear();
        resultTable.top().left();
        resultTable.add("@loading").pad(20);
        loadingSpinner = resultTable.image(Icon.settings, Pal.accent).update(i -> {
            i.rotateBy(Time.delta * 360f / 20f);
        }).size(16f).padLeft(4f).get();
    }

    private void complete(){
        loadingSpinner = null;
        resultTable.clear();
        resultTable.top().left();
        if(resource.allResults.size == 0){
            resultTable.add(resource.enabled.isEmpty() ? "@musicSquare.noProvider" : "@musicList.empty").pad(20);
        }else{
            for(var t : resource.allResults){
                trackShow(t);
            }
        }
        Log.info("Found " + resource.allResults.size + " result(s) at all\n");
        isLoading = false;
    }

    private void clearCoverTextures(){
        coverGeneration++;
        for(Texture texture : coverTextures){
            texture.dispose();
        }
        coverTextures.clear();
    }

    private void trackShow(Track t){
        var sound = (CustomSoundControl)control.sound;

        resultTable.table(Styles.grayPanel, row -> {
            row.defaults().pad(4).left();

            Image cover = new Image();
            cover.setScaling(Scaling.fit);
            row.add(cover).size(48f);

            row.table(info -> {
                info.defaults().left();
                info.add(t.name != null ? t.name : "@unknown")
                    .color(Pal.accent).growX().wrap();
                info.row();
                info.add(t.artist != null ? t.artist : "")
                    .color(Color.lightGray).growX().wrap();
            }).width(300f).growX().padLeft(8f);

            if(t.url != null && !t.url.isEmpty() && Provider.isSafeUrl(t.url)){
                row.button(Icon.play, new ImageButton.ImageButtonStyle(Styles.clearNonei){{
                    imageUp = Icon.play;
                    imageChecked = Icon.pause;
                    imageDisabled = loading;
                }}, () -> {
                    if(t.url.equals(previewingUrl)){
                        stopPreviewListening();
                    }else{
                        trackPreviewListening(t);
                    }
                }).checked(b -> t.url.equals(previewingUrl) && sound.isPreview()).disabled(b -> t.url.equals(previewingUrl) && !sound.isPreview()).size(38f);
                row.button(Icon.download, Styles.clearNonei, () -> {
                    trackDownload(t);
                }).size(38f);
            }else{
                row.add(Core.bundle.get("musicSquare.noSource")).color(Color.lightGray).padLeft(8f);
            }

            if(t.pic != null && !t.pic.isEmpty() && Provider.isSafeUrl(t.pic)){
                int generation = coverGeneration;

                Http.get(t.pic, res -> {
                    try{
                        byte[] data = res.getResult();

                        Core.app.post(() -> {
                            if(generation != coverGeneration) return;

                            Pixmap pix = null;
                            Texture tex = null;
                            try{
                                pix = new Pixmap(data);
                                tex = new Texture(pix);
                                cover.setDrawable(new TextureRegionDrawable(new TextureRegion(tex)));
                                coverTextures.add(tex);
                                tex = null;
                            }catch(Exception e){
                                cover.setDrawable(Core.atlas.find("error"));
                            }finally{
                                if(pix != null) pix.dispose();
                                if(tex != null) tex.dispose();
                            }
                        });
                    }catch(Exception e){
                        Core.app.post(() -> cover.setDrawable(Core.atlas.find("error")));
                    }
                }, e -> {});
            }
        }).growX().pad(2).row();
    }

    private void trackPreviewListening(Track t){
        if(!Provider.isSafeUrl(t.url)) return;
        previewingUrl = t.url;

        var sound = (CustomSoundControl)control.sound;

        t.downloadToTmp(fi -> {
            Core.app.post(() -> {
                if(!previewingUrl.equals(t.url)) return;
                if(fi == null){
                    ui.showInfo("@musicSquare.unusableExt");
                    previewingUrl = "";
                    return;
                }
                try{
                    sound.playPreView(
                            new Music(fi){
                                @Override
                                public void setLooping(boolean isLooping){}
                            }
                    );
                }catch(Exception ex){
                    ui.showException(ex);
                }
            });
        }, e -> {
            if(!previewingUrl.equals(t.url)) return;
            previewingUrl = "";
            Core.app.post(() -> ui.showInfo("@musicSquare.timeOut"));
        }, true);
    }

    private void stopPreviewListening(){
        ((CustomSoundControl)control.sound).stopPreView();
        previewingUrl = "";
    }

    private void trackDownload(Track t){
        BaseDialog dialog = new BaseDialog("@musicSquare.selectCategory");
        dialog.addCloseButton();
        dialog.cont.table(Tex.button, bt -> {
            bt.defaults().size(200f, 60f).left();
            bt.button("@importMusic.ambient", Styles.flatt, () -> {
                dialog.hide();
                t.download("ambient");
            });
            bt.row();
            bt.button("@importMusic.dark", Styles.flatt, () -> {
                dialog.hide();
                t.download("dark");
            });
            bt.row();
            bt.button("@importMusic.boss", Styles.flatt, () -> {
                dialog.hide();
                t.download("boss");
            });
            bt.row();
            bt.button("@importMusic.menu", Styles.flatt, () -> {
                dialog.hide();
                t.download("menu");
            });
            bt.row();
            bt.button("@importMusic.editor", Styles.flatt, () -> {
                dialog.hide();
                t.download("editor");
            });
            bt.row();
            bt.button("@importMusic.planet", Styles.flatt, () -> {
                var planets = new BaseDialog("@importMusic.planet");
                planets.addCloseButton();
                planets.cont.pane(table -> {
                    table.defaults().size(200f, 60f).left();
                    for(var planet : content.planets()){
                        if(!planet.accessible) continue;
                        table.button(planet.localizedName, Icon.planet.tint(planet.iconColor), () -> {
                            planets.hide();
                            dialog.hide();
                            t.download(planet.name);
                        });
                        table.row();
                    }
                });
                planets.show();
            });
        });
        dialog.show();
    }

    private class SearchSettingsDialog extends BaseDialog{
        public SearchSettingsDialog(){
            super("@musicSquare.searchSettings");
            cont.add(new SettingsMenuDialog.SettingsTable(){{
                sliderPref("musicSquare-limit", 10, 1, 30, i -> i + "");
                sliderPref("musicSquare-timeout", 15, 0, 60, i -> i == 0 ? "@musicSquare.unlimit" : Core.bundle.format("setting.seconds", i));
                if(!mobile){
                    pref(new MCS_SettingMenuDialog.ButtonSetting("@openProviderFolder", Icon.folder, () -> {
                        try{
                            Core.app.openFolder(saveFolder.child("providers").absolutePath());
                        }catch(Exception e){
                            ui.showException(e);
                        }
                    }));
                }
            }});
            addCloseButton();
        }
    }
}
