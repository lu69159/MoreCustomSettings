package MCS.game;

import MCS.game.enumClass.ContentManageMode;
import arc.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.EventType;
import mindustry.mod.Mods;
import mindustry.type.*;
import mindustry.ui.dialogs.DatabaseDialog;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

import static mindustry.Vars.*;
import static MCS.main.*;

public class ContentManager {
    public boolean enabled;
    public ContentManageMode mode;

    private Fi saveFi;
    private final Seq<PlanetContentData> planetContentDataMap = new Seq<>();
    private final Seq<Jval> unloadedData = new Seq<>();

    public ContentManager(){
        Events.on(EventType.SectorLaunchEvent.class, e -> overrideRule());
        Events.on(EventType.SaveLoadEvent.class, e -> overrideRule());
    }

    public void load(){
        saveFi = saveFolder.child("contents.json");

        if(saveFi.exists()){
            try{
                var json = Jval.read(new InputStreamReader(new FileInputStream(saveFi.path()), StandardCharsets.UTF_8));
                mode = ContentManageMode.valueOf(json.getString("mode", "planet"));
                var roots = json.get("roots").asArray();
                for(var obj : roots){
                    Planet planet = content.planet(obj.getString("planet", ""));
                    if(planet == null){
                        unloadedData.add(obj);
                    }else{
                        planetContentDataMap.add(new PlanetContentData(planet, obj));
                    }
                }
            }catch(Exception e){
                ui.showException(e);
                mode = ContentManageMode.planet;
                for(var planet : content.planets()){
                    if(planetContentDataMap.find(data -> data.planet == planet) == null && planet.accessible){
                        planetContentDataMap.add(new PlanetContentData(planet));
                    }
                }
            }
        }else{
            mode = ContentManageMode.planet;
            for(var planet : content.planets()){
                if(planetContentDataMap.find(data -> data.planet == planet) == null && planet.accessible){
                    planetContentDataMap.add(new PlanetContentData(planet));
                }
            }
        }
        
        enabled = Core.settings.getBool("enableContentManager", false);
        reloadData();
    }
    public void save(){
        Jval roots = Jval.newArray();
        for(var d : planetContentDataMap){
            d.save(roots);
        }
        for(var un : unloadedData){
            roots.add(un);
        }

        try(Writer w = new OutputStreamWriter(new FileOutputStream(saveFi.path()), StandardCharsets.UTF_8)){
            Jval.newObject().put("mode", mode.name()).put("roots", roots).writeTo(w, Jval.Jformat.formatted);
        }catch(IOException e){
            ui.showException(e);
        }
    }
    public void reset(){
        mode = ContentManageMode.planet;
        planetContentDataMap.clear();
        saveFolder.child("contents.json").delete();
        load();
    }

    public PlanetContentData getData(Planet planet){
        var data = planetContentDataMap.find(pr -> pr.planet == planet);
        if(data == null) planetContentDataMap.add(new PlanetContentData(planet));
        return planetContentDataMap.find(pr -> pr.planet == planet);
    }

    public void reloadData(){
        if(!enabled) return;

        for(var d : planetContentDataMap){
            d.loadData();
        }
        try{
            Field f = DatabaseDialog.class.getDeclaredField("allTabs");
            f.setAccessible(true);
            f.set(ui.database, null);
        }catch(ReflectiveOperationException e){
            ui.showException(e);
        }
    }
    public void resetData(){
        for(var d : planetContentDataMap){
            d.resetData();
        }
        try{
            Field f = DatabaseDialog.class.getDeclaredField("allTabs");
            f.setAccessible(true);
            f.set(ui.database, null);
        }catch(ReflectiveOperationException e){
            ui.showException(e);
        }
    }

    private void overrideRule(){
        if(state.isCampaign()){
            if(state.rules.bannedBlocks.size > 0){
                var blocks = state.rules.bannedBlocks;
                if(state.rules.blockWhitelist){
                    for(var b : content.blocks()){
                        if(b.shownPlanets.contains(state.getPlanet())){
                            blocks.add(b);
                        }
                    }
                }else{
                    for(var b : blocks){
                        if(b.shownPlanets.contains(state.getPlanet())){
                            blocks.remove(b);
                        }
                    }
                }
            }
            if(state.rules.bannedUnits.size > 0){
                var units = state.rules.bannedUnits;
                if(state.rules.blockWhitelist){
                    for(var u : content.units()){
                        if(u.shownPlanets.contains(state.getPlanet())){
                            units.add(u);
                        }
                    }
                }else{
                    for(var u : units){
                        if(u.shownPlanets.contains(state.getPlanet())){
                            units.remove(u);
                        }
                    }
                }
            }
        }
    }

    public static class PlanetContentData{
        public Planet planet;
        public Seq<Planet> loadedPlanets;
        public Seq<String> unloadedPlanets;
        public Seq<Mods.LoadedMod> loadedMods;
        public Seq<String> unloadedMods;
        public Seq<UnlockableContent> originalData;

        public PlanetContentData(Planet planet){
            this.planet = planet;
            this.init();
        }
        public PlanetContentData(Planet planet, @Nullable Jval obj){
            this.planet = planet;
            loadedPlanets = Seq.with(planet);
            unloadedPlanets = new Seq<>();
            this.load(obj);
        }

