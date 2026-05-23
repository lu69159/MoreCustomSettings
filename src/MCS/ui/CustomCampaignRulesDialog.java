package MCS.ui;

import arc.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import MCS.game.*;

import static mindustry.Vars.*;
import static mindustry.game.EventType.*;
import static MCS.main.*;

public class CustomCampaignRulesDialog extends CampaignRulesDialog{
    Planet planet;
    Table current;
    CustomCampaignRules customRule;
    RuleTeam team;

    public CustomCampaignRulesDialog(){
        super();

        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                rulesMaps.get(state.getPlanet()).apply(state.getPlanet(), state.rules);
            }
        });
        Events.on(SectorLaunchEvent.class, e -> {
            if(state.isCampaign()){
                rulesMaps.get(e.sector.planet).apply(e.sector.planet, state.rules);
            }
        });

        hidden(() -> {
            if(planet != null){
                rulesMaps.save(planet, customRule);
                if(state.isGame() && state.isCampaign() && state.getPlanet() == planet){
                    customRule.apply(planet, state.rules);
                }
            }
        });

        onResize(() -> {
            rebuild();
        });
    }

    void rebuild(){
        cont.clear();

        cont.top().pane(inner -> {
            inner.top().left().defaults().fillX().left().pad(5);
            current = inner;

            current.table(Tex.button, t -> {
                t.margin(10f);
                var group = new ButtonGroup<>();
                var style = Styles.flatTogglet;

                t.defaults().size(140f, 50f);

                for(var rt : RuleTeam.all){
                    t.button(rt.localized(), style, () -> {
                        team = rt;
                        rebuild();
                    }).group(group).checked(b -> team == rt);
                }
            }).center().fill(false).expand(false, false).row();

            current.table(Tex.button, t -> {
                slider(t, "@rules.blockhealthmultiplier", customRule.team(team).blockHealthMultiplier, value -> {
                    customRule.team(team).blockHealthMultiplier = value;
                });
                slider(t, "@rules.unithealthmultiplier", customRule.team(team).unitHealthMultiplier, value -> {
                    customRule.team(team).unitHealthMultiplier = value;
                });
                slider(t, "@rules.unithealthmultiplier", customRule.team(team).unitCostMultiplier, value -> {
                    customRule.team(team).unitHealthMultiplier = value;
                });
                slider(t, "@rules.unitbuildspeedmultiplier", customRule.team(team).unitBuildSpeedMultiplier, value -> {
                    customRule.team(team).unitBuildSpeedMultiplier = value;
                });
            }).width(500f).left().fillX().row();

            current.table(Tex.button, t -> {
                slider(t, "@rules.wavetimemultiplier", customRule.waveTimeMultiplier, value -> {
                    customRule.waveTimeMultiplier = value;
                }, 10f, 1000f, 1.0f, true);
                slider(t, "@rules.unitfactoryactivation", customRule.unitFactoryActivationDelay, value -> {
                    customRule.unitFactoryActivationDelay = value * 60f;
                }, 0, 60, 0.5f, StatUnit.minutes.localized());
            }).width(500f).left().fillX().row();

            if(planet.allowSectorInvasion){
                check("@rules.invasions", b -> customRule.sectorInvasion = b, () -> customRule.sectorInvasion);
            }

            check("@rules.fog", b -> customRule.fog = b, () -> customRule.fog);
            check("@rules.showspawns", b -> customRule.showSpawns = b, () -> customRule.showSpawns);
            check("@rules.randomwaveai", b -> customRule.randomWaveAI = b, () -> customRule.randomWaveAI);
            check("@rules.pauseDisabled", b -> customRule.pauseDisabled = b, () -> customRule.pauseDisabled);

            if(planet.showRtsAIRule){
                check("@rules.rtsai.campaign", b -> customRule.rtsAI = b, () -> customRule.rtsAI);
            }

            if(!planet.clearSectorOnLose){
                check("@rules.clearsectoronloss", b -> customRule.clearSectorOnLose = b, () -> customRule.clearSectorOnLose);
            }
        }).growY();
    }

    @Override
    public void show(Planet planet){
        this.planet = planet;
        team = RuleTeam.player;
        customRule = rulesMaps.get(planet);

        rebuild();
        show();
    }

    void check(String text, Boolc cons, Boolp prov){
        check(text, cons, prov, () -> true);
    }

    void check(String text, Boolc cons, Boolp prov, Boolp condition){
        String infoText = text.substring(1) + ".info";
        var cell = current.check(text, cons).checked(prov.get()).update(a -> a.setDisabled(!condition.get()));
        if(Core.bundle.has(infoText)){
            cell.tooltip(text + ".info");
        }
        cell.get().left();
        current.row();
    }

    void slider(Table t, String text, float def, Floatc listener){
        slider(t, text, def, listener, 0.1f, 10f, 0.01f, false);
    }

    void slider(Table t, String text, float def, Floatc listener, float min, float max, float step, boolean percent){
        t.add(text).left();

        Slider slider = new Slider(min, max, step,false);
        slider.setValue(def);

        TextField field = Elem.newField(def + "", s -> {
            try {
                float parsedValue = Strings.parseFloat(s);
                slider.setValue(parsedValue);
            }catch(NumberFormatException ignored){};
        });

        slider.moved(listener);
        slider.changed(() -> {
            field.setText(String.format("%.1f", slider.getValue()));
        });

        t.add(slider).growX().padLeft(10f).left();
        t.add(field);
        if(percent) t.add("%");
        t.row();
    }

    void slider(Table t, String text, float def, Floatc listener, float min, float max, float step, String tail){
        t.add(text).left();

        Slider slider = new Slider(min, max, step,false);
        slider.setValue(def);

        TextField field = Elem.newField(def + "", s -> {
            try {
                float parsedValue = Strings.parseFloat(s);
                slider.setValue(parsedValue);
            }catch(NumberFormatException ignored){};
        });

        slider.moved(listener);
        slider.changed(() -> {
            field.setText(String.format("%.1f", slider.getValue()));
        });

        t.add(slider).growX().padLeft(10f).left();
        t.add(field);
        t.add(tail);
        t.row();
    }
}
