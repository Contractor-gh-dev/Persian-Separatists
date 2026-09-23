package contractor.data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI.CREvent;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AsteroidCutter extends BaseHullMod implements HullModFleetEffect {
	public static final int RATING = 30;
	public static final float MINCR = 0.4f;
	public static final float MOVE_BONUS = 1f;
	public static final String MODKEY = "sep_Cutter";
	private static final Map<Object, Object> chanceMap = new HashMap<>(); static {
		chanceMap.put(ShipAPI.HullSize.FRIGATE, 2f);
		chanceMap.put(ShipAPI.HullSize.DESTROYER, 10f);
		chanceMap.put(ShipAPI.HullSize.CRUISER, 20f);
		chanceMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 30f);
		chanceMap.put(ShipAPI.HullSize.FIGHTER, 0f);
		chanceMap.put(ShipAPI.HullSize.DEFAULT, 10f);
	}

	private static final Map<Object, Object> bulkMap = new HashMap<>(); static {
		bulkMap.put(ShipAPI.HullSize.FRIGATE, 1f);
		bulkMap.put(ShipAPI.HullSize.DESTROYER, 5f);
		bulkMap.put(ShipAPI.HullSize.CRUISER, 10f);
		bulkMap.put(ShipAPI.HullSize.CAPITAL_SHIP, 20f);
		bulkMap.put(ShipAPI.HullSize.FIGHTER, 0f);
		bulkMap.put(ShipAPI.HullSize.DEFAULT, 5f);
	}

	private static float timer = 0f;
	private static long evalTimePrev = 0;

	public boolean withOnFleetSync() {
		return true;
	}

	public void onFleetSync(CampaignFleetAPI fleet) {
		if (getNumCutters(fleet) > 0) {
			if (getFleetBulk(fleet.getFleetData().getMembersListCopy()) <= getNumCutters(fleet) * RATING)
				fleet.getStats().getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).modifyFlat(MODKEY, MOVE_BONUS, "Asteroid cutters");
			else
				fleet.getStats().getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).unmodify(MODKEY);
		}
	}

	public void advanceInCampaign(FleetMemberAPI member, float amount) {
		if (member.getFleetData() == null || member.getFleetData().getFleet() == null || !member.getFleetData().getFleet().isPlayerFleet() || amount == 0f)
			return;
		if (evalTimePrev == Global.getSector().getClock().getTimestamp())
			return;
		evalTimePrev = Global.getSector().getClock().getTimestamp();
		timer += amount;
		if (timer < 1f)
			return;
		timer -= 1f;

		List<FleetMemberAPI> memberList = new ArrayList<>(member.getFleetData().getMembersListCopy());
		FleetMemberAPI affectedShip = null;
		float crRestore = 0f;
		int cutterNum = 0;

		for (FleetMemberAPI memberSelect : memberList) {
			boolean skip = false;
			if (memberSelect.getRepairTracker().getCR() > MINCR) {
				List<String> mods = memberSelect.getHullSpec().getBuiltInMods();
				for (String modId : mods)
					if (modId.equals("contractor_asteroidcutter"))
						cutterNum++;
			}

			List<CREvent> eventList = new ArrayList<>(memberSelect.getRepairTracker().getRecentEvents());
			if (eventList.isEmpty())
				continue;
			for (CREvent event : eventList) {
				if (Objects.equals(event.id, "asteroid_cutter")) {
					skip = true;
					break;
				}
			}
			if (skip)
				continue;
			for (CREvent event : eventList) {
				if (Objects.equals(event.id, "asteroid_impact")) {
					if (event.elapsed > 3f)
						break;
					crRestore = Math.abs(event.crAmount);
					affectedShip = memberSelect;
					break;
				}
			}
		}

		if (affectedShip == null)
			return;

		float chance = computeChance(affectedShip.getHullSpec().getHullSize(), cutterNum);
		String crDamageId = "asteroid_cutter";
		String crDamageReason = "Asteroid cutter";

		if (chance < Math.random() * 5f) {
			affectedShip.getRepairTracker().applyCREvent(crRestore, crDamageId, crDamageReason);
			affectedShip.getStatus().repairFraction(crRestore * 3f);
			float maxCR = affectedShip.getRepairTracker().getMaxCR();
			if (maxCR < affectedShip.getRepairTracker().getBaseCR())
				affectedShip.getRepairTracker().setCR(maxCR);
			affectedShip.getStatus().repairArmorAllCells(crRestore * 0.8f);
			Global.getSector().getCampaignUI().addMessage("Impact against " + affectedShip.getShipName() + "has been mitigated by " + cutterNum + " asteroid cutters", Misc.getPositiveHighlightColor());
		} else {
			crRestore = 0f;
			affectedShip.getRepairTracker().applyCREvent(crRestore, crDamageId, crDamageReason);
			Global.getSector().getCampaignUI().addMessage("Impact mitigation against " + affectedShip.getShipName() + " has failed", Misc.getNegativeHighlightColor());
		}
	}

	private static float computeChance(ShipAPI.HullSize hull, int cutterNum) {
		float size = (float) chanceMap.get(hull);
		return (float) Math.min(4.8f, Math.max(0.2, Math.log(size / (Math.min(10f, cutterNum) * 1.82f)) + 2f));
	}

	private static int getNumCutters(ShipAPI ship) {
		int cutterNum = 0;
		for (FleetMemberAPI memberSelect : ship.getFleetMember().getFleetData().getMembersListCopy()) {
			if (memberSelect.getRepairTracker().getCR() > MINCR) {
				if (memberSelect.getVariant().hasHullMod("contractor_asteroidcutter")) {
					cutterNum++;
					if (memberSelect.getVariant().getSModdedBuiltIns().contains("contractor_asteroidcutter"))
						cutterNum++;
				}
			}
		}
		return cutterNum;
	}

	public static int getNumCutters(CampaignFleetAPI fleet) {
		int cutterNum = 0;
		for (FleetMemberAPI memberSelect : fleet.getFleetData().getMembersListCopy()) {
			if (memberSelect.getRepairTracker().getCR() > MINCR) {
				if (memberSelect.getVariant().hasHullMod("contractor_asteroidcutter")) {
					cutterNum++;
					if (memberSelect.getVariant().getSModdedBuiltIns().contains("contractor_asteroidcutter"))
						cutterNum++;
				}
			}
		}
		return cutterNum;
	}

	public static int getNumCuttersNoBonus(ShipAPI ship) {
		int cutterNum = 0;
		for (FleetMemberAPI memberSelect : ship.getFleetMember().getFleetData().getMembersListCopy()) {
			if (memberSelect.getRepairTracker().getCR() > MINCR) {
				if (memberSelect.getVariant().hasHullMod("contractor_asteroidcutter"))
					cutterNum++;
			}
		}
		return cutterNum;
	}

	public static float getFleetBulk(List<FleetMemberAPI> fleetList) {
		float bulk = 0f;
		for (FleetMemberAPI ship : fleetList) {
			bulk += (float) bulkMap.get(ship.getHullSpec().getHullSize());
		}
		return bulk;
	}

	public static float getShipBulk(ShipAPI ship) {
		ShipAPI.HullSize size = ship.getHullSize();
		return (float) bulkMap.get(size);
	}

	public boolean hasSModEffect() {
		return true; //manual override
	}

	public String getSModDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return "Doubles";
		return null;
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(ShipAPI.HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("Ship possesses necessary weapons, armor, or drones to engage in asteroid cutting, and gives the fleet a chance to mitigate an asteroid impact and makes movement through adverse terrain easier.",
				opad, h);

		if (isForModSpec || ship == null) return;
		if (Global.getSettings().getCurrentState() == GameState.TITLE) return;

		if (ship.getCurrentCR() < MINCR) {
			LabelAPI label = tooltip.addPara("This ship's combat readiness is below %s " +
							"and cannot defend against impacts or contribute to the fleet cutter rating.",
					opad, h,
					Math.round(MINCR * 100f) + "%");
			label.setHighlightColors(bad);
		} else {
			int rate;
			if (ship.getVariant().getSModdedBuiltIns().contains("contractor_asteroidcutter"))
				rate = RATING * 2;
			else
				rate = RATING;

			tooltip.addPara("This ship contributes %s to the fleets cutter rating.", opad, h,
					rate + "");
		}

		int cutterNum = getNumCutters(ship);
		tooltip.addPara("The fleet currently has the equivalent of %s effective asteroid cutters, " +
						"which translates to a mitigation chance of %s/%s/%s/%s per hull size.",
				opad, h,
				"" + cutterNum,
				Math.round(100f - 20f * computeChance(ShipAPI.HullSize.FRIGATE, cutterNum)) + "%",
				Math.round(100f - 20f * computeChance(ShipAPI.HullSize.DESTROYER, cutterNum)) + "%",
				Math.round(100f - 20f * computeChance(ShipAPI.HullSize.CRUISER, cutterNum)) + "%",
				Math.round(100f - 20f * computeChance(ShipAPI.HullSize.CAPITAL_SHIP, cutterNum)) + "%"
		);

		if (getFleetBulk(ship.getFleetMember().getFleetData().getMembersListCopy()) <= cutterNum * RATING)
			tooltip.addPara("The fleet's current cutter rating is %s which exceeds the fleet's bulk of %s, and grants a slow movement bonus of %s.",
					opad, h,
					"" + cutterNum * RATING,
					"" + Math.round(getFleetBulk(ship.getFleetMember().getFleetData().getMembersListCopy())),
					"" + Math.round(MOVE_BONUS)
			);
		else
			tooltip.addPara("The fleet's current cutter rating is %s which is below the fleet's bulk of %s, and grants no bonus.",
					opad, h,
					"" + cutterNum * RATING,
					"" + Math.round(getFleetBulk(ship.getFleetMember().getFleetData().getMembersListCopy()))
			);
	}

	@Override
	public void advanceInCampaign(CampaignFleetAPI fleet) {
	}

	@Override
	public boolean withAdvanceInCampaign() {
		return false;
	}
}
