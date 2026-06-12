package MCS.ui.dialogs;

import arc.*;
import arc.func.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.scene.utils.*;
import arc.util.*;
import mindustry.Vars;
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
        new BaseDialog("@campaign.difficulty");

        Events.on(SaveLoadEvent.class, e -> {
            if(state.isCampaign()){
                rulesMaps.get(state.getPlanet()).apply(state.getPlanet(), state.rules);
                Call.setRules(Vars.state.rules);
            }
        });
        Events.on(SectorLaunchEvent.class, e -> {
            if(state.isCampaign()){
                rulesMaps.get(e.sector.planet).apply(e.sector.planet, state.rules);
                Call.setRules(Vars.state.rules);
            }
        });

        hidden(() -> {
            if(planet != null){
                rulesMaps.save(planet, customRule);
                if(state.isGame() && state.isCampaign() && state.getPlanet() == planet){
                    customRule.apply(planet, state.rules);
                    Call.setRules(Vars.state.rules);
                }
            }
        });
    }

    @Override
    protected void onResize(Runnable run){
        Events.on(ResizeEvent.class, event -> {
            if(isShown() && Core.scene.getDialog() == this && !Core.input.isShowingTextInput()){
                rebuild();
                updateScrollFocus();
            }
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

                for(CustomDifficulty diff : CustomDifficulty.all){
                    t.button(diff.localized(), style, () -> {
                        if(!diff.isCustom){
                            customRule.set(diff);
                            rebuild();
                        }
                        else {
                            customRule.customDiff = CustomDifficulty.custom;
                            rebuild();
                        }
                    }).group(group).checked(b -> customRule.customDiff == diff).tooltip(diff.info());
                    if(Core.graphics.isPortrait() && diff.ordinal() % 2 == 1){
                        t.row();
                    }
                }
            }).left().fill(false).expand(false, false).row();
            current.add(new Label("@ruleteam")).left().expandX().row();
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
            }).growX().padLeft(10f).fill(false).row();
            current.table(Tex.button, t -> {
                slider(t, "@rules.blockhealthmultiplier", customRule.team(team).blockHealthMultiplier, value -> {
                    customRule.team(team).blockHealthMultiplier = value;
                });
                slider(t, "@rules.unithealthmultiplier", customRule.team(team).unitHealthMultiplier, value -> {
                    customRule.team(team).unitHealthMultiplier = value;
                });
                slider(t, "@rules.unitcostmultiplier", customRule.team(team).unitCostMultiplier, value -> {
                    customRule.team(team).unitCostMultiplier = value;
                });
                slider(t, "@rules.unitbuildspeedmultiplier", customRule.team(team).unitBuildSpeedMultiplier, value -> {
                    customRule.team(team).unitBuildSpeedMultiplier = value;
                });
            }).left().growX().row();

            current.add(new Label("@other")).left().row();
            current.table(Tex.button, t -> {
                slider(t, "@rules.enemySpawnMultiplier", customRule.enemySpawnMultiplier, value -> {
                    customRule.enemySpawnMultiplier = value;
                }, 10f, 1000f, 1.0f, true);
                slider(t, "@rules.wavetimemultiplier", customRule.waveTimeMultiplier, value -> {
                    customRule.waveTimeMultiplier = value;
                }, 10f, 1000f, 1.0f, true);
                slider(t, "@rules.extendWaves", customRule.extendWaves, value -> {
                    customRule.extendWaves = (int)value;
                }, 0, 50, 1, false);
                slider(t, "@rules.unitfactoryactivation", customRule.unitFactoryActivationDelay, value -> {
                    customRule.unitFactoryActivationDelay = value * 60f;
                }, 0, 60, 0.5f, StatUnit.minutes.localized());
            }).left().growX().row();

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
        team = RuleTeam.enemy;
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
        if(percent) {
            slider(t, text, def, listener, min, max, step, "%");
        }
        else{
            slider(t, text, def, listener, min, max, step, "");
        }
    }

    void slider(Table t, String text, float def, Floatc listener, float min, float max, float step, String tail){
        Table row = new Table();

        Label label = new Label(text);
        row.add(label).left();

        Slider slider = new Slider(min, max, step, false);
        slider.setValue(def);

        TextField field = Elem.newField(def + "", s -> {
            try {
                float parsedValue = Strings.parseFloat(s);
                slider.setValue(parsedValue);
            } catch (NumberFormatException ignored) {}
        });

        slider.moved(listener);
        slider.changed(() -> {
            if(customRule.customDiff != CustomDifficulty.custom){
                customRule.customDiff = CustomDifficulty.custom;
            }
            field.setText(String.format("%.1f", slider.getValue()));
        });

        row.add(slider).growX().padLeft(10f);
        row.add(field).right();
        row.add(tail).right();

        t.add(row).growX().row();
    }
}
