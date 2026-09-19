package contractor.data.scripts.campaign.fleets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.DataForEncounterSide;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin.FleetMemberData;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.BaseFIDDelegate;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig;
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfigGen;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import contractor.data.scripts.util.ContractorStaticVars;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NaniteFIDConfig implements FIDConfigGen {
	public FIDConfig createConfig() {
		FIDConfig config = new FIDConfig();
		config.alwaysAttackVsAttack = true;
		config.alwaysHarry = true;
		config.showTransponderStatus = false;
		config.lootCredits = false;

		config.delegate = new BaseFIDDelegate() {
			public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
				if (!(dialog.getInteractionTarget() instanceof CampaignFleetAPI fleet)) return;

				float mult = context.computePlayerContribFraction();
				float p = Global.getSettings().getFloat("salvageHullmodProb");
				float pItem = Global.getSettings().getFloat("salvageHullmodRequiredItemProb");

				DataForEncounterSide data = context.getDataFor(fleet);
				List<FleetMemberAPI> losses = new ArrayList<>();
				for (FleetMemberData fmd : data.getOwnCasualties()) {
					losses.add(fmd.getMember());
				}

				Random random = Misc.getRandom(Misc.getSalvageSeed(fleet), 7);

				for (FleetMemberAPI member : losses) {
					if (member.getHullSpec().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID)) {
						int rolls = switch (member.getHullSpec().getHullSize()) {
							case CAPITAL_SHIP -> 12;
							case CRUISER -> 6;
							case DESTROYER -> 3;
							case FRIGATE -> 1;
							default -> 0;
						};

						for (int i = 0; i < rolls; i++) {
							ShipVariantAPI variant = member.getVariant();
							for (String id : variant.getHullMods()) {
								if (variant.getHullSpec().isBuiltInMod(id)) {
									if (random.nextFloat() < pItem && random.nextFloat() < mult) {
										HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
										CargoStackAPI item = spec.getEffect().getRequiredItem();
										if (item != null) {
											boolean addToLoot = true;
											if (item.getSpecialItemSpecIfSpecial() != null && item.getSpecialItemSpecIfSpecial().hasTag(Tags.NO_DROP)) {
												addToLoot = false;
											} else if (item.getResourceIfResource() != null && item.getResourceIfResource().hasTag(Tags.NO_DROP)) {
												addToLoot = false;
											} else if (item.getFighterWingSpecIfWing() != null && item.getFighterWingSpecIfWing().hasTag(Tags.NO_DROP)) {
												addToLoot = false;
											} else if (item.getWeaponSpecIfWeapon() != null && item.getWeaponSpecIfWeapon().hasTag(Tags.NO_DROP)) {
												addToLoot = false;
											}
											if (addToLoot) {
												salvage.addItems(item.getType(), item.getData(), 1);
											}
										}
									}
								}
								//dont drop the actual hullmods
//								if (random.nextFloat() < p && random.nextFloat() < mult) {
//									HullModSpecAPI spec = Global.getSettings().getHullModSpec(id);
//									boolean known = Global.getSector().getPlayerFaction().knowsHullMod(id);
//									for (CargoStackAPI stack : salvage.getStacksCopy())
//										if (stack.getHullModSpecIfHullMod() == spec)
//											known = true;
//									if (DebugFlags.ALLOW_KNOWN_HULLMOD_DROPS) known = false;
//									if (known || spec.isHidden() || spec.isHiddenEverywhere()) continue;
//									if (spec.hasTag(Tags.HULLMOD_NO_DROP)) continue;
//
//									salvage.addHullmods(id, 1);
//								}
							}
						}
					}
				}
			}

			public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
				bcc.aiRetreatAllowed = false;
				bcc.fightToTheLast = true;

				if (bcc.getOtherFleet() != null) {
					for (FleetMemberAPI curr : bcc.getOtherFleet().getMembersWithFightersCopy()) {
						if (curr.getHullSpec().hasTag(ContractorStaticVars.CONTRACTOR_NANITE_ID)) {
							bcc.forceObjectivesOnMap = true;
							break;
						}
					}
				}
				Global.getSector().getPlayerMemoryWithoutUpdate().set("$encounteredNanites", true);
			}
		};
		return config;
	}
}





