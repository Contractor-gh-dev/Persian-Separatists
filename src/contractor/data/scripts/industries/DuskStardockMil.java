package contractor.data.scripts.industries;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI.MarketInteractionMode;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD.RaidDangerLevel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;


public class DuskStardockMil extends BaseIndustry implements FleetEventListener {

	public static final float DEFENSE_BONUS_BASE = 0.25f;
	public static final float DEFENSE_BONUS_BATTLESTATION = 1f;
	public static final float DEFENSE_BONUS_FORTRESS = 2f;

	public static final float IMPROVE_STABILITY_BONUS = 1f;

	public void apply() {
		super.apply(false);

		int size = 3;

		boolean battlestation = getSpec().hasTag(Industries.TAG_BATTLESTATION);
		boolean starfortress = getSpec().hasTag(Industries.TAG_STARFORTRESS);
		if (battlestation) {
			size = 5;
		} else if (starfortress) {
			size = 7;
		}

		modifyStabilityWithBaseMod();

		applyIncomeAndUpkeep(size);

		demand(Commodities.CREW, size);
		demand(Commodities.SUPPLIES, size);

		float bonus = DEFENSE_BONUS_BASE;
		if (battlestation) bonus = DEFENSE_BONUS_BATTLESTATION;
		else if (starfortress) bonus = DEFENSE_BONUS_FORTRESS;
		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD)
				.modifyMult(getModId(), 1f + bonus, getNameForModifier());

		matchCommanderToAICore(aiCoreId);

