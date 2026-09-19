package contractor.data.scripts.campaign.intel;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.intel.events.*;
import com.fs.starfarer.api.impl.campaign.intel.group.FGRaidAction;
import com.fs.starfarer.api.impl.campaign.intel.group.FleetGroupIntel;
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI;
import com.fs.starfarer.api.impl.campaign.missions.FleetCreatorMission;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import contractor.data.scripts.campaign.fleets.DisposableNaniteFleetManager;
import contractor.data.scripts.campaign.fleets.NaniteFleetBehaviorScript;
import contractor.data.scripts.util.ContractorStaticVars;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.BASE_QUALITY_WHEN_NO_MARKET;

public class NaniteHostileActivityFactor extends BaseHostileActivityFactor implements FleetGroupIntel.FGIEventListener {
	public static final float CORE_DIST_LY = 20f;

	public NaniteHostileActivityFactor(HostileActivityEventIntel intel) {
		super(intel);
	}

	public boolean shouldShow(BaseEventIntel intel) {
		return getProgress(intel) > 0;
	}

	public int getProgress(BaseEventIntel intel) {
		int total = 0;
		if (Global.getSector().getMemoryWithoutUpdate().contains(NaniteRespiteScript.KEY))
			return total;
		for (HostileActivityCause2 cause : getCauses()) {
			total += cause.getProgress();
		}
		return total;
	}

	public String getProgressStr(BaseEventIntel intel) {
		return "";
	}

	public String getDesc(BaseEventIntel intel) {
		return "Nanite activity";
	}

	public String getNameForThreatList(boolean first) {
		return "Nanite";
	}

	public Color getDescColor(BaseEventIntel intel) {
		if (getProgress(intel) <= 0) {
			return Misc.getGrayColor();
		}
		return Global.getSector().getFaction(ContractorStaticVars.CONTRACTOR_NANITE_ID).getBaseUIColor();
	}

