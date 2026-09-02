package MCS.ui.dialogs;

import arc.Core;
import arc.graphics.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import mindustry.content.Planets;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.BorderImage;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;

import static mindustry.Vars.*;
import static MCS.main.*;

public class ContentManagerDialog extends BaseDialog{
    Planet choosePlanet;
    private final Seq<Planet> loadedPlanets = new Seq<>(), unloadedPlanets = new Seq<>();

    public ContentManagerDialog(String title) {
        super(title);
        addCloseButton();
        buttons.button("@settings.reset", Icon.trash, () -> ui.showConfirm("@confirm", "@contentManager.reset.confirm", () -> {
            contentManager.reset();
            setup();
        })).size(210f, 64f);

        shown(this::setup);
        onResize(this::setup);
        hidden(contentManager::save);
    }

    void setup(){
        float w = Math.max(Core.graphics.getWidth() / 8f, 50f);
        float h = Math.max(Core.graphics.getHeight() / 8f, 35f);

        if(choosePlanet == null) choosePlanet = Planets.erekir;

        loadedPlanets.clear();
        unloadedPlanets.clear();

        loadedPlanets.addAll(contentManager.getData(choosePlanet).loadedPlanets);
        for(var p : content.planets()){
            if(p.accessible && !loadedPlanets.contains(p)) unloadedPlanets.add(p);
        }

        cont.clear();
        cont.table(left -> {
            left.add("[accent]" + Core.bundle.get("planets")).top().row();
            left.image().color(Pal.accent).height(3.0F).left().fillX().padBottom(5.0F).top().row();
            left.pane(side -> {
                content.planets().sort().each(planet -> {
                    if(planet.accessible){
                        side.button("[#" + planet.iconColor + "]" + Iconc.planet + "[#FFFFFF]" + planet.localizedName, Styles.clearTogglet, () -> {
                            choosePlanet = planet;
                            setup();
                        }).checked(b -> choosePlanet == planet).height(80f).growX().row();
                    }
                });
            }).grow();
        }).width(w).left().growY();

        cont.table(mid -> {
            mid.table(loaded -> {
                loaded.add(Core.bundle.get("contentManager.loaded")).top().row();
                loaded.pane(tab -> {
                    boolean isEmpty = true;
                    for(var planet : loadedPlanets){
                        isEmpty = false;
                        tab.button(t -> {
                            t.defaults().left().top();
                            t.margin(12f);
                            t.table(title1 -> {
                                title1.left();
                                title1.add(new BorderImage(){{
                                    if(Core.atlas.isFound(planet.fullIcon)){
                                        setDrawable(planet.fullIcon);
                                    }else{
                                        setDrawable(Icon.planet.getRegion());
                                        setColor(planet.iconColor);
                                    }
                                    border(Pal.accent);
                                }}).size(h - 8f).padTop(-8f).padLeft(-8f).padRight(8f);
                                title1.table(text -> {
                                    text.add(planet.localizedName + "\n" + (planet.isVanilla() ? "" : "[lightgray]" + planet.minfo.mod.meta.displayName)).wrap().top().width(300f).growX().left();
                                }).top().growX();
                                title1.add().growX();
                            });
                        }, Styles.grayt, () -> {
                            contentManager.getData(choosePlanet).loadedPlanets.remove(planet);
                            contentManager.reloadData();
                            setup();
                        }).height(h).top().growX().row();
                    }
                    if(isEmpty) tab.add("@empty");
                }).top().grow();

            }).width(w).left().growY();
            mid.table(unloaded -> {
                unloaded.add(Core.bundle.get("contentManager.unloaded")).top().row();
                unloaded.pane(tab -> {
                    boolean isEmpty = true;
                    for(var planet : unloadedPlanets){
                        isEmpty = false;
                        tab.button(t -> {
                            t.defaults().left().top();
                            t.margin(12f);
                            t.table(title1 -> {
                                title1.left();
                                title1.add(new BorderImage(){{
                                    if(Core.atlas.isFound(planet.fullIcon)){
                                        setDrawable(planet.fullIcon);
                                    }else{
                                        setDrawable(Icon.planet.getRegion());
                                        setColor(planet.iconColor);
                                    }
                                    border(Color.lightGray);
                                }}).size(h - 8f).padTop(-8f).padLeft(-8f).padRight(8f);
                                title1.table(text -> {
                                    text.add(planet.localizedName + "\n" + (planet.isVanilla() ? "" : "[lightgray]" + planet.minfo.mod.meta.displayName)).wrap().top().width(300f).growX().left();
                                }).height(h).top().growX();
                                title1.add().growX();
                            });
                        }, Styles.grayt, () -> {
                            contentManager.getData(choosePlanet).loadedPlanets.add(planet);
                            contentManager.reloadData();
                            setup();
                        }).height(h).top().growX().row();
                    }
                    if(isEmpty) tab.add("@empty");
                }).top().grow();

            }).width(w).right().growY();
        }).width(2*w + 5f).left().growY();

        cont.table(right -> {
            right.add(Core.bundle.get("database")).row();
            right.image().color(Pal.accent).height(3.0F).left().fillX().padBottom(5.0F).row();
            right.pane(data -> {
                showPlanetDataBase(choosePlanet, data);
            }).marginTop(10f).grow();
        }).grow();
    }

