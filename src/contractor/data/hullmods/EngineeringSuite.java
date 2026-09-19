package contractor.data.hullmods;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.HullModFleetEffect;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class EngineeringSuite extends BaseHullMod implements HullModFleetEffect {
	public static final float SUPPLY_MOD = 5f;
	public static final float REPAIR_MOD = 6f;
	public static final float MIN_CR = 0.4f;
	public static final String ID = "con_engineering_suite_id";

	@Override
	public boolean withOnFleetSync() {
		return true;
	}

	@Override
	public void onFleetSync(CampaignFleetAPI fleet) {
		float mod = computeBonus(fleet);

		if (mod <= 0f) {
			for (FleetMemberAPI mem : fleet.getFleetData().getMembersListCopy())
				mem.getStats().getSuppliesPerMonth().unmodify(ID);
			return;
		}


		for (FleetMemberAPI mem : fleet.getFleetData().getMembersListCopy()) {
			mem.getStats().getSuppliesPerMonth().modifyPercent(ID, -mod * SUPPLY_MOD);
			mem.getStats().getRepairRatePercentPerDay().modifyPercent(ID, -mod * REPAIR_MOD);
		}
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

		tooltip.addPara("Ship contains an advanced engineering suite rivaling those found on stations, greatly easing the burden of repair and maintenance.",
				opad, h);

		tooltip.addPara("Reduces daily supply cost of %s ships by %s, and speeds up repairs by %s. Each ship with an Engineering Suite provides diminishing returns, with a theoretical maximum " +
						"of %s cost reduction and %s repair boost.", opad, h,
				"all",
				Math.round(SUPPLY_MOD) + "%",
				Math.round(REPAIR_MOD) + "%",
				Math.round(SUPPLY_MOD) * 4 + "%",
				Math.round(REPAIR_MOD) * 4 + "%");

		if (ship != null && ship.getFleetMember().getFleetData() != null) {
			if (ship.getFleetMember().getRepairTracker().getCR() <= MIN_CR) {
				tooltip.addPara("This ship has insufficient combat readiness, its Engineering Suite is offline and doesn't contribute to the fleet.",
						opad, bad);
			}

			float mod = computeBonus(ship.getFleetMember().getFleetData());
			if (mod > 0f) {
				tooltip.addPara("The effective bonuses for the fleet are a %s reduction in daily supply cost, and a %s increase in repair speed.", opad, h,
						Math.round(mod * SUPPLY_MOD) + "%",
						Math.round(mod * REPAIR_MOD) + "%");

			}
		}
	}

	public float computeBonus(CampaignFleetAPI fleet) {
		int numHullmod = 0;
		float mod = 0f;
		for (FleetMemberAPI mem : fleet.getFleetData().getMembersListCopy()) {
			if (mem.getVariant().hasHullMod("contractor_engineering_suite"))
				if (mem.getRepairTracker().getCR() > MIN_CR)
					numHullmod++;
		}

		if (numHullmod < 1)
			return 0f;

		for (int i = 1; i <= numHullmod; i++) {
			mod += (1f / (1f * i));
		}

		return mod;
	}

	public float computeBonus(FleetDataAPI data) {
		int numHullmod = 0;
		float mod = 0f;
		for (FleetMemberAPI mem : data.getMembersListCopy()) {
			if (mem.getVariant().hasHullMod("contractor_engineering_suite"))
				if (mem.getRepairTracker().getCR() > MIN_CR)
					numHullmod++;
		}

		if (numHullmod < 1)
			return 0f;

		for (int i = 1; i <= numHullmod; i++) {
			mod += (1f / (1f * i));
		}

		return mod;
	}

	public boolean withAdvanceInCampaign() {
		return false;
	}

	public void advanceInCampaign(CampaignFleetAPI fleet) {
	}
}