	public TooltipMakerAPI.TooltipCreator getMainRowTooltip(BaseEventIntel intel) {
		return new BaseFactorTooltip() {
			public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
				float opad = 10f;
				tooltip.addPara("Nanite fleets have been sighted in your space.", 0f);
				tooltip.addPara("Like a moth to flame, Nanite fleets are drawn toward EM activity, such as the " +
						"kind broadcast by advanced civilisation. Occasionally a few fleets will appear at the edges of your " +
						"colonial systems, looking for things to hunt.", opad);
			}
		};
	}

	@Override
	public int getMaxNumFleets(StarSystemAPI system) {
		if (getProgress(intel) <= 0) {
			return 0;
		}
		return 4;
	}

	@Override
	public CampaignFleetAPI createFleet(StarSystemAPI system, Random random) {
		float mag = 0f;
		mag += getEffectMagnitude(system);
		if (mag > 1f) mag = 1f;

		int difficulty = 0;
		difficulty += (int) Math.round(mag * 3f);
		int minDiff = Math.round(intel.getMarketPresenceFactor(system) * 3f);
		if (difficulty < minDiff) difficulty = minDiff;

		WeightedRandomPicker<DisposableThreatFleetManager.FabricatorEscortStrength> picker = new WeightedRandomPicker<>();
		DisposableThreatFleetManager.FabricatorEscortStrength strength;
		int unifications;

		switch (difficulty) {
			case 0 -> {
				unifications = 0;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 1f);
				strength = picker.pick();
				if (Math.random() > 0.9d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 1;
				}
			}
			case 1 -> {
				unifications = 0;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 15f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 3f);
				strength = picker.pick();
				if (Math.random() > 0.8d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 1;
				}
			}
			case 2 -> {
				unifications = 1;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 1f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
				strength = picker.pick();
				if (Math.random() > 0.7d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 2;
				}
			}
			case 3 -> {
				unifications = 2;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM, 3f);
				strength = picker.pick();
				if (Math.random() > 0.6d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 3;
				}
			}
			default -> {
				unifications = 0;
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 15f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
				picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 3f);
				strength = picker.pick();
				if (Math.random() > 0.8d &&
						(strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM)) {
					unifications = 1;
				}
			}
		}

		CampaignFleetAPI f = DisposableNaniteFleetManager.createNaniteFleet(unifications, strength);
		if (f == null)
			return null;

		system.addEntity(f);

		float radius = 12000f + 6000f * (float) Math.random();
		Vector2f loc = Misc.getPointAtRadius(new Vector2f(), radius);
		f.setLocation(loc.x, loc.y);

		f.addScript(new NaniteFleetBehaviorScript(f, system));

		return f;
	}

	public void addBulletPointForEvent(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage, TooltipMakerAPI info,
									   IntelInfoPlugin.ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
		info.addPara("Nanite surge detected", tc, initPad);
	}

	public void addBulletPointForEventReset(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage, TooltipMakerAPI info,
											IntelInfoPlugin.ListInfoMode mode, boolean isUpdate, Color tc, float initPad) {
		info.addPara("Nanite surge defeated", tc, initPad);
	}

	public void addStageDescriptionForEvent(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage, TooltipMakerAPI info) {
		float small = 8f;
		float opad = 10f;

		info.addPara("Long range sensors have detected a large mass of Nanite ships headed towards "
						+ "one of your colonies which presents great risk of losing infrastructure. If the " +
						"'raid' is successful, the targeted colonies will suffer total loss most industries, " +
						"starbases, and space relays.",
				small,
				Misc.getNegativeHighlightColor(), "total loss", "industries", "starbases",
				"space relays");

		LabelAPI label = info.addPara("Defeating the surge would grant a period of relief from Nanite attacks, "
						+ "but they will return eventually, as there's no telling how many more wait in the black " +
						"between stars.",
				opad);
		label.setHighlight("period of relief", "Nanite", "return eventually");
		label.setHighlightColors(
				Misc.getPositiveHighlightColor(),
				Global.getSector().getFaction(ContractorStaticVars.CONTRACTOR_NANITE_ID).getBaseUIColor(),
				Misc.getHighlightColor());

		stage.beginResetReqList(info, true, "crisis", opad);
		info.addPara("The %s are destroyed", 0f, getDescColor(intel), "attacking fleets");
		stage.endResetReqList(info, false, "crisis", -1, -1);

		addBorder(info, Global.getSector().getFaction(ContractorStaticVars.CONTRACTOR_NANITE_ID).getBaseUIColor());
	}

	public String getEventStageIcon(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage) {
		return Global.getSector().getFaction(ContractorStaticVars.CONTRACTOR_NANITE_ID).getCrest();
	}

	public TooltipMakerAPI.TooltipCreator getStageTooltipImpl(final HostileActivityEventIntel intel, final BaseEventIntel.EventStageData stage) {
		if (stage.id == HostileActivityEventIntel.Stage.HA_EVENT) {
			return getDefaultEventTooltip("Nanite surge detected", intel, stage);
		}
		return null;
	}

	public float getEventFrequency(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage) {
		if (stage.id == HostileActivityEventIntel.Stage.HA_EVENT) {
			MarketAPI target = findAttackTarget(stage);
			if (target != null) {
				return 7f;
			}
		}
		return 0f;
	}

	public void rollEvent(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage) {
		MarketAPI target = findAttackTarget(stage);
		if (target == null) return;

		HostileActivityEventIntel.HAERandomEventData data = new HostileActivityEventIntel.HAERandomEventData(this, stage);
		data.custom = target;
		stage.rollData = data;
		intel.sendUpdateIfPlayerHasIntel(data, false);
	}

	public boolean fireEvent(HostileActivityEventIntel intel, BaseEventIntel.EventStageData stage) {
		MarketAPI target = findAttackTarget(stage);
		if (target == null || target.getStarSystem() == null) return false;

		stage.rollData = null;
		return startAttack(target, target.getStarSystem(), stage, getRandomizedStageRandom(3));
	}

	public MarketAPI findAttackTarget(BaseEventIntel.EventStageData stage) {
		WeightedRandomPicker<MarketAPI> picker = new WeightedRandomPicker<MarketAPI>(getRandomizedStageRandom(3));

		for (StarSystemAPI system : Misc.getPlayerSystems(false)) {
			for (MarketAPI curr : Misc.getMarketsInLocation(system, Factions.PLAYER)) {
				picker.add(curr, curr.getSize() * curr.getSize() * curr.getSize());
			}
		}
		return picker.pick();
	}

	public boolean startAttack(MarketAPI target, StarSystemAPI system, BaseEventIntel.EventStageData stage, Random random) {
		GenericRaidFGI.GenericRaidParams params = new GenericRaidFGI.GenericRaidParams(new Random(random.nextLong()), true);

		params.makeFleetsHostile = true;
		params.factionId = ContractorStaticVars.CONTRACTOR_NANITE_ID;

		Vector2f point = MathUtils.getPointOnCircumference(system.getCenter().getLocation(), 25600f, (float) (Math.random() * 360f));

		MarketAPI market = Global.getFactory().createMarket("fake", "fake", 5);
		market.getStability().modifyFlat("fake", 10000);
		market.setFactionId(params.factionId);
		SectorEntityToken token = system.createToken(point.getX(), point.getY());
		market.setPrimaryEntity(token);

		market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", BASE_QUALITY_WHEN_NO_MARKET);
		market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);


		params.source = market;
		params.prepDays = 14f;
		params.payloadDays = 35f + 14f * random.nextFloat();

		params.raidParams.where = system;
		params.raidParams.type = FGRaidAction.FGRaidType.CONCURRENT;
		params.raidParams.tryToCaptureObjectives = true;
		params.raidParams.allowedTargets.add(target);
		params.raidParams.allowNonHostileTargets = true;
		params.raidParams.setBombardment(MarketCMD.BombardType.TACTICAL);
		params.noun = "integrator attack";
		params.forcesNoun = "nanite integrators";
		params.raidParams.raidApproachText = "moving to harvest";
		params.raidParams.raidActionText = "harvesting";
		params.raidParams.raidsPerColony = 4;

		params.style = FleetCreatorMission.FleetStyle.STANDARD;
		params.repImpact = HubMissionWithTriggers.ComplicationRepImpact.FULL;

		// standard Askonia fleet size multiplier with no shortages/issues is a bit over 230%
		float fleetSizeMult = 1f;

		float totalDifficulty = fleetSizeMult * 50f;

		totalDifficulty -= 10;
		params.fleetSizes.add(10);

		while (totalDifficulty > 0) {
			int min = 6;
			int max = 10;

			int diff = min + random.nextInt(max - min + 1);

			params.fleetSizes.add(diff);
			totalDifficulty -= diff;
		}

		NaniteRaidFGI raid = new NaniteRaidFGI(params);
		raid.setListener(this);
		Global.getSector().getIntelManager().addIntel(raid);

		return true;
	}

	@Override
	public void reportFGIAborted(FleetGroupIntel intel) {
		new NaniteRespiteScript();
	}
}
