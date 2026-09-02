package MCS.game;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import mindustry.ctype.*;
import mindustry.type.*;
import mindustry.ui.dialogs.DatabaseDialog;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;

import static mindustry.Vars.*;
import static MCS.main.*;

public class ContentManager {
    private Fi saveFi;
    private final Seq<PlanetContentData> planetContentDataMap = new Seq<>();
    private final Seq<Jval> unloadedData = new Seq<>();


    public void load(){
        saveFi = saveFolder.child("contents.json");

        if(saveFi.exists()){
            try{
                var roots = Jval.read(new InputStreamReader(new FileInputStream(saveFi.path()), StandardCharsets.UTF_8)).asArray();
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
                for(var planet : content.planets()){
                    if(planetContentDataMap.find(data -> data.planet == planet) == null && planet.accessible){
                        planetContentDataMap.add(new PlanetContentData(planet));
                    }
                }
            }
        }else{
            for(var planet : content.planets()){
                if(planetContentDataMap.find(data -> data.planet == planet) == null && planet.accessible){
                    planetContentDataMap.add(new PlanetContentData(planet));
                }
            }
        }
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
            roots.writeTo(w, Jval.Jformat.formatted);
        }catch(IOException e){
            ui.showException(e);
        }
    }
    public void reset(){
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

    public static class PlanetContentData{
        public Planet planet;
        public Seq<Planet> loadedPlanets;
        public Seq<String> unloadedPlanets;
        public Seq<UnlockableContent> originalData;

        public PlanetContentData(Planet planet){
            this.planet = planet;
            this.init();
            loadedPlanets = Seq.with(planet);
            unloadedPlanets = new Seq<>();
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
                    if(c instanceof UnlockableContent u && !u.hideDatabase && (u.isOnPlanet(planet) || u.databaseTabs.contains(planet))){
                        originalData.add(u);
                    }
                }
            }

            if(obj == null){
                loadedPlanets = Seq.with(planet);
                unloadedPlanets = new Seq<>();
                return;
            }
            try{
                loadedPlanets = new Seq<>();
                unloadedPlanets = new Seq<>();
                var DATA = obj.get("data").asArray();
                if(DATA != null){
                    for(var name : DATA){
                        Planet p = content.planet(name.asString());
                        if(p == null){
                            unloadedPlanets.add(name.asString());
                        }else{
                            loadedPlanets.add(p);
                        }
                    }
                }
            }catch(Exception e){
                loadedPlanets = Seq.with(planet);
                unloadedPlanets = new Seq<>();
            }
        }
        public void save(Jval roots){
            var DATA = Jval.newArray();
            for(var loadedPlanet : loadedPlanets){
                DATA.add(loadedPlanet.name);
            }
            for(var unloadedPlanet : unloadedPlanets){
                DATA.add(unloadedPlanet);
            }
            var DATAOBJ = Jval.newObject().put("planet", planet.name).put("data", DATA);
            roots.add(DATAOBJ);
        }
        public void init(){
            originalData = new Seq<>();
            for(var all : content.getContentMap()){
                for(var c : all){
                    if(c instanceof UnlockableContent u && !u.hideDatabase && (u.isOnPlanet(planet) || u.databaseTabs.contains(planet))){
                        originalData.add(u);
                    }
                }
            }
            loadedPlanets = Seq.with(planet);
            unloadedPlanets = new Seq<>();
        }

        public void loadData(){
            Seq<UnlockableContent> tmp = new Seq<>();
            if(loadedPlanets.size > 0){
                for(var planetData : contentManager.planetContentDataMap){
                    if(loadedPlanets.contains(planetData.planet)){
                        for(var u : planetData.originalData){
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
                    }
                }
            }

        }
    }
}
