package contractor.data.hullmods;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.awt.*;
import java.util.List;

public class ExposedFlightDeck extends BaseHullMod {
	public static final float CREW_LOSS = 33f;
	public static final float RATIO = 1.718282f;
	public static final String KEY = "ExposedFlightDeck";

	public void advanceInCombat(ShipAPI ship, float amount) {
		if (Global.getCurrentState() != GameState.COMBAT || ship.isHulk())
			return;

		List<FighterLaunchBayAPI> bays = ship.getLaunchBaysCopy();
		if (bays.isEmpty())
			return;

		WeightedRandomPicker<String> spriteIds = new WeightedRandomPicker<>();
		float hullScale = (float) Math.log(ship.getHullLevel() + RATIO);
		for (FighterLaunchBayAPI bay : bays) {
			if (bay.getWing() == null)
				continue;
			if (hullScale < 0.9f && bay.getCurrRate() > hullScale)
				bay.setCurrRate(hullScale);
			String name = bay.getWing().getSpec().getVariant().getHullSpec().getSpriteName();
			spriteIds.add(name);
		}

		if (!spriteIds.isEmpty()) {
			int slotRendered = 0;
			for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy())
				if (slot.isDecorative() && slot.getId().matches("DECO\\d+"))
					slotRendered++;

			for (WeaponSlotAPI slot : ship.getHullSpec().getAllWeaponSlotsCopy()) {
				if (ship.getSharedFighterReplacementRate() < 1f - (0.15f * slotRendered))
					break;
				if (slot.isDecorative() && slot.getId().matches("DECO\\d+")) {
					SpriteAPI sprite = Global.getSettings().getSprite(spriteIds.pick());
					//sprite.setSize(sprite.getWidth() * 0.4f, sprite.getHeight() * 0.4f);
					//sprite.setAngle(slot.computeMidArcAngle(ship));
					//sprite.setColor(Color.white);
					//sprite.setAlphaMult(1f);
					//sprite.setCenter(sprite.getWidth() / 2f, sprite.getHeight() / 2f);
					float x = slot.computePosition(ship).getX();
					float y = slot.computePosition(ship).getY();
					float angle = Misc.normalizeAngle(slot.computeMidArcAngle(ship) - 90f);
					//sprite.renderAtCenter(x, y); why the hell does this not work

					slotRendered--;
					MagicRender.battlespace(sprite, new Vector2f(x, y), new Vector2f(), new Vector2f(sprite.getWidth() * 0.38f, sprite.getHeight() * 0.38f), null, angle,
							0f, Color.white, false, 0f, 0f, 0f, 0f, 0f, 0f, 0.017f, 0f, CombatEngineLayers.BELOW_PHASED_SHIPS_LAYER);
				}
			}
		}

		if (ship.getMutableStats().getCrewLossMult().getPercentStatMod(ship.getId() + "_" + KEY) == null)
			ship.getMutableStats().getCrewLossMult().modifyPercent(ship.getId() + "_" + KEY, CREW_LOSS);

		if (Global.getCombatEngine() != null && Global.getCombatEngine().getPlayerShip() == ship) {
			//if (Global.getSettings().isDevMode() && ship.getMutableStats().getCrewLossMult().getPercentStatMod(ship.getId() + "_" + KEY) != null)
			//Global.getCombatEngine().maintainStatusForPlayerShip(KEY + "_", Global.getSettings().getSpriteName("ui", "contractor_ui_efdstatus"), "FLIGHT DECK", "Crew at risk", true);
			if (hullScale < 0.9f) {
				String status = "LIGHT DAMAGE";
				if (hullScale < 0.7f)
					status = "HEAVY DAMAGE";
				else if (hullScale < 0.8f)
					status = "MEDIUM DAMAGE";

				Global.getCombatEngine().maintainStatusForPlayerShip(KEY, Global.getSettings().getSpriteName("ui", "contractor_ui_efdstatus"), "FLIGHT DECK", status, true);
			}
		}
	}

	public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
		if (index == 0) return (int) CREW_LOSS + "%";
		if (index == 1) return "any fighter bay";
		if (index == 2) return Math.round(Math.log(RATIO) * 100) + "%";
		return null;
	}
}

