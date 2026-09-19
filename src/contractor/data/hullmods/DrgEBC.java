package contractor.data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.WeaponBaseRangeModifier;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

public class DrgEBC extends BaseHullMod {
	public static final float RANGE_BONUS = 50f;

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		ship.addListener(new DrgEBCRangeModifier());
	}

	public static class DrgEBCRangeModifier implements WeaponBaseRangeModifier {
		public DrgEBCRangeModifier() {
		}

		public float getWeaponBaseRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
			return 0f;
		}

		public float getWeaponBaseRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
			return 1f;
		}

		public float getWeaponBaseRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
			if (weapon.isBeam()) return 0f;
			if (weapon.getType() == WeaponType.ENERGY || weapon.getType() == WeaponType.HYBRID)
				return RANGE_BONUS;

			return 0f;
		}
	}

	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return false;
	}

	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float opad = 10f;
		Color h = Misc.getHighlightColor();

		tooltip.addPara("Derived from the original Tri-Tachyon design, the Dusk Research Group's bolt coherer has been heavily modified with "
				+ "exotic materials, and stabilized for combat conditions. Though this comes at the expense of reducing its beneficial effects.", opad);
		tooltip.addPara("Increases the base range of all non-beam Energy and Hybrid weapons by %s.", opad, h,
				(int) RANGE_BONUS + "");

		tooltip.addSectionHeading("Interactions with other modifiers", Alignment.MID, opad);
		tooltip.addPara("Since the base range is increased, this range modifier"
				+ " - unlike most other flat modifiers in the game - "
				+ "is increased by percentage modifiers from other hullmods and skills.", opad);
	}
}