        public void load(Jval obj){
            originalData = new Seq<>();
            for(var all : content.getContentMap()){
                for(var c : all){
                    if(c instanceof UnlockableContent u && !u.hideDatabase && (u.shownPlanets.contains(planet) || u.databaseTabs.contains(planet))){
                        originalData.add(u);
                    }
                }
            }
            if(obj == null){
                loadedPlanets = Seq.with(planet);
                unloadedPlanets = new Seq<>();
                loadedMods = planet.isVanilla() ? new Seq<>() : Seq.with(planet.minfo.mod);
                unloadedMods = new Seq<>();
                return;
            }

            try{
                loadedPlanets = new Seq<>();
                unloadedPlanets = new Seq<>();
                loadedMods = new Seq<>();
                unloadedMods = new Seq<>();

                var PLANETS = obj.get("planets").asArray();
                if(PLANETS != null){
                    for(var name : PLANETS){
                        Planet p = content.planet(name.asString());
                        if(p == null){
                            unloadedPlanets.add(name.asString());
                        }else{
                            loadedPlanets.add(p);
                        }
                    }
                }

                var MODS = obj.get("mods").asArray();
                if(MODS != null){
                    for(var name : MODS){
                        Mods.LoadedMod m = mods.getMod(name.asString());
                        if(m == null){
                            unloadedMods.add(name.asString());
                        }else{
                            loadedMods.add(m);
                        }
                    }
                }
            }catch(Exception e){
                loadedPlanets = Seq.with(planet);
                unloadedPlanets = new Seq<>();
                loadedMods = planet.isVanilla() ? new Seq<>() : Seq.with(planet.minfo.mod);
                unloadedMods = new Seq<>();
            }
        }
        public void save(Jval roots){
            var PLANETS = Jval.newArray();
            for(var loadedPlanet : loadedPlanets){
                PLANETS.add(loadedPlanet.name);
            }
            for(var unloadedPlanet : unloadedPlanets){
                PLANETS.add(unloadedPlanet);
            }

            var MODS = Jval.newArray();
            for(var loadedMod : loadedMods){
                MODS.add(loadedMod.name);
            }
            for(var unloadedMod : unloadedMods){
                MODS.add(unloadedMod);
            }

            var DATAOBJ = Jval.newObject().put("planet", planet.name).put("planets", PLANETS).put("mods", MODS);
            roots.add(DATAOBJ);
        }
        public void init(){
            originalData = new Seq<>();
            for(var all : content.getContentMap()){
                for(var c : all){
                    if(c instanceof UnlockableContent u && !u.hideDatabase && (u.shownPlanets.contains(planet) || u.databaseTabs.contains(planet))){
                        originalData.add(u);
                    }
                }
            }
            loadedPlanets = Seq.with(planet);
            unloadedPlanets = new Seq<>();
            loadedMods = planet.isVanilla() ? new Seq<>() : Seq.with(planet.minfo.mod);
            unloadedMods = new Seq<>();
        }

        public void loadData(){
            Seq<UnlockableContent> tmp = new Seq<>();
            if(contentManager.mode == ContentManageMode.planet){
                if(loadedPlanets.size > 0){
                    for(var planetData : contentManager.planetContentDataMap){
                        if(loadedPlanets.contains(planetData.planet)){
                            for(var u : planetData.originalData){
                                if(!tmp.contains(u)) tmp.add(u);
                            }
                        }
                    }
                }
            }
            else if(contentManager.mode == ContentManageMode.mod){
                for(var all : content.getContentMap()){
                    for(var c : all){
                        if(c instanceof UnlockableContent u && (originalData.contains(u) || (!u.isVanilla() && loadedMods.contains(u.minfo.mod)))){
                            if(!tmp.contains(u)) tmp.add(u);
                        }
                    }
                }
            }

            for(var all : content.getContentMap()){
                for(var c : all){
                    if(c instanceof UnlockableContent u && !u.hideDatabase){
                        if(tmp.contains(u)){
                            if(!u.shownPlanets.contains(planet)) u.shownPlanets.add(planet);
                            if(!u.databaseTabs.contains(planet)) u.databaseTabs.add(planet);
                        }else{
                            if(u.shownPlanets.contains(planet)) u.shownPlanets.remove(planet);
                            if(u.databaseTabs.contains(planet)) u.databaseTabs.remove(planet);
                        }
                        if(u.shownPlanets.isEmpty()) u.shownPlanets.add(Planets.sun);
                        if(u.databaseTabs.isEmpty()) u.databaseTabs.add(Planets.sun);
                        if(u instanceof UnitType unit){
                            unit.envDisabled = 0;
                        }
                    }
                }
            }
        }
        public void resetData(){
            for(var all : content.getContentMap()){
                for(var c : all){
                    if(c instanceof UnlockableContent u && !u.hideDatabase){
                        if(originalData.contains(u)){
                            if(!u.shownPlanets.contains(planet)) u.shownPlanets.add(planet);
                            if(!u.databaseTabs.contains(planet)) u.databaseTabs.add(planet);
                        }else{
                            if(u.shownPlanets.contains(planet)) u.shownPlanets.remove(planet);
                            if(u.databaseTabs.contains(planet)) u.databaseTabs.remove(planet);
                        }
                    }
                }
            }
        }
    }
}