    void showPlanetDataBase(Planet planet, Table inner){
        inner.left().top();

        OrderedMap<String, OrderedMap<String, Seq<UnlockableContent>>> cats = new OrderedMap<>();

        for(Seq<Content> list : content.getContentMap()){
            for(Content c : list){
                if(c instanceof UnlockableContent u){
                    String cat = u.databaseCategory == null ? u.getContentType().name() : u.databaseCategory;
                    String tag = u.databaseTag == null ? "default" : u.databaseTag;

                    if(u.isHidden() || u.hideDatabase || !(u.allDatabaseTabs || u.databaseTabs.contains(planet))) continue;

                    var m = cats.get(cat, new OrderedMap<>());
                    var arr = m.get(tag, new Seq<>());
                    arr.add(u);
                    m.put(tag, arr);
                    cats.put(cat, m);
                }
            }
        }
        if(cats.isEmpty()){
            inner.add("@none.found");
            return;
        }

        int cols = (int) Mathf.clamp((Core.graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 12), 1, 22);

        inner.pane(p -> {
            for(int ci = 0; ci < cats.size; ci++){
                String catName = cats.orderedKeys().get(ci);
                OrderedMap<String, Seq<UnlockableContent>> m = cats.get(catName);
                if(m.isEmpty()) continue;

                p.add("@database-category." + catName).growX().left().color(Pal.accent);
                p.row();
                p.image().pad(5).padLeft(0).padRight(0).height(3).color(Pal.accent).growX();
                p.row();

                p.table(sub -> {
                    for(int ti = 0; ti < m.size; ti++){
                        String tagName = m.orderedKeys().get(ti);
                        Seq<UnlockableContent> arr = m.get(tagName);
                        if(arr.isEmpty()) continue;

                        if(!"default".equals(tagName)){
                            sub.table(tg -> {
                                tg.add("@database-tag." + tagName).left().color(Pal.gray);
                                tg.image().growX().pad(5).height(3).color(Pal.gray);
                            }).pad(4, 8, 4, 8).growX().row();
                        }

                        sub.table(list -> {
                            list.left();
                            int count = 0;
                            for(UnlockableContent u : arr){
                                Image image = list.add(new Image(u.uiIcon)).size(Mathf.clamp((Core.graphics.getWidth() - Math.max(Core.graphics.getWidth() / 8f, 200f)) / 40f, 32f, Core.app.isMobile() ? 40f : 80f)).pad(3).get();

                                image.clicked(() -> ui.content.show(u));          // Vars.ui
                                image.addListener(new Tooltip(tip -> tip.background(Tex.button).add(u.localizedName)));

                                if((++count) % cols == 0) list.row();
                            }
                            // 补齐最后一行剩下的空格（可选，对齐原版 DatabaseDialog.java:221）
                            for(int k = 0; k < cols - count; k++){
                                Image filler = new Image();
                                filler.setColor(Color.clear);
                                list.add(filler).size(32).pad(3);
                            }
                        }).growX().left().padBottom(10).row();
                    }
                }).width(cols * Scl.scl(38f)).growX().left().padBottom(10);   // 给分组内表格一个稳定的列宽

                p.row();
            }
        }).grow().scrollX(false);
    }


}