		if (!isFunctional()) {
			supply.clear();
			unapply();
		} else {
			applyCRToStation();
		}
	}

	@Override
	public void unapply() {
		super.unapply();

		unmodifyStabilityWithBaseMod();

		matchCommanderToAICore(null);

		market.getStats().getDynamic().getMod(Stats.GROUND_DEFENSES_MOD).unmodifyMult(getModId());
	}

	protected void applyCRToStation() {
		if (stationFleet != null) {
			float cr = getCR();
			for (FleetMemberAPI member : stationFleet.getFleetData().getMembersListCopy()) {
				member.getRepairTracker().setCR(cr);
			}
			FleetInflater inflater = stationFleet.getInflater();
			if (inflater != null) {
				if (stationFleet.isInflated()) {
					stationFleet.deflate();
				}
				inflater.setQuality(Misc.getShipQuality(market));
				if (inflater instanceof DefaultFleetInflater) {
					DefaultFleetInflater dfi = (DefaultFleetInflater) inflater;
					((DefaultFleetInflaterParams) dfi.getParams()).allWeapons = true;
				}
			}
		}
	}

	protected float getCR() {
		float deficit = getMaxDeficit(Commodities.CREW, Commodities.SUPPLIES).two;
		float demand = Math.max(getDemand(Commodities.CREW).getQuantity().getModifiedInt(),
				getDemand(Commodities.SUPPLIES).getQuantity().getModifiedInt());

		if (deficit < 0) deficit = 0f;
		if (demand < 1) {
			demand = 1;
			deficit = 0f;
		}


		float q = Misc.getShipQuality(market);
		if (q < 0) q = 0;
		if (q > 1) q = 1;

		float d = (demand - deficit) / demand;
		if (d < 0) d = 0;
		if (d > 1) d = 1;

		float cr = 0.5f + 0.5f * Math.min(d, q);
		if (cr > 1) cr = 1;

		return cr;
	}


	protected boolean hasPostDemandSection(boolean hasDemand, IndustryTooltipMode mode) {
		//return mode == IndustryTooltipMode.NORMAL && isFunctional();
		return mode != IndustryTooltipMode.NORMAL || isFunctional();
	}

	@Override
	protected void addPostDemandSection(TooltipMakerAPI tooltip, boolean hasDemand, IndustryTooltipMode mode) {
		//if (mode == IndustryTooltipMode.NORMAL && isFunctional()) {
		if (mode != IndustryTooltipMode.NORMAL || isFunctional()) {
			Color h = Misc.getHighlightColor();
			float opad = 10f;

			float cr = getCR();
			tooltip.addPara("Station combat readiness: %s", opad, h, "" + Math.round(cr * 100f) + "%");

			addStabilityPostDemandSection(tooltip, hasDemand, mode);

			boolean battlestation = getSpec().hasTag(Industries.TAG_BATTLESTATION);
			boolean starfortress = getSpec().hasTag(Industries.TAG_STARFORTRESS);
			float bonus = DEFENSE_BONUS_BASE;
			if (battlestation) bonus = DEFENSE_BONUS_BATTLESTATION;
			else if (starfortress) bonus = DEFENSE_BONUS_FORTRESS;
			addGroundDefensesImpactSection(tooltip, bonus, Commodities.SUPPLIES);
		}
	}

	@Override
	protected Object readResolve() {
		super.readResolve();
		return this;
	}


	protected CampaignFleetAPI stationFleet = null;
	protected boolean usingExistingStation = false;
	protected SectorEntityToken stationEntity = null;

	//protected IntervalUtil tracker = new IntervalUtil(0.7f, 1.3f);

	@Override
	public void advance(float amount) {
		super.advance(amount);

		if (Global.getSector().getEconomy().isSimMode()) return;


		if (stationEntity == null) {
			spawnStation();
		}

		if (stationFleet != null) {
			stationFleet.setAI(null);
			if (stationFleet.getOrbit() == null && stationEntity != null) {
				stationFleet.setCircularOrbit(stationEntity, 0, 0, 100);
			}
		}
	}


	@Override
	protected void buildingFinished() {
		super.buildingFinished();

		if (stationEntity != null && stationFleet != null) {
			matchStationAndCommanderToCurrentIndustry();
		} else {
			spawnStation();
		}
	}

	@Override
	public void notifyBeingRemoved(MarketInteractionMode mode, boolean forUpgrade) {
		super.notifyBeingRemoved(mode, forUpgrade);

		if (!forUpgrade) {
			removeStationEntityAndFleetIfNeeded();
		}
	}

	@Override
	protected void upgradeFinished(Industry previous) {
		super.upgradeFinished(previous);

		if (previous instanceof DuskStardockMil) {
			DuskStardockMil prev = (DuskStardockMil) previous;
			stationEntity = prev.stationEntity;
			stationFleet = prev.stationFleet;
			usingExistingStation = prev.usingExistingStation;

			if (stationFleet != null) {
				stationFleet.removeEventListener(prev);
				stationFleet.addEventListener(this);
			}

			if (stationEntity != null && stationFleet != null) {
				matchStationAndCommanderToCurrentIndustry();
			} else {
				spawnStation();
			}
		}
	}

	protected void removeStationEntityAndFleetIfNeeded() {
		if (stationEntity != null) {
			stationEntity.getMemoryWithoutUpdate().unset(MemFlags.STATION_FLEET);
			stationEntity.getMemoryWithoutUpdate().unset(MemFlags.STATION_BASE_FLEET);

			stationEntity.getContainingLocation().removeEntity(stationFleet);

			if (stationEntity.getContainingLocation() != null && !usingExistingStation) {
				stationEntity.getContainingLocation().removeEntity(stationEntity);
				market.getConnectedEntities().remove(stationEntity);

				// commented out so that MarketCMD doesn't NPE if you destroy a market through bombardment of a station
				//stationEntity.setMarket(null);

			} else if (stationEntity.hasTag(Tags.USE_STATION_VISUAL)) {
				((CustomCampaignEntityAPI) stationEntity).setFleetForVisual(null);
				float origRadius = ((CustomCampaignEntityAPI) stationEntity).getCustomEntitySpec().getDefaultRadius();
				((CustomCampaignEntityAPI) stationEntity).setRadius(origRadius);
			}

			if (stationFleet != null) {
				stationFleet.getMemoryWithoutUpdate().unset(MemFlags.STATION_MARKET);
				stationFleet.removeEventListener(this);
			}

			stationEntity = null;
			stationFleet = null;
		}
	}


	@Override
	public void notifyColonyRenamed() {
		super.notifyColonyRenamed();
		if (!usingExistingStation) {
			stationFleet.setName(market.getName() + " Station");
			stationEntity.setName(market.getName() + " Station");
		}
	}


	protected void spawnStation() {
		FleetParamsV3 fParams = new FleetParamsV3(null, null,
				market.getFactionId(),
				1f,
				FleetTypes.PATROL_SMALL,
				0,
				0, 0, 0, 0, 0, 0);
		fParams.allWeapons = true;

		removeStationEntityAndFleetIfNeeded();

		stationFleet = FleetFactoryV3.createFleet(fParams);
		//stationFleet.setName(getCurrentName());
		stationFleet.setNoFactionInName(true);


		stationFleet.setStationMode(true);

		//stationFleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);

		// needed for AI fleets to engage it, as they engage the hidden station fleet, unlike
		// the player that interacts with the stationEntity
		stationFleet.clearAbilities();
		stationFleet.addAbility(Abilities.TRANSPONDER);
		stationFleet.getAbility(Abilities.TRANSPONDER).activate();
		stationFleet.getDetectedRangeMod().modifyFlat("gen", 10000f);

		stationFleet.setAI(null);
		stationFleet.addEventListener(this);


		ensureStationEntityIsSetOrCreated();

		if (stationEntity instanceof CustomCampaignEntityAPI) {
			if (!usingExistingStation || stationEntity.hasTag(Tags.USE_STATION_VISUAL)) {
				((CustomCampaignEntityAPI) stationEntity).setFleetForVisual(stationFleet);
			}
		}

		stationFleet.setCircularOrbit(stationEntity, 0, 0, 100);
		stationFleet.getMemoryWithoutUpdate().set(MemFlags.STATION_MARKET, market);
		stationFleet.setHidden(true);


		matchStationAndCommanderToCurrentIndustry();
	}


	protected void ensureStationEntityIsSetOrCreated() {
		if (stationEntity == null) {
			for (SectorEntityToken entity : market.getConnectedEntities()) {
				if (entity.hasTag(Tags.STATION) && !entity.hasTag("NO_ORBITAL_STATION")) { // added NO_ORBITAL_STATION per modder request 
					stationEntity = entity;
					usingExistingStation = true;
					break;
				}
			}
		}

		if (stationEntity == null) {
			stationEntity = market.getContainingLocation().addCustomEntity(
					null, market.getName() + " Station", Entities.STATION_BUILT_FROM_INDUSTRY, market.getFactionId());
			SectorEntityToken primary = market.getPrimaryEntity();
			float orbitRadius = primary.getRadius() + 150f;
			stationEntity.setCircularOrbitWithSpin(primary, (float) Math.random() * 360f, orbitRadius, orbitRadius / 10f, 5f, 5f);
			market.getConnectedEntities().add(stationEntity);
			stationEntity.setMarket(market);
		}
	}


	protected void matchStationAndCommanderToCurrentIndustry() {
		stationFleet.getFleetData().clear();

		String fleetName = null;
		String variantId = null;
		float radius = 60f;

		try {
			JSONObject json = new JSONObject(getSpec().getData());
			variantId = json.getString("variant");
			radius = (float) json.getDouble("radius");
			fleetName = json.getString("fleetName");
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}

		if (stationEntity != null) {
			fleetName = stationEntity.getName();
		}


		stationFleet.setName(fleetName);

		FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, variantId);
		//String name = stationFleet.getFleetData().pickShipName(member, null);
		String name = fleetName;
		member.setShipName(name);

		stationFleet.getFleetData().addFleetMember(member);

		applyCRToStation();

		if (!usingExistingStation && stationEntity instanceof CustomCampaignEntityAPI) {
			((CustomCampaignEntityAPI) stationEntity).setRadius(radius);
		} else if (stationEntity.hasTag(Tags.USE_STATION_VISUAL)) {
			((CustomCampaignEntityAPI) stationEntity).setRadius(radius);
		}

		boolean skeletonMode = !isFunctional();

		if (skeletonMode) {
			stationEntity.getMemoryWithoutUpdate().unset(MemFlags.STATION_FLEET);
			stationEntity.getMemoryWithoutUpdate().set(MemFlags.STATION_BASE_FLEET, stationFleet);
			stationEntity.getContainingLocation().removeEntity(stationFleet);

			for (int i = 1; i < member.getStatus().getNumStatuses(); i++) {
				ShipVariantAPI variant = member.getVariant();
				if (i > 0) {
					String slotId = member.getVariant().getModuleSlots().get(i - 1);
					variant = variant.getModuleVariant(slotId);
				} else {
					continue;
				}

				if (!variant.hasHullMod(HullMods.VASTBULK)) {
					member.getStatus().setDetached(i, true);
					member.getStatus().setPermaDetached(i, true);
					member.getStatus().setHullFraction(i, 0f);
				}
			}

		} else {
			stationEntity.getMemoryWithoutUpdate().unset(MemFlags.STATION_BASE_FLEET);
			stationEntity.getMemoryWithoutUpdate().set(MemFlags.STATION_FLEET, stationFleet);
			stationEntity.getContainingLocation().removeEntity(stationFleet);
			stationFleet.setExpired(false);
			stationEntity.getContainingLocation().addEntity(stationFleet);
		}
	}

	protected int getHumanCommanderLevel() {
		boolean battlestation = getSpec().hasTag(Industries.TAG_BATTLESTATION);
		boolean starfortress = getSpec().hasTag(Industries.TAG_STARFORTRESS);

		if (starfortress) {
			return Global.getSettings().getInt("tier3StationOfficerLevel");
		} else if (battlestation) {
			return Global.getSettings().getInt("tier2StationOfficerLevel");
		}
		return Global.getSettings().getInt("tier1StationOfficerLevel");
	}

	protected void matchCommanderToAICore(String aiCore) {
		if (stationFleet == null) return;

		PersonAPI commander = null;
		if (Commodities.ALPHA_CORE.equals(aiCore)) {
			AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(Commodities.ALPHA_CORE);
			commander = plugin.createPerson(Commodities.ALPHA_CORE, Factions.REMNANTS, null);
			if (stationFleet.getFlagship() != null) {
				RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(stationFleet.getFlagship());
			}
		} else {
			if (stationFleet.getFlagship() != null) {
				int level = getHumanCommanderLevel();
				PersonAPI current = stationFleet.getFlagship().getCaptain();
				if (level > 0) {
					if (current.isAICore() || current.getStats().getLevel() != level) {
						commander = OfficerManagerEvent.createOfficer(
								Global.getSector().getFaction(market.getFactionId()), level, true);
					}
				} else {
					if (stationFleet.getFlagship() == null || stationFleet.getFlagship().getCaptain() == null ||
							!stationFleet.getFlagship().getCaptain().isDefault()) {
						commander = Global.getFactory().createPerson();
					}
				}
			}

		}

		if (commander != null) {
			//stationFleet.setCommander(commander); // don't want a  "this is a flagship" star showing in the fleet list
			if (stationFleet.getFlagship() != null) {
				stationFleet.getFlagship().setCaptain(commander);
				stationFleet.getFlagship().setFlagship(false);
			}
		}
	}


	public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {

	}


	@Override
	protected void disruptionFinished() {
		super.disruptionFinished();

		matchStationAndCommanderToCurrentIndustry();
	}

	@Override
	protected void notifyDisrupted() {
		super.notifyDisrupted();

		matchStationAndCommanderToCurrentIndustry();
	}

	public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
		if (fleet != stationFleet) return; // shouldn't happen...

		disrupt(this);

		// bug where somehow a station fleet can become empty as a result of combat
		// then its despawn() gets called every frame
		if (stationFleet.getMembersWithFightersCopy().isEmpty()) {
			matchStationAndCommanderToCurrentIndustry();
		}
		stationFleet.setAbortDespawn(true);
	}

	public static void disrupt(Industry station) {
		station.setDisrupted(station.getSpec().getBuildTime() * 0.5f, true);
	}

	public boolean isAvailableToBuild() {
		Industry reqind = market.getIndustry("duskstardock");
		if (reqind == null || !reqind.isFunctional())
			return false;

		boolean canBuild = false;
		for (Industry ind : market.getIndustries()) {
			if (ind == this) continue;
			if (!ind.isFunctional()) continue;
			if (ind.getSpec().hasTag(Industries.TAG_SPACEPORT)) {
				canBuild = true;
				break;
			}
		}
		return canBuild;
	}

	public boolean showWhenUnavailable() {
		if (market.hasTag(Tags.MARKET_NO_INDUSTRIES_ALLOWED)) return false;
		if (market.getIndustry("duskstardock") == null) return false;
		return true;
	}

	public String getUnavailableReason() {
		return "Requires a functional Stardock";
	}

	@Override
	protected int getBaseStabilityMod() {
		boolean battlestation = getSpec().hasTag(Industries.TAG_BATTLESTATION);
		boolean starfortress = getSpec().hasTag(Industries.TAG_STARFORTRESS);
		int stabilityMod = 1;
		if (battlestation) {
			stabilityMod = 2;
		} else if (starfortress) {
			stabilityMod = 3;
		}
		return stabilityMod;
	}

	@Override
	protected Pair<String, Integer> getStabilityAffectingDeficit() {
		return getMaxDeficit(Commodities.SUPPLIES, Commodities.CREW);
	}

	@Override
	protected void applyAlphaCoreSupplyAndDemandModifiers() {
		demandReduction.modifyFlat(getModId(0), DEMAND_REDUCTION, "Alpha core");
	}

	protected void addAlphaCoreDescription(TooltipMakerAPI tooltip, AICoreDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();

		String pre = "Alpha-level AI core currently assigned. ";
		if (mode == AICoreDescriptionMode.MANAGE_CORE_DIALOG_LIST || mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			pre = "Alpha-level AI core. ";
		}
		if (mode == AICoreDescriptionMode.INDUSTRY_TOOLTIP) {
			CommoditySpecAPI coreSpec = Global.getSettings().getCommoditySpec(aiCoreId);
			TooltipMakerAPI text = tooltip.beginImageWithText(coreSpec.getIconName(), 48);
			text.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
							"Increases station combat effectiveness.", 0f, highlight,
					"" + (int) ((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION);
			tooltip.addImageWithText(opad);
			return;
		}

		tooltip.addPara(pre + "Reduces upkeep cost by %s. Reduces demand by %s unit. " +
						"Increases station combat effectiveness.", opad, highlight,
				"" + (int) ((1f - UPKEEP_MULT) * 100f) + "%", "" + DEMAND_REDUCTION);

	}

	@Override
	public boolean canImprove() {
		return true;
	}

	protected void applyImproveModifiers() {
		if (isImproved()) {
			market.getStability().modifyFlat("orbital_station_improve", IMPROVE_STABILITY_BONUS,
					getImprovementsDescForModifiers() + " (" + getNameForModifier() + ")");
		} else {
			market.getStability().unmodifyFlat("orbital_station_improve");
		}
	}

	public void addImproveDesc(TooltipMakerAPI info, ImprovementDescriptionMode mode) {
		float opad = 10f;
		Color highlight = Misc.getHighlightColor();


		if (mode == ImprovementDescriptionMode.INDUSTRY_TOOLTIP) {
			info.addPara("Stability increased by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
		} else {
			info.addPara("Increases stability by %s.", 0f, highlight, "" + (int) IMPROVE_STABILITY_BONUS);
		}

		info.addSpacer(opad);
		super.addImproveDesc(info, mode);
	}


	protected boolean isMiltiarized() {
		boolean battlestation = getSpec().hasTag(Industries.TAG_BATTLESTATION);
		boolean starfortress = getSpec().hasTag(Industries.TAG_STARFORTRESS);
		return battlestation || starfortress;
	}

	@Override
	public RaidDangerLevel adjustCommodityDangerLevel(String commodityId, RaidDangerLevel level) {
		if (!isMiltiarized()) return level;
		return level.next();
	}

	@Override
	public RaidDangerLevel adjustItemDangerLevel(String itemId, String data, RaidDangerLevel level) {
		if (!isMiltiarized()) return level;
		return level.next();
	}

}





